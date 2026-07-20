import { useCallback, useEffect, useMemo, useState } from "react";
import { Bell, Building2, CalendarDays, Inbox, Megaphone, Send, Trash2, Users } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { useAuth } from "@/features/auth/authStore";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { ROLES } from "@/lib/constants";
import * as api from "./api";
import type { Notice, NoticePriority, NoticeRole } from "./types";

const labels: Record<NoticeRole, string> = {
  PRINCIPAL: "Principals",
  HOD: "HODs",
  STUDENT_SECTION: "Student Section",
  FEE_SECTION: "Accountants / Fee Section",
  CLASS_TEACHER: "Class Teachers",
  SUBJECT_TEACHER: "Subject Teachers",
  STUDENT: "Students",
};

export function NoticesPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN]);
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
            "STUDENT",
          ]
        : principal
          ? ["HOD", "STUDENT_SECTION", "FEE_SECTION", "CLASS_TEACHER", "SUBJECT_TEACHER", "STUDENT"]
          : hod
            ? ["STUDENT"]
            : [],
    [admin, principal, hod],
  );
  const [inbox, setInbox] = useState<Notice[]>([]);
  const [sent, setSent] = useState<Notice[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [tab, setTab] = useState<"inbox" | "sent">("inbox");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [priority, setPriority] = useState<NoticePriority>("NORMAL");
  const [collegeIds, setCollegeIds] = useState<number[]>([]);
  const [roles, setRoles] = useState<NoticeRole[]>(hod ? ["STUDENT"] : []);
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
        .then(setColleges)
        .catch(() => setColleges([]));
  }, [admin]);

  function toggleRole(role: NoticeRole) {
    if (hod) return;
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
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim() || !message.trim() || roles.length === 0) {
      toast.error("Enter a title, message, and select at least one audience.");
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
      });
      toast.success("Notice sent successfully");
      setTitle("");
      setMessage("");
      setPriority("NORMAL");
      setRoles(hod ? ["STUDENT"] : []);
      setCollegeIds([]);
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
                High and urgent notices require every recipient to wait 8 seconds and acknowledge them before opening the dashboard.
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
                          <span className={`mt-2 inline-flex rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${notice.priority === "URGENT" ? "bg-rose-100 text-rose-700" : "bg-amber-100 text-amber-700"}`}>
                            {notice.priority}
                          </span>
                        )}
                        <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                          <span className="font-semibold text-brand-700">
                            {tab === "inbox"
                              ? `From ${notice.createdByName}`
                              : `To ${notice.audienceRoles.map((r) => labels[r]).join(", ")}`}
                          </span>
                          <span className="inline-flex items-center gap-1">
                            <Building2 className="h-3.5 w-3.5" />
                            {notice.allColleges || notice.collegeNames.length === 0
                              ? "All colleges"
                              : notice.collegeNames.join(", ")}
                            {notice.departmentName ? ` • ${notice.departmentName}` : ""}
                          </span>
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
