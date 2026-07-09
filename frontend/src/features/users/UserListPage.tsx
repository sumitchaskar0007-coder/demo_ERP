import { Plus, Search, UserRound } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
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
import { DataTable, type Column } from "@/components/table/DataTable";
import { TableActions } from "@/components/table/TableActions";
import type { College } from "@/features/colleges/types";
import { getActiveColleges } from "@/features/colleges/api";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import { PAGE_SIZE, ROUTES, STATUS_OPTIONS } from "@/lib/constants";
import type { PageResponse } from "@/types/api";
import * as api from "./api";
import type { User } from "./types";

const emptyPage: PageResponse<User> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, last: true };

export function UserListPage() {
  const navigate = useNavigate();
  const [result, setResult] = useState(emptyPage);
  const [colleges, setColleges] = useState<College[]>([]);
  const [keyword, setKeyword] = useState("");
  const [collegeId, setCollegeId] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState<User | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  useEffect(() => { getActiveColleges().then(setColleges).catch(() => setColleges([])); }, []);
  const load = useCallback(async () => {
    setLoading(true);
    try { setResult(await api.searchUsers({ keyword: keyword || undefined, collegeId: Number(collegeId) || undefined, role: role || undefined, status: status as "ACTIVE" | "INACTIVE" | "", page, size: PAGE_SIZE, sortBy: "createdAt", sortDir: "desc" })); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setLoading(false); }
  }, [collegeId, keyword, page, role, status]);
  useEffect(() => { const timer = setTimeout(load, 250); return () => clearTimeout(timer); }, [load]);
  const toggle = async () => {
    if (!confirming) return;
    setActionLoading(true);
    try { const response = await api.setUserStatus(confirming.id, confirming.status !== "ACTIVE"); toast.success(response.message); setConfirming(null); await load(); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setActionLoading(false); }
  };
  const columns: Column<User>[] = [
    { key: "name", header: "User", render: (row) => <div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-violet-50 text-violet-600"><UserRound className="h-5 w-5" /></div><div><p className="font-semibold text-slate-900">{row.fullName}</p><p className="text-xs text-slate-400">{row.email}</p></div></div> },
    { key: "phone", header: "Phone", render: (row) => row.phone || "—" },
    { key: "college", header: "College", render: (row) => row.collegeName || <span className="text-slate-400">System-wide</span> },
    { key: "roles", header: "Roles", render: (row) => <div className="flex flex-wrap gap-1">{row.roles.map((item) => <Badge key={item} tone="info">{item.replaceAll("_", " ")}</Badge>)}</div> },
    { key: "status", header: "Status", render: (row) => <StatusBadge status={row.status} /> },
    { key: "lastLogin", header: "Last login", render: (row) => formatDate(row.lastLoginAt) },
    { key: "actions", header: "", className: "w-16", render: (row) => <TableActions active={row.status === "ACTIVE"} onView={() => navigate(`/users/${row.id}`)} onToggle={() => setConfirming(row)} /> },
  ];
  const collegeOptions = [{ label: "All colleges", value: "" }, ...colleges.map((item) => ({ label: item.name, value: item.id }))];
  const roleOptions = [{ label: "All roles", value: "" }, { label: "Super Admin", value: "SUPER_ADMIN" }, { label: "Principal", value: "PRINCIPAL" }];
  return <div className="page-container"><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><h1 className="page-title">Users</h1><p className="page-subtitle">Manage Super Admin and Principal accounts.</p></div><Button onClick={() => navigate(ROUTES.createPrincipal)}><Plus className="h-4 w-4" />Create Principal</Button></div><Card className="mt-6"><div className="grid gap-3 border-b p-4 lg:grid-cols-[1fr_190px_180px_180px]"><Input placeholder="Search name, email, phone or college…" icon={<Search className="h-4 w-4" />} value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(0); }} /><Select options={collegeOptions} value={collegeId} onChange={(event) => { setCollegeId(event.target.value); setPage(0); }} aria-label="College" /><Select options={roleOptions} value={role} onChange={(event) => { setRole(event.target.value); setPage(0); }} aria-label="Role" /><Select options={STATUS_OPTIONS} value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }} aria-label="Status" /></div>{loading ? <Loader label="Loading users…" /> : result.content.length ? <><DataTable columns={columns} data={result.content} rowKey={(row) => row.id} /><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} /></div></> : <EmptyState title="No users found" description="Adjust your filters or create a Principal account." />}</Card><ConfirmDialog open={Boolean(confirming)} onClose={() => setConfirming(null)} onConfirm={toggle} loading={actionLoading} tone={confirming?.status === "ACTIVE" ? "danger" : "primary"} title={`${confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} user?`} description={`${confirming?.fullName || "This user"} will ${confirming?.status === "ACTIVE" ? "lose access to login" : "be able to log in again"}.`} confirmLabel={confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} /></div>;
}
