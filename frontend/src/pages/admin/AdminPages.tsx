import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { Pagination } from "@/components/common/Pagination";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/admin/api";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { getActiveDepartmentsForAdmin } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import type { FeeStructureResponse } from "@/features/fees/types";
import type { PageResponse } from "@/types/api";
const categories = ["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"].map((v) => ({
  label: v,
  value: v,
}));
export function AdminDashboardPage() {
  const [d, setD] = useState<api.AdminAnalytics | null>(null);
  useEffect(() => {
    api
      .getAdminAnalytics()
      .then(setD)
      .catch((e) => toast.error(handleApiError(e).message));
  }, []);
  if (!d) return <Loader />;
  return (
    <div className="page-container">
      <div className="rounded-2xl bg-gradient-to-r from-blue-700 to-cyan-500 p-7 text-white">
        <h1 className="text-2xl font-bold">Super Admin Control Center</h1>
        <p className="mt-1 text-blue-50">
          Govern colleges, principals, fees, and global analytics.
        </p>
      </div>
      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Object.entries(d.summary).map(([k, v]) => (
          <Card className="p-5" key={k}>
            <p className="text-xs font-bold uppercase text-slate-400">
              {k.replace(/([A-Z])/g, " $1")}
            </p>
            <p className="mt-2 text-2xl font-bold">{v}</p>
          </Card>
        ))}
      </div>
      <Card className="mt-6 p-5">
        <h2 className="font-bold">Quick actions</h2>
        <div className="mt-4 flex flex-wrap gap-3">
          <Link to="/colleges/create">
            <Button>Create College</Button>
          </Link>
          <Link to="/principals/create">
            <Button variant="secondary">Create Principal</Button>
          </Link>
          <Link to={ROUTES.adminFeeSetup}>
            <Button variant="secondary">Setup Fee</Button>
          </Link>
        </div>
      </Card>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Chart title="College-wise Students" data={d.collegeWiseStudents} />
        <Chart title="College-wise Fee Collection" data={d.collegeWiseFeeCollection} />
      </div>
    </div>
  );
}
function Chart({ title, data }: { title: string; data: { label: string; value: number }[] }) {
  const max = Math.max(1, ...data.map((x) => Number(x.value)));
  return (
    <Card className="p-5">
      <h2 className="font-bold">{title}</h2>
      <div className="mt-4 space-y-3">
        {data.map((x) => (
          <div key={x.label}>
            <div className="flex justify-between text-sm">
              <span>{x.label}</span>
              <b>{x.value}</b>
            </div>
            <div className="mt-1 h-2 rounded bg-slate-100">
              <div
                className="h-2 rounded bg-blue-600"
                style={{ width: `${(Number(x.value) / max) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}
export function AdminFeeSetupPage() {
  const [rows, setRows] = useState<FeeStructureResponse[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loadingColleges, setLoadingColleges] = useState(true);
  const [loadingDepartments, setLoadingDepartments] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [filter, setFilter] = useState({ collegeId: "", departmentId: "", studentCategory: "" });
  const [filterDepartments, setFilterDepartments] = useState<Department[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [v, setV] = useState<Record<string, string>>({
    collegeId: "",
    departmentId: "",
    studentCategory: "OPEN",
    academicYear: "2026-27",
    totalFee: "",
    minimumAmountForAdmission: "",
  });

  const load = (requestedPage = page) =>
    api
      .searchAdminFeeStructures({
        collegeId: filter.collegeId || undefined,
        departmentId: filter.departmentId || undefined,
        studentCategory: filter.studentCategory || undefined,
        page: requestedPage,
        size: 10,
      })
      .then((r) => {
        setRows(r.content);
        setPage(r.page);
        setTotalPages(r.totalPages);
        setTotalElements(r.totalElements);
      })
      .catch((e) => toast.error(handleApiError(e).message));

  useEffect(() => {
    void load();
    getActiveColleges()
      .then(setColleges)
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoadingColleges(false));
  }, []);

  useEffect(() => {
    void load(0);
  }, [filter]);

  const selectFilterCollege = async (collegeId: string) => {
    setFilter({ ...filter, collegeId, departmentId: "" });
    setFilterDepartments(collegeId ? await getActiveDepartmentsForAdmin(Number(collegeId)) : []);
  };

  const selectCollege = async (collegeId: string) => {
    setV((current) => ({ ...current, collegeId, departmentId: "" }));
    setDepartments([]);

    if (!collegeId) return;

    setLoadingDepartments(true);
    try {
      setDepartments(await getActiveDepartmentsForAdmin(Number(collegeId)));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setLoadingDepartments(false);
    }
  };

  const input = (n: string, l: string) => (
    <Input label={l} value={v[n] || ""} onChange={(e) => setV({ ...v, [n]: e.target.value })} />
  );

  const save = async () => {
    if (!v.collegeId) {
      toast.error("Please select a college");
      return;
    }
    if (!v.departmentId) {
      toast.error("Please select a department");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        collegeId: Number(v.collegeId),
        departmentId: Number(v.departmentId),
        academicYear: v.academicYear,
        studentCategory: v.studentCategory,
        title: v.title || `${v.studentCategory} Fee`,
        totalFee: Number(v.totalFee),
        minimumAmountForAdmission: Number(v.minimumAmountForAdmission),
        admissionFee: 0,
        tuitionFee: Number(v.totalFee),
        examFee: 0,
        libraryFee: 0,
        otherFee: 0,
      };
      if (editingId) await api.updateAdminFeeStructure(editingId, payload);
      else await api.createAdminFeeStructure(payload);
      toast.success(editingId ? "Fee structure updated" : "Fee structure created");
      setEditingId(null);
      void load();
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const collegeOptions = [
    { label: loadingColleges ? "Loading colleges..." : "Select college", value: "" },
    ...colleges.map((college) => ({ label: college.name, value: college.id })),
  ];

  const departmentOptions = [
    {
      label: loadingDepartments
        ? "Loading departments..."
        : v.collegeId
          ? "Select department"
          : "Select college first",
      value: "",
    },
    ...departments.map((department) => ({ label: department.name, value: department.id })),
  ];

  return (
    <div className="page-container">
      <h1 className="page-title">Fee Setup</h1>
      <p className="page-subtitle">Set fees by college, department, academic year and category.</p>
      <Card className="mt-6 p-5">
        <div className="grid gap-4 md:grid-cols-3">
          <Select
            label="College"
            options={collegeOptions}
            value={v.collegeId}
            disabled={loadingColleges}
            onChange={(event) => void selectCollege(event.target.value)}
          />
          <Select
            label="Department / Course"
            options={departmentOptions}
            value={v.departmentId}
            disabled={!v.collegeId || loadingDepartments}
            onChange={(event) => setV({ ...v, departmentId: event.target.value })}
          />
          {input("academicYear", "Academic Year")}
          <Select
            label="Category"
            options={categories}
            value={v.studentCategory}
            onChange={(e) => setV({ ...v, studentCategory: e.target.value })}
          />
          {input("totalFee", "Total Fee")}
          {input("minimumAmountForAdmission", "Minimum Admission Fee")}
        </div>
        <Button className="mt-4" loading={saving} onClick={save}>
          {editingId ? "Update Fee Structure" : "Create Fee Structure"}
        </Button>
      </Card>
      <Card className="mt-6 p-5">
        <h2 className="font-bold">Configured fees</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Select label="Filter by college" options={[{ label: "All colleges", value: "" }, ...colleges.map((c) => ({ label: c.name, value: c.id }))]} value={filter.collegeId} onChange={(e) => void selectFilterCollege(e.target.value)} />
          <Select label="Filter by department" options={[{ label: filter.collegeId ? "All departments" : "Select college first", value: "" }, ...filterDepartments.map((d) => ({ label: d.name, value: d.id }))]} value={filter.departmentId} disabled={!filter.collegeId} onChange={(e) => setFilter({ ...filter, departmentId: e.target.value })} />
          <Select label="Filter by caste/category" options={[{ label: "All categories", value: "" }, ...categories]} value={filter.studentCategory} onChange={(e) => setFilter({ ...filter, studentCategory: e.target.value })} />
        </div>
        <div className="mt-3 space-y-2">
          {rows.map((r) => (
            <div className="flex items-center justify-between gap-3 rounded-xl border p-3" key={r.id}>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={async () => {
                  setEditingId(r.id);
                  setDepartments(await getActiveDepartmentsForAdmin(r.collegeId));
                  setV({ collegeId: String(r.collegeId), departmentId: String(r.departmentId), studentCategory: r.studentCategory || "OPEN", academicYear: r.academicYear, title: r.title, totalFee: String(r.totalFee), minimumAmountForAdmission: String(r.minimumAmountForAdmission) });
                }}>Edit</Button>
                <Button variant="danger" onClick={async () => {
                  if (!window.confirm(`Delete fee structure "${r.title}"?`)) return;
                  try { await api.deleteAdminFeeStructure(r.id); toast.success("Fee structure deleted"); void load(); }
                  catch (error) { toast.error(handleApiError(error).message); }
                }}>Delete</Button>
              </div>
              <span>
                {r.collegeName} · {r.departmentName} · {r.studentCategory || "OPEN"}
              </span>
              <b>₹{r.totalFee}</b>
            </div>
          ))}
        </div>
        {!rows.length && <EmptyState title="No fee structures" description="No configured fees match these filters." />}
        <div className="mt-4"><Pagination page={page} totalPages={totalPages} totalElements={totalElements} onChange={(next) => void load(next)} /></div>
      </Card>
    </div>
  );
}
export function AdminMoneyPage({ pending = false }: { pending?: boolean }) {
  const [result, setResult] = useState<PageResponse<api.Row> | null>(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    setPage(0);
  }, [pending]);

  useEffect(() => {
    setResult(null);
    (pending ? api.getPendingFees({ page, size: 20 }) : api.getCollections({ page, size: 20 }))
      .then(setResult)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [page, pending]);

  const rows = result?.content ?? [];
  return (
    <div className="page-container">
      <h1 className="page-title">{pending ? "Pending Fees" : "Fee Collection"}</h1>
      <p className="page-subtitle">Global college and department fee overview.</p>
      <Card className="mt-6">
        {!result ? (
          <Loader />
        ) : rows.length ? (
          <>
            <pre className="overflow-auto p-5 text-sm">{JSON.stringify(rows, null, 2)}</pre>
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
          <EmptyState title="No records" description="Fee records will appear here." />
        )}
      </Card>
    </div>
  );
}
export function AdminAnalyticsPage() {
  return <AdminDashboardPage />;
}
