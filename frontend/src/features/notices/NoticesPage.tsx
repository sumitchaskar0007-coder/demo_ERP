import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bell,
  Building2,
  CalendarDays,
  Eye,
  Inbox,
  Megaphone,
  Search,
  Send,
  Trash2,
  Users,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { Link } from "react-router-dom";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { useAuth } from "@/features/auth/authStore";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { getActiveDepartmentsForAdmin, searchDepartments } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import { handleApiError } from "@/lib/handleApiError";
import { ROLES } from "@/lib/constants";
import * as api from "./api";
import type {
  Notice,
  NoticeDeliveryMode,
  NoticePriority,
  NoticeReceipt,
  NoticeRecipientOption,
  NoticeRole,
} from "./types";
import {
  getVisibleNoticeCollegeNames,
  isCollegeVisibleInNoticeScope,
} from "./noticeCollegeVisibility";

const labels: Record<NoticeRole, string> = {
  SUPER_ADMIN: "Super Admins",
  ADMIN: "Admins",
  PRINCIPAL: "Principals",
  HOD: "HODs",
  STUDENT_SECTION: "Student Section",
  FEE_SECTION: "Accountants / Fee Section",
  CLASS_TEACHER: "Class Teachers",
  SUBJECT_TEACHER: "Subject Teachers",
  GENERAL_STAFF: "General Staff",
  STUDENT: "Students",
};

function NoticeCollegeScope({ notice }: { notice: Notice }) {
  const collegeNames = getVisibleNoticeCollegeNames(notice.collegeNames);
  const scopeParts: string[] = [];
  if (notice.allColleges || notice.collegeNames.length === 0) scopeParts.push("All colleges");
  else if (collegeNames.length > 0) scopeParts.push(collegeNames.join(", "));
  if (notice.departmentName) scopeParts.push(notice.departmentName);
  if (scopeParts.length === 0) return null;

  return (
    <span className="inline-flex items-center gap-1">
      <Building2 className="h-3.5 w-3.5" />
      {scopeParts.join(" • ")}
    </span>
  );
}

export function NoticesPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN, ROLES.ADMIN]);
  const principal = isRole([ROLES.PRINCIPAL]);
  const hod = isRole([ROLES.HOD]);
  const canSend = admin || principal || hod;
  const allowed = useMemo<NoticeRole[]>(
    () =>
      admin
        ? [
            "PRINCIPAL",
            "HOD",
            "STUDENT_SECTION",
            "FEE_SECTION",
            "CLASS_TEACHER",
            "SUBJECT_TEACHER",
            "GENERAL_STAFF",
            "STUDENT",
          ]
        : principal
          ? [
              "HOD",
              "STUDENT_SECTION",
              "FEE_SECTION",
              "CLASS_TEACHER",
              "SUBJECT_TEACHER",
              "GENERAL_STAFF",
              "STUDENT",
            ]
          : hod
            ? [
                "STUDENT_SECTION",
                "FEE_SECTION",
                "CLASS_TEACHER",
                "SUBJECT_TEACHER",
                "GENERAL_STAFF",
                "STUDENT",
              ]
            : [],
    [admin, principal, hod],
  );
  const [inbox, setInbox] = useState<Notice[]>([]);
  const [sent, setSent] = useState<Notice[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [tab, setTab] = useState<"inbox" | "sent">("inbox");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [priority, setPriority] = useState<NoticePriority>("NORMAL");
  const [collegeIds, setCollegeIds] = useState<number[]>([]);
  const [roles, setRoles] = useState<NoticeRole[]>(hod ? ["STUDENT"] : []);
  const [deliveryMode, setDeliveryMode] = useState<NoticeDeliveryMode>("COMMON");
  const [departmentId, setDepartmentId] = useState<number | null>(null);
  const [recipientRole, setRecipientRole] = useState<NoticeRole | "">(
    principal ? "HOD" : hod ? "STUDENT" : "",
  );
  const [recipientQuery, setRecipientQuery] = useState("");
  const [recipientResults, setRecipientResults] = useState<NoticeRecipientOption[]>([]);
  const [selectedRecipients, setSelectedRecipients] = useState<NoticeRecipientOption[]>([]);
  const [searchingRecipients, setSearchingRecipients] = useState(false);
  const [openReceiptNoticeId, setOpenReceiptNoticeId] = useState<number | null>(null);
  const [receiptByNotice, setReceiptByNotice] = useState<Record<number, NoticeReceipt>>({});
  const [loadingReceipts, setLoadingReceipts] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [received, outgoing] = await Promise.all([
        api.getNoticeInbox(),
        canSend ? api.getSentNotices() : Promise.resolve([]),
      ]);
      setInbox(received);
      setSent(outgoing);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [canSend]);
  useEffect(() => {
    void load();
  }, [load]);
  useEffect(() => {
    if (admin)
      getActiveColleges()
        .then((activeColleges) =>
          setColleges(activeColleges.filter(isCollegeVisibleInNoticeScope)),
        )
        .catch(() => setColleges([]));
  }, [admin]);
  useEffect(() => {
    const collegeId = admin
      ? collegeIds.length === 1
        ? collegeIds[0]
        : null
      : principal
        ? (user?.collegeId ?? null)
        : null;
    setDepartmentId(null);
    if (!collegeId || hod) {
      setDepartments([]);
      return;
    }
    const request = admin
      ? getActiveDepartmentsForAdmin(collegeId)
      : searchDepartments({ collegeId, status: "ACTIVE", page: 0, size: 100 }).then(
          (result) => result.content,
        );
    request.then(setDepartments).catch(() => setDepartments([]));
  }, [admin, principal, hod, collegeIds, user?.collegeId]);
  useEffect(() => {
    if (deliveryMode !== "INDIVIDUAL") return;
    const timer = window.setTimeout(() => {
      setSearchingRecipients(true);
      api
        .searchNoticeRecipients({
          collegeId: admin && collegeIds.length === 1 ? collegeIds[0] : undefined,
          departmentId: departmentId ?? undefined,
          role: recipientRole || undefined,
          query: recipientQuery.trim() || undefined,
          size: 50,
        })
        .then((result) => setRecipientResults(result.content))
        .catch((error) => toast.error(handleApiError(error).message))
        .finally(() => setSearchingRecipients(false));
    }, 300);
    return () => window.clearTimeout(timer);
  }, [deliveryMode, admin, collegeIds, departmentId, recipientRole, recipientQuery]);

  function toggleRole(role: NoticeRole) {
    setRoles((current) =>
      current.includes(role) ? current.filter((item) => item !== role) : [...current, role],
    );
  }
  function toggleCollege(id: number) {
    setCollegeIds((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    );
  }
  async function removeNotice(id: number) {
    if (
      !window.confirm(
        "Delete this notice from every user dashboard? Its audit record will be retained.",
      )
    )
      return;
    try {
      await api.deleteNotice(id);
      toast.success("Notice deleted");
      await load();
    } catch (error) {
      toast.error(handleApiError(error).message);
    }
  }
  async function toggleReceipts(id: number) {
    if (openReceiptNoticeId === id) {
      setOpenReceiptNoticeId(null);
      return;
    }
    setOpenReceiptNoticeId(id);
    if (receiptByNotice[id]) return;
    setLoadingReceipts(id);
    try {
      const receipt = await api.getNoticeReceipts(id);
      setReceiptByNotice((current) => ({ ...current, [id]: receipt }));
    } catch (error) {
      setOpenReceiptNoticeId(null);
      toast.error(handleApiError(error).message);
    } finally {
      setLoadingReceipts(null);
    }
  }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim() || !message.trim()) {
      toast.error("Enter a notice title and message.");
      return;
    }
    if (deliveryMode === "COMMON" && roles.length === 0) {
      toast.error("Select at least one audience role.");
      return;
    }
    if (deliveryMode === "INDIVIDUAL" && selectedRecipients.length === 0) {
      toast.error("Select at least one individual recipient.");
      return;
    }
    setSending(true);
    try {
      await api.sendNotice({
        title,
        message,
        priority,
        audienceRoles: roles,
        collegeIds: admin ? collegeIds : [],
        departmentId,
        deliveryMode,
        recipientUserIds: selectedRecipients.map((recipient) => recipient.userId),
      });
      toast.success("Notice sent successfully");
      setTitle("");
      setMessage("");
      setPriority("NORMAL");
      setRoles(hod ? ["STUDENT"] : []);
      setCollegeIds([]);
      setDepartmentId(null);
      setDeliveryMode("COMMON");
      setSelectedRecipients([]);
      setRecipientQuery("");
      await load();
      setTab("sent");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSending(false);
    }
  }
  const visible = tab === "inbox" ? inbox : sent;
  return (
    <div className="page-container space-y-6 pb-10">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold text-slate-400">
            Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;<span className="text-brand-600">Notices</span>
          </p>
          <h1 className="page-title mt-2">Notice Board</h1>
          <p className="page-subtitle">
            Create targeted announcements and keep every recipient informed.
          </p>
        </div>
        <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-500 shadow-sm">
          <Bell className="h-4 w-4 text-brand-600" />
          <span>
            <b className="text-slate-800">{inbox.length}</b> received notices
          </span>
        </div>
      </div>
      {canSend && (
        <Card className="overflow-hidden">
          <div className="flex items-center gap-3 border-b border-slate-200 bg-gradient-to-r from-brand-50 to-white px-6 py-5">
            <div className="grid h-11 w-11 place-items-center rounded-xl bg-brand-600 text-white shadow-sm">
              <Megaphone className="h-5 w-5" />
            </div>
            <div>
              <h2 className="font-bold text-slate-900">Create a new notice</h2>
              <p className="mt-0.5 text-xs text-slate-500">
                {admin
                  ? "Choose colleges and recipient roles before publishing."
                  : principal
                    ? `This notice will stay within ${user?.collegeName}.`
                    : "This notice will go to students in your department."}
              </p>
            </div>
          </div>
          <form onSubmit={submit} className="space-y-6 p-6">
            <div className="grid gap-5 lg:grid-cols-[0.85fr_1.15fr]">
              <Input
                label="Notice title"
                value={title}
                maxLength={180}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Example: Holiday announcement"
                className="h-12"
              />
              <Textarea
                label="Message"
                value={message}
                maxLength={10000}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Write a clear message for the recipients…"
                className="min-h-28"
              />
            </div>
            <section className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <label className="text-sm font-semibold text-slate-800" htmlFor="notice-priority">
                Notice priority
              </label>
              <p className="mt-1 text-xs text-slate-500">
                High and urgent notices require every recipient to wait 8 seconds and acknowledge
                them before opening the dashboard.
              </p>
              <select
                id="notice-priority"
                value={priority}
                onChange={(event) => setPriority(event.target.value as NoticePriority)}
                className="mt-3 h-11 w-full max-w-xs rounded-xl border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              >
                <option value="NORMAL">Normal</option>
                <option value="HIGH">High — acknowledgement required</option>
                <option value="URGENT">Urgent — acknowledgement required</option>
              </select>
            </section>
            <section className="grid gap-3 sm:grid-cols-2">
              {(["COMMON", "INDIVIDUAL"] as NoticeDeliveryMode[]).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  onClick={() => setDeliveryMode(mode)}
                  className={`rounded-2xl border p-4 text-left transition ${
                    deliveryMode === mode
                      ? "border-brand-500 bg-brand-50 ring-2 ring-brand-100"
                      : "border-slate-200 bg-white hover:border-brand-200"
                  }`}
                >
                  <span className="block text-sm font-bold text-slate-900">
                    {mode === "COMMON" ? "Common notice" : "Individual notice"}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">
                    {mode === "COMMON"
                      ? "Send to everyone matching the selected scope and roles."
                      : "Search for users and send to only the people you select."}
                  </span>
                </button>
              ))}
            </section>
            {admin && (
              <section className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <Building2 className="h-4 w-4 text-brand-600" />
                    <div>
                      <p className="text-sm font-semibold text-slate-800">College scope</p>
                      <p className="text-xs text-slate-500">
                        Select one, several, or every college.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setCollegeIds(colleges.map((college) => college.id))}
                    className="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-brand-600 shadow-sm ring-1 ring-slate-200 hover:bg-brand-50"
                  >
                    Select all colleges
                  </button>
                </div>
                <div className="grid max-h-48 gap-2 overflow-y-auto sm:grid-cols-2 xl:grid-cols-3">
                  {colleges.map((college) => {
                    const selected = collegeIds.includes(college.id);
                    return (
                      <label
                        key={college.id}
                        className={`flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-sm transition ${selected ? "border-brand-300 bg-brand-50 text-brand-800 shadow-sm" : "border-slate-200 bg-white text-slate-600 hover:border-slate-300"}`}
                      >
                        <input
                          type="checkbox"
                          checked={selected}
                          onChange={() => toggleCollege(college.id)}
                          className="h-4 w-4 rounded border-slate-300 text-brand-600"
                        />
                        <span className="min-w-0">
                          <b className="block truncate font-semibold">{college.name}</b>
                          <span className="text-xs text-slate-400">{college.code}</span>
                        </span>
                      </label>
                    );
                  })}
                </div>
                <div className="mt-3 inline-flex rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-500 ring-1 ring-slate-200">
                  {collegeIds.length === 0
                    ? "All colleges"
                    : `${collegeIds.length} college${collegeIds.length === 1 ? "" : "s"} selected`}
                </div>
              </section>
            )}
            {!hod && departments.length > 0 && (
              <section className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <label htmlFor="notice-department" className="text-sm font-semibold text-slate-800">
                  Department filter
                </label>
                <p className="mt-1 text-xs text-slate-500">
                  Leave this as all departments, or limit the notice to one department.
                </p>
                <select
                  id="notice-department"
                  value={departmentId ?? ""}
                  onChange={(event) =>
                    setDepartmentId(event.target.value ? Number(event.target.value) : null)
                  }
                  className="mt-3 h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
                >
                  <option value="">All departments</option>
                  {departments.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name} ({department.code})
                    </option>
                  ))}
                </select>
              </section>
            )}
            {deliveryMode === "COMMON" ? (
              <section>
                <div className="mb-3 flex items-center gap-2">
                  <Users className="h-4 w-4 text-brand-600" />
                  <p className="text-sm font-semibold text-slate-800">Select recipients</p>
                  <span className="text-xs text-slate-400">Choose one or more roles</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {allowed.map((role) => {
                    const selected = roles.includes(role);
                    return (
                      <label
                        key={role}
                        className={`flex cursor-pointer items-center gap-2 rounded-full border px-4 py-2.5 text-sm font-medium transition ${selected ? "border-brand-600 bg-brand-600 text-white shadow-sm" : "border-slate-200 bg-white text-slate-600 hover:border-brand-300 hover:text-brand-700"}`}
                      >
                        <input
                          type="checkbox"
                          checked={selected}
                          disabled={hod}
                          onChange={() => toggleRole(role)}
                          className="sr-only"
                        />
                        <span
                          className={`h-2 w-2 rounded-full ${selected ? "bg-white" : "bg-slate-300"}`}
                        />
                        {labels[role]}
                      </label>
                    );
                  })}
                </div>
              </section>
            ) : (
              <section className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div>
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-brand-600" />
                    <p className="text-sm font-semibold text-slate-800">Select individual users</p>
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    Results are automatically restricted to your permitted college or department.
                  </p>
                </div>
                <div className="grid gap-3 md:grid-cols-[1fr_240px]">
                  <div className="relative">
                    <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
                    <input
                      value={recipientQuery}
                      onChange={(event) => setRecipientQuery(event.target.value)}
                      placeholder="Search name, email, or phone"
                      className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
                    />
                  </div>
                  <select
                    value={recipientRole}
                    onChange={(event) => setRecipientRole(event.target.value as NoticeRole | "")}
                    className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
                  >
                    <option value="">All roles</option>
                    {allowed.map((role) => (
                      <option key={role} value={role}>
                        {labels[role]}
                      </option>
                    ))}
                  </select>
                </div>
                {selectedRecipients.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {selectedRecipients.map((recipient) => (
                      <span
                        key={recipient.userId}
                        className="inline-flex items-center gap-2 rounded-full bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white"
                      >
                        {recipient.fullName}
                        <button
                          type="button"
                          aria-label={`Remove ${recipient.fullName}`}
                          onClick={() =>
                            setSelectedRecipients((current) =>
                              current.filter((item) => item.userId !== recipient.userId),
                            )
                          }
                        >
                          <X className="h-3.5 w-3.5" />
                        </button>
                      </span>
                    ))}
                  </div>
                )}
                <div className="max-h-72 overflow-y-auto rounded-xl border border-slate-200 bg-white">
                  {searchingRecipients ? (
                    <p className="p-5 text-center text-sm text-slate-500">Searching users…</p>
                  ) : recipientResults.length === 0 ? (
                    <p className="p-5 text-center text-sm text-slate-500">
                      No matching users found.
                    </p>
                  ) : (
                    recipientResults.map((recipient) => {
                      const selected = selectedRecipients.some(
                        (item) => item.userId === recipient.userId,
                      );
                      return (
                        <label
                          key={recipient.userId}
                          className="flex cursor-pointer items-start gap-3 border-b border-slate-100 p-3 last:border-0 hover:bg-slate-50"
                        >
                          <input
                            type="checkbox"
                            checked={selected}
                            onChange={() =>
                              setSelectedRecipients((current) =>
                                selected
                                  ? current.filter((item) => item.userId !== recipient.userId)
                                  : [...current, recipient],
                              )
                            }
                            className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600"
                          />
                          <span className="min-w-0 flex-1">
                            <b className="block truncate text-sm text-slate-800">
                              {recipient.fullName}
                            </b>
                            <span className="block truncate text-xs text-slate-500">
                              {recipient.email}
                            </span>
                            <span className="mt-1 block text-[11px] text-slate-400">
                              {[
                                recipient.collegeName,
                                recipient.departmentName,
                                recipient.roles.map((role) => labels[role]).join(", "),
                              ]
                                .filter(Boolean)
                                .join(" • ")}
                            </span>
                          </span>
                        </label>
                      );
                    })
                  )}
                </div>
                <p className="text-xs font-semibold text-brand-700">
                  {selectedRecipients.length} user{selectedRecipients.length === 1 ? "" : "s"}{" "}
                  selected
                </p>
              </section>
            )}
            <div className="flex items-center justify-between border-t border-slate-100 pt-5">
              <p className="hidden text-xs text-slate-400 sm:block">
                The notice will be saved to the Notice Board after publishing.
              </p>
              <Button type="submit" loading={sending} className="h-11 px-6">
                <Send className="h-4 w-4" />
                Publish notice
              </Button>
            </div>
          </form>
        </Card>
      )}
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-200 bg-white px-5">
          <div className="flex">
            <button
              onClick={() => setTab("inbox")}
              className={`flex h-14 items-center gap-2 border-b-2 px-4 text-sm font-semibold transition ${tab === "inbox" ? "border-brand-600 text-brand-700" : "border-transparent text-slate-500 hover:text-slate-800"}`}
            >
              <Inbox className="h-4 w-4" />
              Inbox
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs">{inbox.length}</span>
            </button>
            {canSend && (
              <button
                onClick={() => setTab("sent")}
                className={`flex h-14 items-center gap-2 border-b-2 px-4 text-sm font-semibold transition ${tab === "sent" ? "border-brand-600 text-brand-700" : "border-transparent text-slate-500 hover:text-slate-800"}`}
              >
                <Send className="h-4 w-4" />
                Sent
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs">{sent.length}</span>
              </button>
            )}
          </div>
        </div>
        <div className="space-y-3 bg-slate-50/60 p-4 sm:p-5">
          {loading ? (
            <p className="p-8 text-center text-sm text-slate-500">Loading notices…</p>
          ) : visible.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-200 bg-white p-10 text-center">
              <Bell className="mx-auto h-8 w-8 text-slate-300" />
              <p className="mt-3 text-sm font-semibold text-slate-600">No notices here yet</p>
            </div>
          ) : (
            visible.map((notice) => (
              <article
                key={notice.id}
                className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-brand-200 hover:shadow-md"
              >
                <div className="flex items-start gap-4">
                  <div className="hidden h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600 sm:grid">
                    <Megaphone className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-start">
                      <div>
                        <h3 className="text-base font-bold text-slate-900">{notice.title}</h3>
                        {notice.priority !== "NORMAL" && (
                          <span
                            className={`mt-2 inline-flex rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${notice.priority === "URGENT" ? "bg-rose-100 text-rose-700" : "bg-amber-100 text-amber-700"}`}
                          >
                            {notice.priority}
                          </span>
                        )}
                        <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                          <span className="font-semibold text-brand-700">
                            {tab === "inbox"
                              ? `From ${notice.createdByName}`
                              : notice.deliveryMode === "INDIVIDUAL"
                                ? `To ${notice.recipientNames.join(", ")}${notice.recipientCount > notice.recipientNames.length ? ` +${notice.recipientCount - notice.recipientNames.length} more` : ""}`
                                : `To ${notice.audienceRoles.map((r) => labels[r]).join(", ")}`}
                          </span>
                          <NoticeCollegeScope notice={notice} />
                        </div>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <time className="inline-flex items-center gap-1.5 text-xs text-slate-400">
                          <CalendarDays className="h-3.5 w-3.5" />
                          {new Date(notice.createdAt).toLocaleString()}
                        </time>
                        {admin && tab === "sent" && (
                          <button
                            type="button"
                            onClick={() => void removeNotice(notice.id)}
                            className="rounded-lg p-2 text-rose-500 hover:bg-rose-50"
                            aria-label={`Delete ${notice.title}`}
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </div>
                    <p className="mt-4 whitespace-pre-wrap border-t border-slate-100 pt-4 text-sm leading-6 text-slate-600">
                      {notice.message}
                    </p>
                    {tab === "sent" && (
                      <div className="mt-4 border-t border-slate-100 pt-4">
                        <button
                          type="button"
                          onClick={() => void toggleReceipts(notice.id)}
                          className="inline-flex items-center gap-2 rounded-xl bg-slate-100 px-3 py-2 text-xs font-bold text-slate-700 transition hover:bg-brand-50 hover:text-brand-700"
                        >
                          <Eye className="h-4 w-4" />
                          {loadingReceipts === notice.id
                            ? "Loading read status…"
                            : openReceiptNoticeId === notice.id
                              ? "Hide read status"
                              : "View read status"}
                        </button>
                        {openReceiptNoticeId === notice.id && receiptByNotice[notice.id] && (
                          <div className="mt-3 overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
                            <div className="flex flex-wrap gap-3 border-b border-slate-200 bg-white px-4 py-3 text-xs font-semibold">
                              <span className="text-emerald-700">
                                Seen: {receiptByNotice[notice.id].seenCount}
                              </span>
                            </div>
                            {receiptByNotice[notice.id].recipients.length === 0 ? (
                              <p className="p-4 text-sm text-slate-500">
                                No receiver has read this notice yet.
                              </p>
                            ) : (
                              <div className="max-h-64 divide-y divide-slate-200 overflow-y-auto">
                                {receiptByNotice[notice.id].recipients.map((receipt) => (
                                  <div
                                    key={receipt.userId}
                                    className="flex items-center justify-between gap-3 p-3"
                                  >
                                    <div className="min-w-0">
                                      <p className="truncate text-sm font-semibold text-slate-800">
                                        {receipt.fullName}
                                      </p>
                                      <p className="truncate text-xs text-slate-500">
                                        {receipt.email}
                                      </p>
                                    </div>
                                    <div className="shrink-0 text-right text-xs">
                                      {receipt.seen ? (
                                        <span className="inline-flex items-center gap-1 font-bold text-emerald-700">
                                          <Eye className="h-3.5 w-3.5" />
                                          Seen
                                        </span>
                                      ) : (
                                        <span className="font-semibold text-slate-400">
                                          Not seen
                                        </span>
                                      )}
                                      {receipt.seenAt && (
                                        <p className="mt-1 text-[10px] text-slate-400">
                                          {new Date(receipt.seenAt).toLocaleString()}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                    {notice.actionPath && tab === "inbox" && (
                      <Link
                        to={notice.actionPath}
                        className="mt-4 inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-brand-700"
                      >
                        {notice.actionPath.startsWith("/timetable")
                          ? "Review Timetable"
                          : "Open Action"}
                      </Link>
                    )}
                  </div>
                </div>
              </article>
            ))
          )}
        </div>
      </Card>
    </div>
  );
}
