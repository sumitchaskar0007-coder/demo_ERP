import { LibraryBig, Plus, Search } from "lucide-react";
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
import { Modal } from "@/components/common/Modal";
import { Pagination } from "@/components/common/Pagination";
import { Select } from "@/components/common/Select";
import { DataTable, type Column } from "@/components/table/DataTable";
import { TableActions } from "@/components/table/TableActions";
import { useAuth } from "@/features/auth/authStore";
import type { College } from "@/features/colleges/types";
import { getActiveColleges } from "@/features/colleges/api";
import { handleApiError } from "@/lib/handleApiError";
import { PAGE_SIZE, ROLES, STATUS_OPTIONS } from "@/lib/constants";
import { formatDate } from "@/lib/utils";
import type { PageResponse } from "@/types/api";
import * as api from "./api";
import { DepartmentForm } from "./DepartmentForm";
import type { Department } from "./types";

const emptyPage: PageResponse<Department> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, last: true };

export function DepartmentListPage() {
  const { user, isRole } = useAuth();
  const navigate = useNavigate();
  const admin = isRole([ROLES.SUPER_ADMIN]);
  const [result, setResult] = useState(emptyPage);
  const [colleges, setColleges] = useState<College[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [collegeId, setCollegeId] = useState<number | "">(user?.collegeId || "");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Department | null>(null);
  const [confirming, setConfirming] = useState<Department | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  useEffect(() => { if (admin) getActiveColleges().then(setColleges).catch(() => setColleges([])); }, [admin]);
  const load = useCallback(async () => {
    setLoading(true);
    try { setResult(await api.searchDepartments({ keyword: keyword || undefined, collegeId: Number(collegeId) || undefined, status: status as "ACTIVE" | "INACTIVE" | "", page, size: PAGE_SIZE, sortBy: "createdAt", sortDir: "desc" })); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setLoading(false); }
  }, [collegeId, keyword, page, status]);
  useEffect(() => { const timer = setTimeout(load, 250); return () => clearTimeout(timer); }, [load]);
  if (!user) return null;
  const submit = async (values: { collegeId: number; name: string; code: string; description: string }) => {
    try {
      const response = editing ? await api.updateDepartment(editing.id, { name: values.name, description: values.description }) : await api.createDepartment({ ...values, collegeId: user.collegeId || values.collegeId });
      toast.success(response.message); setFormOpen(false); setEditing(null); await load();
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  const toggle = async () => {
    if (!confirming) return;
    setActionLoading(true);
    try { const response = await api.setDepartmentStatus(confirming.id, confirming.status !== "ACTIVE"); toast.success(response.message); setConfirming(null); await load(); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setActionLoading(false); }
  };
  const columns: Column<Department>[] = [
    { key: "name", header: "Department", render: (row) => <div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-600"><LibraryBig className="h-5 w-5" /></div><div><p className="font-semibold text-slate-900">{row.name}</p><p className="text-xs text-slate-400">{row.code}</p></div></div> },
    { key: "college", header: "College", render: (row) => <div><p>{row.collegeName}</p><Badge>{row.collegeCode}</Badge></div> },
    { key: "status", header: "Status", render: (row) => <StatusBadge status={row.status} /> },
    { key: "created", header: "Created", render: (row) => formatDate(row.createdAt) },
    { key: "actions", header: "", className: "w-16", render: (row) => <TableActions active={row.status === "ACTIVE"} onView={() => navigate(`/departments/${row.id}`)} onEdit={() => { setEditing(row); setFormOpen(true); }} onToggle={() => setConfirming(row)} /> },
  ];
  const collegeOptions = [{ label: "All colleges", value: "" }, ...colleges.map((college) => ({ label: college.name, value: college.id }))];
  return <div className="page-container"><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><h1 className="page-title">Departments</h1><p className="page-subtitle">{admin ? "Manage departments across colleges." : `Manage departments for ${user.collegeName}.`}</p></div><Button onClick={() => { setEditing(null); setFormOpen(true); }}><Plus className="h-4 w-4" />Add department</Button></div><Card className="mt-6"><div className={`grid gap-3 border-b p-4 ${admin ? "md:grid-cols-[1fr_220px_200px]" : "sm:grid-cols-[1fr_220px]"}`}><Input placeholder="Search department or college…" icon={<Search className="h-4 w-4" />} value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(0); }} />{admin && <Select options={collegeOptions} value={collegeId} onChange={(event) => { setCollegeId(event.target.value ? Number(event.target.value) : ""); setPage(0); }} aria-label="Filter by college" />}<Select options={STATUS_OPTIONS} value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }} aria-label="Filter by status" /></div>{loading ? <Loader label="Loading departments…" /> : result.content.length ? <><DataTable columns={columns} data={result.content} rowKey={(row) => row.id} /><div className="border-t p-4"><Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} /></div></> : <EmptyState title="No departments found" description="Adjust your filters or create a department." />}</Card><Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? "Edit department" : "Create department"} description={editing ? "Department code and college cannot be changed." : "Create a department inside an active college."}><DepartmentForm department={editing} colleges={colleges} user={user} onSubmit={submit} onCancel={() => setFormOpen(false)} /></Modal><ConfirmDialog open={Boolean(confirming)} onClose={() => setConfirming(null)} onConfirm={toggle} loading={actionLoading} tone={confirming?.status === "ACTIVE" ? "danger" : "primary"} title={`${confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} department?`} description="The department record will remain available and only its status will change." confirmLabel={confirming?.status === "ACTIVE" ? "Deactivate" : "Activate"} /></div>;
}
