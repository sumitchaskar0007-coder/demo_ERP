import { useEffect, useState } from "react";
import { Activity, ArrowRight, Building2, CheckCircle2, Clock3, GraduationCap, UserRound, Users, WalletCards } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
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
import { useAuth } from "@/features/auth/authStore";
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
  const { user } = useAuth();
  const [d, setD] = useState<api.AdminAnalytics | null>(null);

  useEffect(() => {
    api.getAdminAnalytics().then(setD).catch((error) => toast.error(handleApiError(error).message));
  }, []);

  if (!d) return <Loader label="Preparing Super Admin dashboard..." />;

  const organizationMetrics = [
    { key: "totalStaff", label: "Total Staff", value: d.summary.totalStaff, icon: Users, color: "bg-blue-50 text-blue-600", accent: "bg-blue-500" },
    { key: "totalPrincipals", label: "Total Principals", value: d.summary.totalPrincipals, icon: UserRound, color: "bg-violet-50 text-violet-600", accent: "bg-violet-500" },
    { key: "totalColleges", label: "Total Colleges", value: d.summary.totalColleges, icon: Building2, color: "bg-amber-50 text-amber-600", accent: "bg-amber-500" },
    { key: "activeColleges", label: "Active Colleges", value: d.summary.activeColleges, icon: CheckCircle2, color: "bg-emerald-50 text-emerald-600", accent: "bg-emerald-500" },
    { key: "totalStudents", label: "Total Students", value: d.summary.totalStudents, icon: GraduationCap, color: "bg-cyan-50 text-cyan-600", accent: "bg-cyan-500" },
  ];
  const feeTotal = d.summary.totalFeeCollection + d.summary.pendingFee;
  const collectionRate = feeTotal > 0 ? Math.round((d.summary.totalFeeCollection / feeTotal) * 100) : 0;

  return (
    <div className="page-container pb-10">
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div><p className="text-xs font-semibold text-slate-400">Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Super Admin</p><h1 className="mt-2 text-2xl font-bold">Super Admin Dashboard</h1><p className="mt-1 text-sm text-slate-500">Global college, student, and fee overview.</p></div>
        <p className="inline-flex items-center gap-2 text-xs font-medium text-slate-400"><Clock3 className="h-4 w-4" /> Updated just now</p>
      </div>

      <section className="erp-welcome-banner px-7 py-7 sm:px-9">
        <div className="absolute -right-10 -top-20 h-64 w-64 rounded-full border-[30px] border-blue-500/30" />
        <div className="absolute right-52 top-5 h-10 w-10 rotate-45 rounded-lg border-4 border-amber-400/80" />
        <div className="relative flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div><p className="text-sm text-blue-100">Jadhavr ERP Administration</p><h2 className="mt-2 text-3xl font-bold">Welcome back, {user?.fullName?.split(" ")[0] || "Admin"}</h2><p className="mt-2 text-sm text-blue-100">Have a productive day managing your education workspace.</p></div>
          <div className="hidden rounded-2xl border border-white/10 bg-white/10 px-5 py-4 text-right backdrop-blur sm:block"><p className="text-xs text-blue-200">Account status</p><p className="mt-1 font-bold">{user?.status}</p></div>
        </div>
      </section>

      <section className="mt-6">
        <div className="mb-4"><h2 className="text-lg font-bold">Organization Overview</h2><p className="mt-1 text-xs text-slate-500">Live people and institution counts from the database</p></div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {organizationMetrics.map(({ key, label, value, icon: Icon, color, accent }) => (
          <Card className="relative overflow-hidden p-5" key={key}>
            <span className={`absolute inset-x-0 top-0 h-1 ${accent}`} />
            <div className="flex items-center justify-between"><div className={`grid h-11 w-11 place-items-center rounded-xl ${color}`}><Icon className="h-5 w-5" /></div><span className="rounded-full bg-emerald-50 px-2 py-1 text-[10px] font-bold text-emerald-600">Live</span></div>
            <p className="mt-4 text-2xl font-bold">{value}</p><p className="mt-1 text-xs font-medium text-slate-500">{label}</p>
          </Card>
        ))}
        </div>
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <Card className="overflow-hidden p-6">
          <div className="flex items-start justify-between"><div><h2 className="font-bold">Financial Overview</h2><p className="mt-1 text-xs text-slate-500">Collected and pending fees from student accounts</p></div><WalletCards className="h-5 w-5 text-emerald-600" /></div>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-emerald-50 p-5"><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Total Fee Collection</p><p className="mt-3 text-3xl font-black text-emerald-900">₹{d.summary.totalFeeCollection.toLocaleString("en-IN")}</p><p className="mt-2 text-xs text-emerald-700">Verified amount received</p></div>
            <div className="rounded-2xl bg-orange-50 p-5"><p className="text-xs font-bold uppercase tracking-wider text-orange-700">Pending Fee</p><p className="mt-3 text-3xl font-black text-orange-900">₹{d.summary.pendingFee.toLocaleString("en-IN")}</p><p className="mt-2 text-xs text-orange-700">Outstanding student balance</p></div>
          </div>
          <div className="mt-6"><div className="flex items-center justify-between text-xs"><span className="font-semibold text-slate-600">Collection progress</span><b className="text-emerald-600">{collectionRate}%</b></div><div className="mt-2 h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-gradient-to-r from-blue-600 to-emerald-500 transition-all" style={{ width: `${collectionRate}%` }} /></div></div>
        </Card>
        <Card className="overflow-hidden">
          <div className="border-b px-6 py-5"><h2 className="font-bold">Quick actions</h2><p className="mt-1 text-xs text-slate-500">Frequently used administration tools</p></div>
          <div className="space-y-2 p-4">
            {[
              { label: "Create College", detail: "Add a new college workspace", to: "/colleges/create", icon: Building2 },
              { label: "Create Principal", detail: "Add a principal account", to: "/principals/create", icon: Users },
              { label: "Set up Fees", detail: "Configure college fee structures", to: ROUTES.adminFeeSetup, icon: WalletCards },
            ].map(({ label, detail, to, icon: Icon }) => (
              <Link key={label} to={to} className="group flex items-center gap-3 rounded-xl p-3 transition hover:bg-slate-50"><div className="grid h-11 w-11 place-items-center rounded-xl bg-blue-50 text-brand-600"><Icon className="h-5 w-5" /></div><div className="min-w-0 flex-1"><p className="text-sm font-semibold">{label}</p><p className="truncate text-xs text-slate-400">{detail}</p></div><ArrowRight className="h-4 w-4 text-slate-300 group-hover:text-brand-600" /></Link>
            ))}
          </div>
        </Card>
      </div>

      <DashboardAnalytics analytics={d} />
    </div>
  );
}
function DashboardAnalytics({ analytics }: { analytics: api.AdminAnalytics }) {
  const admissions = Object.entries(analytics.admissionStatusDistribution).map(([label, value]) => ({
    label: label.replaceAll("_", " "),
    value: Number(value) || 0,
  }));
  const academics = analytics.collegeWiseStudents.map((item, index) => ({
    ...item,
    value: Number(item.value) || 0,
    color: ["#2563eb", "#14b8a6", "#f59e0b", "#8b5cf6", "#f43f5e"][index % 5],
  }));
  const staffCards = [
    { label: "Total Staff", value: analytics.summary.totalStaff ?? 0, icon: Users, tone: "bg-blue-50 text-blue-600" },
    { label: "Principals", value: analytics.summary.totalPrincipals ?? 0, icon: UserRound, tone: "bg-violet-50 text-violet-600" },
    { label: "Students", value: analytics.summary.totalStudents ?? 0, icon: GraduationCap, tone: "bg-amber-50 text-amber-600" },
    { label: "Active Colleges", value: analytics.summary.activeColleges ?? 0, icon: Building2, tone: "bg-emerald-50 text-emerald-600" },
  ];
  const payroll = [
    { label: "Processed", value: 0 },
    { label: "Pending", value: 0 },
    { label: "On Hold", value: 0 },
  ];

  return (
    <section className="mt-6 space-y-6">
      <div className="grid gap-6 xl:grid-cols-2">
        <AnalyticsCard title="Fee Collection" subtitle="College-wise collection trend">
          {analytics.collegeWiseFeeCollection.some((item) => Number(item.value) > 0) ? <ResponsiveContainer width="100%" height="100%">
            <LineChart data={analytics.collegeWiseFeeCollection} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <CartesianGrid stroke="#e2e8f0" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis hide />
              <Tooltip formatter={(value) => `₹${Number(value ?? 0).toLocaleString("en-IN")}`} />
              <Line type="monotone" dataKey="value" stroke="#2563eb" strokeWidth={3} dot={{ r: 4, fill: "#2563eb" }} activeDot={{ r: 6 }} />
            </LineChart>
          </ResponsiveContainer> : <ChartEmpty message="No verified fee collections yet" />}
        </AnalyticsCard>

        <AnalyticsCard title="Admissions" subtitle="Application status distribution">
          {admissions.some((item) => item.value > 0) ? <ResponsiveContainer width="100%" height="100%">
            <BarChart data={admissions} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid stroke="#e2e8f0" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 10 }} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#8b5cf6" radius={[8, 8, 0, 0]} maxBarSize={48} />
            </BarChart>
          </ResponsiveContainer> : <ChartEmpty message="No admission records yet" />}
        </AnalyticsCard>

        <AnalyticsCard title="Academics" subtitle="Students allocated across colleges">
          <div className="flex h-full flex-col items-center gap-3 sm:flex-row">
            <div className="h-full min-h-52 flex-1">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={academics.length ? academics : [{ label: "No data", value: 1, color: "#e2e8f0" }]} dataKey="value" nameKey="label" innerRadius={58} outerRadius={82} paddingAngle={academics.length ? 3 : 0} stroke="none">
                    {(academics.length ? academics : [{ label: "No data", value: 1, color: "#e2e8f0" }]).map((item) => <Cell key={item.label} fill={item.color} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="w-full space-y-2 sm:w-48">
              {academics.slice(0, 5).map((item) => <div key={item.label} className="flex items-center gap-2 text-xs"><span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: item.color }} /><span className="min-w-0 flex-1 truncate text-slate-500">{item.label}</span><b>{item.value}</b></div>)}
            </div>
          </div>
        </AnalyticsCard>

        <AnalyticsCard title="Payroll" subtitle="Payroll processing allocation">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={payroll} margin={{ top: 20, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid stroke="#e2e8f0" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#f59e0b" radius={[8, 8, 0, 0]} maxBarSize={52} />
            </BarChart>
          </ResponsiveContainer>
          <p className="-mt-6 text-center text-xs text-slate-400">Awaiting payroll API data</p>
        </AnalyticsCard>
      </div>

      <div>
        <Card className="p-6">
          <div className="flex items-start justify-between"><div><h2 className="font-bold">Staff Overview</h2><p className="mt-1 text-xs text-slate-500">Allocated people and institutions</p></div><CheckCircle2 className="h-5 w-5 text-emerald-600" /></div>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            {staffCards.map(({ label, value, icon: Icon, tone }) => <div key={label} className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4"><div className={`grid h-10 w-10 place-items-center rounded-xl ${tone}`}><Icon className="h-5 w-5" /></div><p className="mt-4 text-2xl font-black">{value}</p><p className="mt-1 text-xs font-semibold text-slate-500">{label}</p></div>)}
          </div>
        </Card>
      </div>
    </section>
  );
}

function AnalyticsCard({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return <Card className="p-6"><h2 className="font-bold">{title}</h2><p className="mt-1 text-xs text-slate-500">{subtitle}</p><div className="mt-4 h-64">{children}</div></Card>;
}

function ChartEmpty({ message }: { message: string }) {
  return <div className="grid h-full place-items-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 text-center"><div><Activity className="mx-auto h-7 w-7 text-slate-300" /><p className="mt-3 text-sm font-semibold text-slate-500">{message}</p><p className="mt-1 text-xs text-slate-400">This chart updates automatically from the database.</p></div></div>;
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
