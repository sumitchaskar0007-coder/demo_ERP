import { Building2, CalendarDays, Mail, Phone, Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { useAuth } from "@/features/auth/authStore";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROLES, STAFF_TYPE_OPTIONS, STATUS_OPTIONS } from "@/lib/constants";
import { initials } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import * as api from "@/features/staff/api";
import type { StaffResponse } from "@/features/staff/types";

const emptyPage: PageResponse<StaffResponse> = {
  content: [],
  page: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

export function StaffListPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN]);
  const [result, setResult] = useState(emptyPage);
  const [colleges, setColleges] = useState<College[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [staffType, setStaffType] = useState("");
  const [collegeId, setCollegeId] = useState<number | "">(user?.collegeId || "");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState<StaffResponse | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  useEffect(() => {
    if (admin)
      getActiveColleges()
        .then(setColleges)
        .catch(() => setColleges([]));
  }, [admin]);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setResult(
        await (admin ? api.searchAdminStaff : api.searchStaff)({
          keyword: keyword || undefined,
          collegeId: Number(collegeId) || undefined,
          status: status as never,
          staffType: staffType as never,
          page,
          size: PAGE_SIZE,
        }),
      );
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [admin, collegeId, keyword, page, staffType, status]);
  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);
  const toggle = async () => {
    if (!confirming) return;
    setActionLoading(true);
    try {
      if (confirming.status === "ACTIVE") await api.deactivateStaff(confirming.id);
      else await api.activateStaff(confirming.id);
      toast.success("Staff status updated");
      setConfirming(null);
      await load();
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setActionLoading(false);
    }
  };
  return (
    <div className="page-container">
      <div>
        <h1 className="page-title">Staff</h1>
        <p className="page-subtitle">Manage all staff roles without deleting records.</p>
      </div>
      <Card className="mt-6 overflow-hidden">
        <div
          className={`grid gap-3 border-b bg-slate-50/50 p-4 sm:p-5 ${admin ? "md:grid-cols-2 xl:grid-cols-[minmax(260px,1fr)_repeat(3,minmax(170px,220px))]" : "md:grid-cols-3"}`}
        >
          <Input
            placeholder="Search staff..."
            icon={<Search className="h-4 w-4" />}
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
          />
          {admin && (
            <Select
              options={[
                { label: "All colleges", value: "" },
                ...colleges.map((college) => ({ label: college.name, value: college.id })),
              ]}
              value={collegeId}
              onChange={(event) => {
                setCollegeId(event.target.value ? Number(event.target.value) : "");
                setPage(0);
              }}
              aria-label="College"
            />
          )}
          <Select
            options={STAFF_TYPE_OPTIONS}
            value={staffType}
            onChange={(event) => {
              setStaffType(event.target.value);
              setPage(0);
            }}
            aria-label="Staff type"
          />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            aria-label="Status"
          />

        </div>
        {loading ? (
          <Loader label="Loading staff..." />
        ) : result.content.length ? (
          <>
            <div className="hidden overflow-x-auto lg:block">
              <table className="w-full table-fixed text-left">
                <thead><tr className="border-b bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500"><th className="w-[26%] px-5 py-4">Staff Member</th><th className="w-[21%] px-5 py-4">Contact</th><th className="w-[24%] px-5 py-4">Assignment</th><th className="w-[11%] px-5 py-4">Status</th><th className="w-[11%] px-5 py-4">Joined</th><th className="w-[7%] px-5 py-4 text-right">Action</th></tr></thead>
                <tbody className="divide-y divide-slate-100">{result.content.map((row) => <StaffTableRow key={row.id} row={row} admin={admin} onToggle={setConfirming} />)}</tbody>
              </table>
            </div>
            <div className="grid gap-4 p-4 sm:grid-cols-2 lg:hidden">{result.content.map((row) => <StaffCard key={row.id} row={row} admin={admin} onToggle={setConfirming} />)}</div>
            <div className="border-t p-4">
              <Pagination
                page={result.page}
                totalPages={result.totalPages}
                totalElements={result.totalElements}
                onChange={setPage}
              />
            </div>
          </>
        ) : (
          <EmptyState
            title="No staff found"
            description="Create staff to begin assigning ERP responsibilities."
          />
        )}
      </Card>
      <ConfirmDialog
        open={Boolean(confirming)}
        onClose={() => setConfirming(null)}
        onConfirm={toggle}
        loading={actionLoading}
        tone={confirming?.status === "ACTIVE" ? "danger" : "primary"}
        title={`${confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} staff?`}
        description="The linked user account status will also be updated."
        confirmLabel={confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"}
      />
    </div>
  );
}

function joiningDate(value?: string | null) {
  if (!value) return "Not provided";
  return new Date(`${value.slice(0, 10)}T00:00:00`).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function StaffAvatar({ name }: { name: string }) {
  return <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-blue-50 text-sm font-bold text-blue-700">{initials(name)}</div>;
}

function StaffTableRow({ row, admin, onToggle }: { row: StaffResponse; admin: boolean; onToggle: (row: StaffResponse) => void }) {
  return <tr className="align-top transition hover:bg-slate-50/70"><td className="px-5 py-4"><div className="flex min-w-0 items-center gap-3"><StaffAvatar name={row.fullName} /><div className="min-w-0"><p className="truncate font-semibold text-slate-900">{row.fullName}</p><p className="mt-0.5 truncate text-xs text-slate-400">{row.employeeCode}</p></div></div></td><td className="px-5 py-4"><a className="block truncate text-sm text-slate-700 hover:text-brand-600" href={`mailto:${row.email}`}>{row.email}</a><p className="mt-1 text-xs text-slate-400">{row.phone || "No phone number"}</p></td><td className="px-5 py-4"><div className="flex flex-wrap gap-1.5"><Badge>{row.staffType.replaceAll("_", " ")}</Badge>{row.roles.filter((role) => role !== row.staffType).slice(0, 1).map((role) => <Badge key={role} tone="info">{role.replaceAll("_", " ")}</Badge>)}</div><p className="mt-2 truncate text-xs text-slate-500">{row.departmentName || "All departments"}</p></td><td className="px-5 py-4"><StatusBadge status={row.status} /></td><td className="px-5 py-4 text-sm text-slate-600">{joiningDate(row.joiningDate)}</td><td className="px-5 py-4 text-right">{admin ? <span className="whitespace-nowrap text-xs text-slate-400">View only</span> : <Button className="whitespace-nowrap" variant={row.status === "ACTIVE" ? "danger" : "secondary"} onClick={() => onToggle(row)}>{row.status === "ACTIVE" ? "Deactivate" : "Activate"}</Button>}</td></tr>;
}

function StaffCard({ row, admin, onToggle }: { row: StaffResponse; admin: boolean; onToggle: (row: StaffResponse) => void }) {
  return <article className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm"><div className="flex items-start gap-3"><StaffAvatar name={row.fullName} /><div className="min-w-0 flex-1"><div className="flex items-start justify-between gap-2"><div className="min-w-0"><h2 className="truncate font-bold text-slate-900">{row.fullName}</h2><p className="truncate text-xs text-slate-400">{row.employeeCode}</p></div><StatusBadge status={row.status} /></div></div></div><div className="mt-4 flex flex-wrap gap-2"><Badge>{row.staffType.replaceAll("_", " ")}</Badge>{row.roles.filter((role) => role !== row.staffType).slice(0, 2).map((role) => <Badge key={role} tone="info">{role.replaceAll("_", " ")}</Badge>)}</div><div className="mt-4 space-y-2 rounded-xl bg-slate-50 p-3 text-xs text-slate-600"><a href={`mailto:${row.email}`} className="flex min-w-0 items-center gap-2 hover:text-brand-600"><Mail className="h-3.5 w-3.5 shrink-0" /><span className="truncate">{row.email}</span></a><p className="flex items-center gap-2"><Phone className="h-3.5 w-3.5 shrink-0" />{row.phone || "No phone number"}</p><p className="flex items-center gap-2"><Building2 className="h-3.5 w-3.5 shrink-0" />{row.departmentName || "All departments"}</p><p className="flex items-center gap-2"><CalendarDays className="h-3.5 w-3.5 shrink-0" />Joined {joiningDate(row.joiningDate)}</p></div>{!admin && <Button className="mt-4 w-full" variant={row.status === "ACTIVE" ? "danger" : "secondary"} onClick={() => onToggle(row)}>{row.status === "ACTIVE" ? "Deactivate Staff" : "Activate Staff"}</Button>}</article>;
}
