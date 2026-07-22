import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Download,
  Eye,
  FileCheck2,
  FileText,
  Filter,
  GraduationCap,
  Printer,
  QrCode,
  RotateCcw,
  TrendingUp,
  UserCheck,
  Users,
  X,
  XCircle,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Modal } from "@/components/common/Modal";
import { PaymentQrManager } from "@/components/colleges/PaymentQrManager";
import { Select as ResponsiveSelect } from "@/components/common/Select";
import {
  getAdmissionAnalytics,
  type AdmissionAnalytics,
  type AdmissionAnalyticsRow,
  type AdmissionGroup,
} from "@/features/reports/api";
import { handleApiError } from "@/lib/handleApiError";
import { exportCollegeExcel } from "@/lib/collegeExcel";
import { localDateString } from "@/lib/date";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";

const date = localDateString;
const initial = () => {
  const end = new Date(),
    start = new Date();
  start.setMonth(end.getMonth() - 6);
  return {
    academicYear: "",
    collegeId: "",
    departmentId: "",
    course: "",
    year: "",
    divisionId: "",
    status: "",
    range: "custom",
    studentName: "",
    admissionNumber: "",
    mobile: "",
    from: date(start),
    to: date(end),
    sort: "newest",
  };
};
type Filters = ReturnType<typeof initial>;
type Drill = {
  departmentKey?: string;
  department?: string;
  yearKey?: string;
  year?: string;
  divisionKey?: string;
  division?: string;
};
const pendingStatuses = [
  "SUBMITTED",
  "STUDENT_SECTION_REVIEW_PENDING",
  "STUDENT_SECTION_APPROVED",
  "PRINCIPAL_REVIEW_PENDING",
];

export function AdmissionAnalyticsDashboard() {
  const { isRole } = useAuth();
  const isPrincipal = isRole([ROLES.PRINCIPAL]);
  const [qrManagerOpen, setQrManagerOpen] = useState(false);
  const [draft, setDraft] = useState<Filters>(initial);
  const [applied, setApplied] = useState<Filters>(initial);
  const [data, setData] = useState<AdmissionAnalytics>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [drill, setDrill] = useState<Drill>({});
  const [selected, setSelected] = useState<AdmissionAnalyticsRow>();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [trendMode, setTrendMode] = useState<"daily" | "weekly" | "monthly" | "yearly">("daily");
  const load = (f = applied, p = page, s = size) => {
    setLoading(true);
    setError("");
    getAdmissionAnalytics({
      academicYear: f.academicYear,
      collegeId: f.collegeId,
      departmentId: f.departmentId,
      status: f.status,
      from: f.from,
      to: f.to,
      studentName: f.studentName,
      admissionNumber: f.admissionNumber,
      mobile: f.mobile,
      page: p,
      size: s,
      sort: f.sort,
    })
      .then(setData)
      .catch((e) => {
        const m = handleApiError(e).message;
        setError(m);
        toast.error(m);
      })
      .finally(() => setLoading(false));
  };
  useEffect(() => load(applied, page, size), [page, size]);
  const years = useMemo(
    () => [...new Set([...(data?.rows ?? []).map((r) => r.academicYear)])].sort(),
    [data],
  );
  const colleges = useMemo(
    () =>
      uniqueBy(data?.rows ?? [], (r) => r.collegeId).map((r) => ({
        id: r.collegeId,
        name: r.college,
      })),
    [data],
  );
  const departments = useMemo(() => data?.departments ?? [], [data]);
  const visibleRows = useMemo(
    () =>
      (data?.rows ?? []).filter(
        (r) =>
          (!applied.course || r.department === applied.course) &&
          (!applied.year || r.year === applied.year) &&
          (!applied.divisionId || String(r.divisionId) === applied.divisionId),
      ),
    [data, applied],
  );
  const apply = () => {
    setApplied(draft);
    setPage(0);
    setDrill({});
    load(draft, 0, size);
  };
  const reset = () => {
    const f = initial();
    setDraft(f);
    setApplied(f);
    setPage(0);
    setDrill({});
    load(f, 0, size);
  };
  const quick = (kind: string) => {
    const f = { ...draft, status: "", from: draft.from, to: draft.to };
    const now = new Date();
    if (kind === "approved") f.status = "PRINCIPAL_APPROVED";
    if (kind === "pending") f.status = "PRINCIPAL_REVIEW_PENDING";
    if (kind === "rejected") f.status = "PRINCIPAL_REJECTED";
    if (kind === "today") {
      f.from = date(now);
      f.to = date(now);
    }
    if (kind === "week") {
      const x = new Date();
      x.setDate(x.getDate() - 6);
      f.from = date(x);
      f.to = date(now);
    }
    if (kind === "month") {
      const x = new Date(now.getFullYear(), now.getMonth(), 1);
      f.from = date(x);
      f.to = date(now);
    }
    setDraft(f);
    setApplied(f);
    setPage(0);
    load(f, 0, size);
  };
  const fetchExportRows = async (selectedOnly = false) => {
    if (selectedOnly) return visibleRows.filter((r) => selectedIds.has(r.id));
    const params = {
      academicYear: applied.academicYear,
      collegeId: applied.collegeId,
      departmentId: applied.departmentId,
      status: applied.status,
      from: applied.from,
      to: applied.to,
      studentName: applied.studentName,
      admissionNumber: applied.admissionNumber,
      mobile: applied.mobile,
      size: 100,
      sort: applied.sort,
    };
    const first = await getAdmissionAnalytics({ ...params, page: 0 });
    const remaining = await Promise.all(
      Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
        getAdmissionAnalytics({ ...params, page: index + 1 }),
      ),
    );
    return [...first.rows, ...remaining.flatMap((result) => result.rows)].filter(
      (r) =>
        (!applied.course || r.department === applied.course) &&
        (!applied.year || r.year === applied.year) &&
        (!applied.divisionId || String(r.divisionId) === applied.divisionId),
    );
  };
  const exportLocal = async (type: "csv" | "excel" | "pdf", selectedOnly = false) => {
    try {
      const exportRows = await fetchExportRows(selectedOnly);
      const collegeNames = [...new Set(exportRows.map((r) => r.college).filter(Boolean))];
      const collegeName =
        collegeNames.length === 1
          ? collegeNames[0]
          : applied.collegeId
            ? colleges.find((c) => String(c.id) === applied.collegeId)?.name || "Selected College"
            : "All Jadhavar Colleges";
      if (type === "csv") {
        const csv = [
          "Admission Number,Student,Department,Year,Status,Submitted",
          ...exportRows.map((r) =>
            [r.admissionNumber, r.studentName, r.department, r.year, r.status, r.submittedAt]
              .map(q)
              .join(","),
          ),
        ].join("\n");
        download(new Blob([csv], { type: "text/csv" }), "admissions-filtered.csv");
        return;
      }
      if (type === "excel") {
        await exportCollegeExcel({
          filename: "admissions-filtered.xlsx",
          sheetName: "Admissions",
          title: "Admission Analytics Report",
          collegeName,
          subtitle: "Official filtered admission register",
          metadata: [
            ["Academic Year", applied.academicYear || "All"],
            ["Reporting Period", `${applied.from} to ${applied.to}`],
            [
              "Department",
              departments.find((d) => String(d.key) === applied.departmentId)?.label ||
                applied.course ||
                "All Departments",
            ],
            ["Applications in Export", exportRows.length],
          ],
          headers: [
            "Admission Number",
            "Student Name",
            "Department",
            "Year",
            "Status",
            "Submitted At",
          ],
          rows: exportRows.map((r) => [
            r.admissionNumber,
            r.studentName,
            r.department,
            r.year,
            labelStatus(r.status),
            fmt(r.submittedAt),
          ]),
          widths: [22, 30, 30, 18, 28, 24],
          orientation: "landscape",
        });
        return;
      }
      const jsPDF = (await import("jspdf")).default;
      const autoTable = (await import("jspdf-autotable")).default;
      const doc = new jsPDF({ orientation: "landscape" });
      doc.setFontSize(16);
      doc.setFont("helvetica", "bold");
      doc.text(collegeName, 14, 14);
      doc.setFontSize(12);
      doc.text("Executive Admission Analytics Report", 14, 22);
      autoTable(doc, {
        startY: 28,
        head: [["Admission No.", "Student", "Department", "Year", "Status", "Submitted"]],
        body: exportRows.map((r) => [
          r.admissionNumber,
          r.studentName,
          r.department,
          r.year,
          labelStatus(r.status),
          fmt(r.submittedAt),
        ]),
        headStyles: { fillColor: [37, 99, 235] },
        didDrawPage: (hook) => {
          doc.setFontSize(8);
          doc.setFont("helvetica", "normal");
          doc.text(
            `${collegeName} · Admission Analytics Report`,
            14,
            doc.internal.pageSize.height - 7,
          );
          doc.text(
            `Page ${hook.pageNumber}`,
            doc.internal.pageSize.width - 24,
            doc.internal.pageSize.height - 7,
          );
        },
      });
      doc.save("admissions-filtered.pdf");
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  const s = data?.summary;
  const trend = useMemo(() => trendData(data?.trend ?? [], trendMode), [data, trendMode]);
  return (
    <div className="page-container space-y-6 pb-12">
      <header className="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
        <div>
          <p className="text-xs font-bold uppercase tracking-[.18em] text-brand-600">
            Executive analytics
          </p>
          <h1 className="page-title mt-1">Admission Analytics Dashboard</h1>
          <p className="mt-1 text-sm text-slate-500">
            Monitor the admission pipeline, bottlenecks, outcomes and individual applications.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {isPrincipal && (
            <Button variant="secondary" onClick={() => setQrManagerOpen(true)}>
              <QrCode className="mr-2 h-4 w-4" />
              Change Payment QR
            </Button>
          )}
          <Button variant="secondary" disabled={!data} onClick={() => exportLocal("pdf")}>
            <FileText className="mr-2 h-4 w-4" />
            PDF
          </Button>
          <Button variant="secondary" disabled={!data} onClick={() => exportLocal("excel")}>
            <Download className="mr-2 h-4 w-4" />
            Excel
          </Button>
          <Button variant="secondary" disabled={!data} onClick={() => exportLocal("csv")}>
            <Download className="mr-2 h-4 w-4" />
            CSV
          </Button>
          <Button variant="secondary" onClick={() => window.print()}>
            <Printer className="mr-2 h-4 w-4" />
            Print
          </Button>
        </div>
      </header>
      <Modal
        open={qrManagerOpen}
        onClose={() => setQrManagerOpen(false)}
        title="Change College Payment QR"
        description="This QR code is shown to students when they submit fee payment proof."
        size="xl"
      >
        <PaymentQrManager />
      </Modal>
      <Card className="sticky top-0 z-20 border-slate-200/80 bg-white/95 p-5 shadow-sm backdrop-blur print:hidden">
        <div className="mb-4 flex items-center gap-2 text-sm font-bold">
          <Filter className="h-4 w-4 text-brand-600" />
          Admission filters
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          <Select
            label="Academic Year"
            value={draft.academicYear}
            set={(v) => setDraft((x) => ({ ...x, academicYear: v }))}
            options={years}
            all="All academic years"
          />
          <Select
            label="College"
            value={draft.collegeId}
            set={(v) => setDraft((x) => ({ ...x, collegeId: v }))}
            options={colleges.map((x) => ({ v: String(x.id), l: x.name }))}
            all="All colleges"
          />
          <Select
            label="Department"
            value={draft.departmentId}
            set={(v) =>
              setDraft((x) => ({ ...x, departmentId: v, course: "", year: "", divisionId: "" }))
            }
            options={departments.map((x) => ({ v: x.key, l: x.label }))}
            all="All departments"
          />
          <Select
            label="Course"
            value={draft.course}
            set={(v) => setDraft((x) => ({ ...x, course: v }))}
            options={departments.map((x) => x.label)}
            all="All courses"
          />
          <Select
            label="Year"
            value={draft.year}
            set={(v) => setDraft((x) => ({ ...x, year: v, divisionId: "" }))}
            options={(data?.years ?? [])
              .filter((x) => !draft.departmentId || x.parentKey === draft.departmentId)
              .map((x) => x.label)}
            all="All years"
          />
          <Select
            label="Division"
            value={draft.divisionId}
            set={(v) => setDraft((x) => ({ ...x, divisionId: v }))}
            options={(data?.divisions ?? []).map((x) => ({ v: x.key, l: x.label }))}
            all="All divisions"
          />
          <Select
            label="Admission Status"
            value={draft.status}
            set={(v) => setDraft((x) => ({ ...x, status: v }))}
            options={[
              "SUBMITTED",
              "STUDENT_SECTION_REVIEW_PENDING",
              "STUDENT_SECTION_APPROVED",
              "PRINCIPAL_REVIEW_PENDING",
              "PRINCIPAL_APPROVED",
              "STUDENT_SECTION_REJECTED",
              "PRINCIPAL_REJECTED",
              "CANCELLED",
            ]}
            all="All statuses"
          />
          <Field
            label="Date From"
            type="date"
            value={draft.from}
            set={(v) => setDraft((x) => ({ ...x, from: v }))}
          />
          <Field
            label="Date To"
            type="date"
            value={draft.to}
            set={(v) => setDraft((x) => ({ ...x, to: v }))}
          />
          <Field
            label="Student Name"
            value={draft.studentName}
            set={(v) => setDraft((x) => ({ ...x, studentName: v }))}
          />
          <Field
            label="Admission Number"
            value={draft.admissionNumber}
            set={(v) => setDraft((x) => ({ ...x, admissionNumber: v }))}
          />
          <Field
            label="Mobile Number"
            value={draft.mobile}
            set={(v) => setDraft((x) => ({ ...x, mobile: v }))}
          />
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Button onClick={apply}>Apply Filters</Button>
          <Button variant="secondary" onClick={reset}>
            <RotateCcw className="mr-2 h-4 w-4" />
            Reset
          </Button>
        </div>
      </Card>
      <div className="flex flex-wrap gap-2 print:hidden">
        {[
          ["all", "All"],
          ["approved", "Approved"],
          ["pending", "Pending"],
          ["rejected", "Rejected"],
          ["today", "Today's Admissions"],
          ["week", "This Week"],
          ["month", "This Month"],
        ].map(([k, l]) => (
          <button
            key={k}
            onClick={() => quick(k)}
            className="rounded-full border bg-white px-4 py-2 text-xs font-bold text-slate-600 transition hover:border-brand-300 hover:text-brand-700"
          >
            {l}
          </button>
        ))}
        <button
          onClick={() => {
            const f = { ...draft, status: "SUBMITTED" };
            setDraft(f);
            setApplied(f);
            load(f, 0, size);
          }}
          className="rounded-full border bg-white px-4 py-2 text-xs font-bold"
        >
          Pending Documents
        </button>
        <button
          onClick={() => toast.info("Fee-pending students are highlighted in the insights panel.")}
          className="rounded-full border bg-white px-4 py-2 text-xs font-bold"
        >
          Pending Fees
        </button>
      </div>
      {loading && !data ? (
        <Skeleton />
      ) : error && !data ? (
        <ErrorCard message={error} retry={() => load(applied, page, size)} />
      ) : data && s ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
            <Kpi
              icon={Users}
              label="Total Applications"
              value={s.totalApplications}
              sub="Within selected scope"
              tone="blue"
            />
            <Kpi
              icon={CheckCircle2}
              label="Approved Admissions"
              value={s.approvedAdmissions}
              sub={`${s.successRate}% success rate`}
              tone="green"
            />
            <Kpi
              icon={Clock3}
              label="Pending Reviews"
              value={s.pendingReviews}
              sub="Awaiting action"
              tone="amber"
            />
            <Kpi
              icon={XCircle}
              label="Rejected Applications"
              value={s.rejectedApplications}
              sub="Review reasons"
              tone="red"
            />
            <Kpi
              icon={TrendingUp}
              label="Today's Applications"
              value={s.todayApplications}
              sub="Received today"
              tone="violet"
            />
            <Kpi
              icon={UserCheck}
              label="Admission Success Rate"
              value={`${s.successRate}%`}
              sub="Approved / applications"
              tone="cyan"
            />
            <Kpi
              icon={FileCheck2}
              label="Documents Pending"
              value={s.documentsPending}
              sub="Incomplete details"
              tone="amber"
            />
            <Kpi
              icon={GraduationCap}
              label="Fee Pending"
              value={s.feePending}
              sub="Minimum fee not met"
              tone="red"
            />
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Alert
              count={s.pendingReviews}
              text="Applications Pending Review"
              click={() => quick("pending")}
            />
            <Alert
              count={s.documentsPending}
              text="Students Pending Documents"
              click={() => quick("all")}
            />
            <Alert
              count={s.feePending}
              text="Students Pending Fee Payment"
              click={() => toast.info("Fee-pending records are listed in Smart Insights")}
            />
            <Alert
              count={
                (data.rows ?? []).filter(
                  (r) =>
                    r.status.includes("REJECTED") &&
                    r.submittedAt.slice(0, 10) === date(new Date()),
                ).length
              }
              text="Applications Rejected Today"
              click={() => quick("rejected")}
            />
          </div>
          <div className="grid gap-6 xl:grid-cols-3">
            <Card className="p-5 xl:col-span-2">
              <Title
                icon={TrendingUp}
                title="Admission trend"
                sub="Applications, approvals and rejections over time"
                action={
                  <div className="flex rounded-lg bg-slate-100 p-1">
                    {(["daily", "weekly", "monthly", "yearly"] as const).map((m) => (
                      <button
                        key={m}
                        onClick={() => setTrendMode(m)}
                        className={`rounded-md px-3 py-1.5 text-xs font-bold capitalize ${trendMode === m ? "bg-white text-brand-700 shadow-sm" : "text-slate-500"}`}
                      >
                        {m}
                      </button>
                    ))}
                  </div>
                }
              />
              <div className="mt-4 h-72">
                <ResponsiveContainer>
                  <LineChart data={trend}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                    <YAxis />
                    <Tooltip />
                    <Line dataKey="applications" stroke="#2563eb" strokeWidth={3} />
                    <Line dataKey="approved" stroke="#10b981" strokeWidth={2} />
                    <Line dataKey="rejected" stroke="#ef4444" strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>
            <Card className="p-5">
              <Title
                icon={BarChart3}
                title="Status distribution"
                sub="Approved, pending, rejected and cancelled"
              />
              <div className="h-64">
                <ResponsiveContainer>
                  <PieChart>
                    <Pie
                      data={[
                        { name: "Approved", value: s.approvedAdmissions },
                        { name: "Pending", value: s.pendingReviews },
                        { name: "Rejected", value: s.rejectedApplications },
                        {
                          name: "Cancelled",
                          value: Math.max(
                            0,
                            s.totalApplications -
                              s.approvedAdmissions -
                              s.pendingReviews -
                              s.rejectedApplications,
                          ),
                        },
                      ]}
                      dataKey="value"
                      innerRadius={55}
                      outerRadius={85}
                    >
                      {["#10b981", "#f59e0b", "#ef4444", "#94a3b8"].map((c) => (
                        <Cell key={c} fill={c} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </div>
          <Card className="p-5">
            <Title
              icon={TrendingUp}
              title="Admission funnel"
              sub="Conversion through every admission stage"
            />
            <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
              {data.funnel.map((f, i) => (
                <div key={f.key} className="relative rounded-xl border bg-slate-50 p-4">
                  <span className="text-[10px] font-bold uppercase text-slate-400">
                    Stage {i + 1}
                  </span>
                  <b className="mt-2 block text-2xl">{f.count}</b>
                  <p className="text-xs font-semibold text-slate-600">{f.label}</p>
                  <div className="mt-3 h-1.5 rounded bg-slate-200">
                    <div
                      className="h-full rounded bg-brand-500"
                      style={{
                        width: `${s.totalApplications ? (f.count * 100) / s.totalApplications : 0}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </Card>
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
            <Card className="overflow-hidden">
              <Breadcrumb drill={drill} set={setDrill} />
              <DrillTable
                data={data}
                drill={drill}
                set={setDrill}
                rows={visibleRows}
                selectedIds={selectedIds}
                setSelectedIds={setSelectedIds}
                view={setSelected}
              />
            </Card>
            <Insights data={data} view={setSelected} />
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <Card className="p-5">
              <Title
                icon={BarChart3}
                title="Department comparison"
                sub="Application volumes and admission rates"
              />
              <div className="mt-4 h-72">
                <ResponsiveContainer>
                  <BarChart data={data.departments}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="applications" fill="#2563eb" radius={[6, 6, 0, 0]} />
                    <Bar dataKey="approved" fill="#10b981" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
            <Card className="p-5">
              <Title
                icon={CheckCircle2}
                title="Admission completion"
                sub="Department-wise approval progress"
              />
              <div className="mt-5 space-y-4">
                {data.departments.slice(0, 10).map((x) => (
                  <Progress key={x.key} label={x.label} value={x.admissionRate} />
                ))}
              </div>
            </Card>
          </div>
          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b p-5">
              <Title
                icon={FileText}
                title="Student admission register"
                sub={`${data.totalElements} filtered admission records`}
              />
              <div className="flex items-center gap-2">
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="h-9 rounded-lg border px-2 text-xs"
                >
                  <option>20</option>
                  <option>50</option>
                  <option>100</option>
                </select>
                <Button
                  variant="secondary"
                  disabled={!selectedIds.size}
                  onClick={() => exportLocal("excel", true)}
                >
                  Export selected ({selectedIds.size})
                </Button>
              </div>
            </div>
            <StudentTable
              rows={visibleRows}
              selectedIds={selectedIds}
              setSelectedIds={setSelectedIds}
              view={setSelected}
            />
            <Pagination
              page={data.page}
              pages={data.totalPages}
              total={data.totalElements}
              set={setPage}
            />
          </Card>
        </>
      ) : null}
      {selected && <Drawer row={selected} close={() => setSelected(undefined)} />}
    </div>
  );
}

function DrillTable({
  data,
  drill,
  set,
  rows,
  selectedIds,
  setSelectedIds,
  view,
}: {
  data: AdmissionAnalytics;
  drill: Drill;
  set: (d: Drill) => void;
  rows: AdmissionAnalyticsRow[];
  selectedIds: Set<number>;
  setSelectedIds: (s: Set<number>) => void;
  view: (r: AdmissionAnalyticsRow) => void;
}) {
  if (!drill.departmentKey)
    return (
      <GroupTable
        title="Department analytics"
        rows={data.departments}
        first="Department"
        click={(g) => set({ departmentKey: g.key, department: g.label })}
      />
    );
  if (!drill.yearKey)
    return (
      <GroupTable
        title="Course / Year analytics"
        rows={data.years.filter((y) => y.parentKey === drill.departmentKey)}
        first="Year"
        click={(g) => set({ ...drill, yearKey: g.key, year: g.label })}
      />
    );
  if (!drill.divisionKey)
    return (
      <GroupTable
        title="Division analytics"
        rows={data.divisions.filter((d) => d.parentKey === drill.yearKey)}
        first="Division"
        teacher
        click={(g) => set({ ...drill, divisionKey: g.key, division: g.label })}
      />
    );
  return (
    <StudentTable
      rows={rows.filter((r) => String(r.divisionId) === drill.divisionKey)}
      selectedIds={selectedIds}
      setSelectedIds={setSelectedIds}
      view={view}
    />
  );
}
function GroupTable({
  title,
  rows,
  first,
  teacher = false,
  click,
}: {
  title: string;
  rows: AdmissionGroup[];
  first: string;
  teacher?: boolean;
  click: (g: AdmissionGroup) => void;
}) {
  return (
    <div>
      <div className="p-5">
        <h2 className="font-bold">{title}</h2>
        <p className="text-xs text-slate-500">Click a row to continue without leaving this page.</p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[800px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              {[
                first,
                "Applications",
                "Approved",
                "Pending",
                "Rejected",
                "Admission Rate",
                ...(teacher ? ["Class Teacher"] : []),
                "Action",
              ].map((h) => (
                <th key={h} className="px-4 py-3">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((g) => (
              <tr
                key={g.key}
                onClick={() => click(g)}
                className="cursor-pointer border-t hover:bg-brand-50/40"
              >
                <td className="px-4 py-4 font-bold">{g.label}</td>
                <td className="px-4 py-4">{g.applications}</td>
                <td className="px-4 py-4 text-emerald-600">{g.approved}</td>
                <td className="px-4 py-4 text-amber-600">{g.pending}</td>
                <td className="px-4 py-4 text-rose-600">{g.rejected}</td>
                <td className="px-4 py-4">
                  <Percent v={g.admissionRate} />
                </td>
                {teacher && <td className="px-4 py-4">{g.classTeacher}</td>}
                <td className="px-4 py-4 font-bold text-brand-700">
                  View <ChevronRight className="inline h-4 w-4" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rows.length && <Empty />}
      </div>
    </div>
  );
}
function StudentTable({
  rows,
  selectedIds,
  setSelectedIds,
  view,
}: {
  rows: AdmissionAnalyticsRow[];
  selectedIds: Set<number>;
  setSelectedIds: (s: Set<number>) => void;
  view: (r: AdmissionAnalyticsRow) => void;
}) {
  const toggle = (id: number) => {
    const n = new Set(selectedIds);
    if (n.has(id)) n.delete(id);
    else n.add(id);
    setSelectedIds(n);
  };
  return (
    <div className="max-h-[620px] overflow-auto">
      <table className="w-full min-w-[1100px] text-left text-sm">
        <thead className="sticky top-0 z-10 bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            <th className="px-4 py-3">
              <input
                type="checkbox"
                checked={!!rows.length && rows.every((r) => selectedIds.has(r.id))}
                onChange={(e) =>
                  setSelectedIds(e.target.checked ? new Set(rows.map((r) => r.id)) : new Set())
                }
              />
            </th>
            {[
              "Student",
              "Admission Number",
              "Department",
              "Course / Year",
              "Status",
              "Submitted Date",
              "Action",
            ].map((h) => (
              <th key={h} className="px-4 py-3" style={{ resize: "horizontal", overflow: "auto" }}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className="border-t hover:bg-slate-50">
              <td className="px-4 py-3">
                <input
                  type="checkbox"
                  checked={selectedIds.has(r.id)}
                  onChange={() => toggle(r.id)}
                />
              </td>
              <td className="px-4 py-3">
                <div className="flex items-center gap-3">
                  <Avatar name={r.studentName} />
                  <span>
                    <b className="block">{r.studentName}</b>
                    <small className="text-slate-500">{r.phone}</small>
                  </span>
                </div>
              </td>
              <td className="px-4 py-3 font-mono text-xs">{r.admissionNumber}</td>
              <td className="px-4 py-3">{r.department}</td>
              <td className="px-4 py-3">{r.year}</td>
              <td className="px-4 py-3">
                <Badge status={r.status} />
              </td>
              <td className="px-4 py-3">{fmt(r.submittedAt)}</td>
              <td className="px-4 py-3">
                <div className="flex gap-2">
                  <button
                    onClick={() => view(r)}
                    className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700"
                  >
                    <Eye className="mr-1 inline h-3.5 w-3.5" />
                    View
                  </button>
                  <details className="relative">
                    <summary className="cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold">
                      More
                    </summary>
                    <div className="absolute right-0 z-20 mt-1 w-48 rounded-xl border bg-white p-2 shadow-xl">
                      {[
                        "Admission Timeline",
                        "Download Application",
                        "Print Admission Form",
                        "Download Documents",
                        "Add Remarks",
                      ].map((a) => (
                        <button
                          key={a}
                          onClick={() =>
                            a === "Admission Timeline"
                              ? view(r)
                              : a.includes("Print")
                                ? window.print()
                                : toast.info(
                                    `${a} will use the admission record for ${r.studentName}.`,
                                  )
                          }
                          className="block w-full rounded-lg px-3 py-2 text-left text-xs hover:bg-slate-50"
                        >
                          {a}
                        </button>
                      ))}
                    </div>
                  </details>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length && <Empty />}
    </div>
  );
}
function Insights({
  data,
  view,
}: {
  data: AdmissionAnalytics;
  view: (r: AdmissionAnalyticsRow) => void;
}) {
  const old = data.rows.filter((r) => pendingStatuses.includes(r.status) && r.processingDays > 7);
  const fees = data.rows.filter((r) => r.feePending);
  const docs = data.rows.filter((r) => !r.documentsUploaded);
  const hi = [...data.departments].sort((a, b) => b.admissionRate - a.admissionRate)[0],
    lo = [...data.departments].sort((a, b) => a.admissionRate - b.admissionRate)[0];
  return (
    <Card className="self-start p-5 xl:sticky xl:top-48">
      <Title
        icon={TrendingUp}
        title="Smart Insights"
        sub="Automatically detected admission signals"
      />
      <div className="mt-4 space-y-3">
        <Insight
          tone="red"
          title={`${old.length} pending over 7 days`}
          text={old[0]?.studentName || "No aged applications"}
          click={() => old[0] && view(old[0])}
        />
        <Insight
          tone="green"
          title="Highest admission rate"
          text={hi ? `${hi.label} · ${hi.admissionRate}%` : "No data"}
        />
        <Insight
          tone="amber"
          title="Lowest admission rate"
          text={lo ? `${lo.label} · ${lo.admissionRate}%` : "No data"}
        />
        <Insight
          tone="orange"
          title={`${docs.length} pending documents`}
          text={docs[0]?.studentName || "No pending documents"}
          click={() => docs[0] && view(docs[0])}
        />
        <Insight
          tone="red"
          title={`${fees.length} pending fees`}
          text={fees[0]?.studentName || "No pending fees"}
          click={() => fees[0] && view(fees[0])}
        />
        <Insight
          tone="blue"
          title="Average processing time"
          text={`${data.summary.averageProcessingDays} days`}
        />
      </div>
    </Card>
  );
}
function Drawer({ row, close }: { row: AdmissionAnalyticsRow; close: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/35" onMouseDown={close}>
      <aside
        onMouseDown={(e) => e.stopPropagation()}
        className="h-full w-full max-w-2xl overflow-y-auto bg-white shadow-2xl"
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b bg-white/95 p-5 backdrop-blur">
          <div className="flex items-center gap-3">
            <Avatar name={row.studentName} large />
            <div>
              <h2 className="font-bold">{row.studentName}</h2>
              <p className="text-xs text-slate-500">
                {row.admissionNumber} · {row.admissionReferenceNumber}
              </p>
            </div>
          </div>
          <button onClick={close} className="rounded-lg p-2 hover:bg-slate-100">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="space-y-6 p-5">
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              ["Gender / DOB", `${row.gender} · ${row.dateOfBirth}`],
              ["Email", row.email],
              ["Phone", row.phone],
              ["Guardian", `${row.guardianName} · ${row.guardianPhone}`],
              ["College", row.college],
              ["Department", row.department],
              ["Academic Year", row.academicYear],
              ["Year / Division", `${row.year} · ${row.division}`],
              ["Class Teacher", row.classTeacher],
              ["Submitted", fmt(row.submittedAt)],
              ["Approved", row.approvedAt ? fmt(row.approvedAt) : "Pending"],
              ["Processing Time", `${row.processingDays} days`],
            ].map(([l, v]) => (
              <Info key={l} label={l} value={v} />
            ))}
          </div>
          <Info label="Address" value={row.address || "—"} />
          <Card className="p-4">
            <div className="flex flex-wrap gap-2">
              <Badge status={row.status} />
              <Flag ok={row.documentsUploaded} yes="Documents uploaded" no="Documents pending" />
              <Flag ok={row.feePaid} yes="Required fee paid" no="Fee pending" />
            </div>
            {row.remarks && (
              <p className="mt-3 text-sm text-slate-600">
                <b>Remarks:</b> {row.remarks}
              </p>
            )}
          </Card>
          <div>
            <h3 className="mb-4 font-bold">Admission Timeline</h3>
            <div className="space-y-0">
              {row.timeline.map((t, i) => (
                <div key={`${t.stage}-${i}`} className="relative flex gap-4 pb-5">
                  <div className="relative z-10 grid h-8 w-8 shrink-0 place-items-center rounded-full bg-emerald-100 text-emerald-700">
                    <CheckCircle2 className="h-4 w-4" />
                  </div>
                  {i < row.timeline.length - 1 && (
                    <span className="absolute left-[15px] top-8 h-full w-px bg-slate-200" />
                  )}
                  <div>
                    <b className="text-sm">{t.stage}</b>
                    <p className="text-xs text-slate-500">
                      {t.date ? fmt(t.date) : "Pending"} · {t.responsibleUser || "System"}
                    </p>
                    {t.remarks && <p className="mt-1 text-xs text-slate-600">{t.remarks}</p>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </aside>
    </div>
  );
}
function Breadcrumb({ drill, set }: { drill: Drill; set: (d: Drill) => void }) {
  return (
    <div className="flex flex-wrap items-center gap-1 border-b bg-slate-50 px-5 py-4 text-sm">
      <button onClick={() => set({})} className="font-bold text-brand-700">
        Admissions
      </button>
      {drill.department && (
        <>
          <ChevronRight className="h-4 w-4" />
          <button
            onClick={() =>
              set({ departmentKey: drill.departmentKey, department: drill.department })
            }
          >
            {drill.department}
          </button>
        </>
      )}
      {drill.year && (
        <>
          <ChevronRight className="h-4 w-4" />
          <button onClick={() => set({ ...drill, divisionKey: undefined, division: undefined })}>
            {drill.year}
          </button>
        </>
      )}
      {drill.division && (
        <>
          <ChevronRight className="h-4 w-4" />
          <span>{drill.division}</span>
          <ChevronRight className="h-4 w-4" />
          <span className="text-slate-500">Students</span>
        </>
      )}
    </div>
  );
}
const tone: Record<string, string> = {
  blue: "bg-blue-50 text-blue-600",
  green: "bg-emerald-50 text-emerald-600",
  amber: "bg-amber-50 text-amber-600",
  red: "bg-rose-50 text-rose-600",
  violet: "bg-violet-50 text-violet-600",
  cyan: "bg-cyan-50 text-cyan-600",
};
function Kpi({
  icon: Icon,
  label,
  value,
  sub,
  tone: t,
}: {
  icon: typeof Users;
  label: string;
  value: string | number;
  sub: string;
  tone: string;
}) {
  return (
    <Card className="p-4 transition hover:-translate-y-1 hover:shadow-md">
      <span className={`inline-flex rounded-xl p-2 ${tone[t]}`}>
        <Icon className="h-4 w-4" />
      </span>
      <b className="mt-3 block text-2xl">{value}</b>
      <p className="mt-1 text-[10px] font-bold uppercase text-slate-500">{label}</p>
      <p className="mt-2 text-[10px] text-slate-400">↗ {sub}</p>
    </Card>
  );
}
function Title({
  icon: Icon,
  title,
  sub,
  action,
}: {
  icon: typeof Users;
  title: string;
  sub: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-3">
        <span className="rounded-lg bg-brand-50 p-2 text-brand-600">
          <Icon className="h-4 w-4" />
        </span>
        <div>
          <h2 className="font-bold">{title}</h2>
          <p className="text-xs text-slate-500">{sub}</p>
        </div>
      </div>
      {action}
    </div>
  );
}
function Select({
  label,
  value,
  set,
  options,
  all,
}: {
  label: string;
  value: string;
  set: (v: string) => void;
  options: (string | { v: string; l: string })[];
  all: string;
}) {
  return (
    <ResponsiveSelect
      label={label}
      value={value}
      onChange={(e) => set(e.target.value)}
      options={[
        { value: "", label: all },
        ...options.map((o) =>
          typeof o === "string" ? { value: o, label: labelStatus(o) } : { value: o.v, label: o.l },
        ),
      ]}
    />
  );
}
function Field({
  label,
  value,
  set,
  type = "text",
}: {
  label: string;
  value: string;
  set: (v: string) => void;
  type?: string;
}) {
  return (
    <label className="text-xs font-semibold text-slate-600">
      {label}
      <input
        type={type}
        value={value}
        onChange={(e) => set(e.target.value)}
        className="mt-1.5 h-10 w-full rounded-lg border px-3 text-sm"
      />
    </label>
  );
}
function Alert({ count, text, click }: { count: number; text: string; click: () => void }) {
  return (
    <button
      onClick={click}
      className="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 p-4 text-left text-amber-900 transition hover:-translate-y-0.5"
    >
      <span>
        <b className="text-xl">{count}</b>
        <span className="ml-2 text-sm font-semibold">{text}</span>
      </span>
      <ChevronRight className="h-4 w-4" />
    </button>
  );
}
function Insight({
  tone,
  title,
  text,
  click,
}: {
  tone: string;
  title: string;
  text: string;
  click?: () => void;
}) {
  return (
    <button
      disabled={!click}
      onClick={click}
      className="flex w-full items-start gap-3 rounded-xl border p-3 text-left hover:bg-slate-50"
    >
      <i
        className={`mt-1 h-2.5 w-2.5 rounded-full ${tone === "green" ? "bg-emerald-500" : tone === "blue" ? "bg-blue-500" : tone === "amber" || tone === "orange" ? "bg-amber-500" : "bg-rose-500"}`}
      />
      <span>
        <b className="block text-sm">{title}</b>
        <small className="text-slate-500">{text}</small>
      </span>
    </button>
  );
}
function Progress({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-sm">
        <b>{label}</b>
        <span>{value}%</span>
      </div>
      <div className="h-2.5 rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full ${value >= 75 ? "bg-emerald-500" : value >= 50 ? "bg-amber-500" : "bg-rose-500"}`}
          style={{ width: `${value}%` }}
        />
      </div>
    </div>
  );
}
function Badge({ status }: { status: string }) {
  const l = labelStatus(status),
    c = status.includes("APPROVED")
      ? "bg-emerald-100 text-emerald-700"
      : status.includes("REJECTED")
        ? "bg-rose-100 text-rose-700"
        : status === "CANCELLED"
          ? "bg-slate-100 text-slate-600"
          : status.includes("REVIEW")
            ? "bg-amber-100 text-amber-700"
            : "bg-blue-100 text-blue-700";
  return <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${c}`}>{l}</span>;
}
function Flag({ ok, yes, no }: { ok: boolean; yes: string; no: string }) {
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${ok ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}
    >
      {ok ? yes : no}
    </span>
  );
}
function Percent({ v }: { v: number }) {
  return (
    <b className={v >= 75 ? "text-emerald-600" : v >= 50 ? "text-amber-600" : "text-rose-600"}>
      {v}%
    </b>
  );
}
function Avatar({ name, large = false }: { name: string; large?: boolean }) {
  return (
    <span
      className={`grid shrink-0 place-items-center rounded-full bg-brand-100 font-bold text-brand-700 ${large ? "h-12 w-12 text-lg" : "h-9 w-9 text-sm"}`}
    >
      {name
        .split(/\s+/)
        .slice(0, 2)
        .map((x) => x[0])
        .join("")}
    </span>
  );
}
function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-1 text-sm font-semibold">{value}</p>
    </div>
  );
}
function Pagination({
  page,
  pages,
  total,
  set,
}: {
  page: number;
  pages: number;
  total: number;
  set: (n: number) => void;
}) {
  return (
    <div className="flex items-center justify-between border-t px-5 py-4 text-sm">
      <span>
        {total} records · Page {pages ? `${page + 1} of ${pages}` : "0 of 0"}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" disabled={page <= 0} onClick={() => set(page - 1)}>
          Previous
        </Button>
        <Button variant="secondary" disabled={page + 1 >= pages} onClick={() => set(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  );
}
function Empty() {
  return (
    <div className="grid min-h-40 place-items-center p-8 text-center text-sm text-slate-400">
      <div>
        <FileText className="mx-auto mb-2 h-8 w-8 opacity-40" />
        <b className="text-slate-600">No admission records found.</b>
        <p>Try changing filters.</p>
      </div>
    </div>
  );
}
function Skeleton() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="h-32 rounded-xl bg-slate-100" />
        ))}
      </div>
      <div className="h-80 rounded-xl bg-slate-100" />
    </div>
  );
}
function ErrorCard({ message, retry }: { message: string; retry: () => void }) {
  return (
    <Card className="grid min-h-72 place-items-center text-center">
      <div>
        <AlertTriangle className="mx-auto h-10 w-10 text-rose-500" />
        <h2 className="mt-3 font-bold">Unable to load admission analytics</h2>
        <p className="my-3 text-sm text-slate-500">{message}</p>
        <Button onClick={retry}>Retry</Button>
      </div>
    </Card>
  );
}
function labelStatus(s: string) {
  return s
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
function fmt(s: string) {
  return new Date(s).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
}
function uniqueBy<T>(a: T[], k: (x: T) => string | number) {
  return [...new Map(a.map((x) => [k(x), x])).values()];
}
function q(v: unknown) {
  return `"${String(v ?? "").replaceAll('"', '""')}"`;
}
function download(b: Blob, n: string) {
  const u = URL.createObjectURL(b),
    a = document.createElement("a");
  a.href = u;
  a.download = n;
  a.click();
  URL.revokeObjectURL(u);
}
function trendData(data: AdmissionAnalytics["trend"], mode: string) {
  if (mode === "daily") return data.map((x) => ({ ...x, label: x.date.slice(5) }));
  const groups = data.reduce<Record<string, typeof data>>((a, x) => {
    const d = new Date(x.date + "T00:00:00");
    let k = x.date.slice(0, 7);
    if (mode === "weekly") {
      d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
      k = date(d);
    }
    if (mode === "yearly") k = x.date.slice(0, 4);
    (a[k] ??= []).push(x);
    return a;
  }, {});
  return Object.entries(groups).map(([label, g]) => ({
    label,
    applications: g.reduce((n, x) => n + x.applications, 0),
    approved: g.reduce((n, x) => n + x.approved, 0),
    rejected: g.reduce((n, x) => n + x.rejected, 0),
  }));
}
