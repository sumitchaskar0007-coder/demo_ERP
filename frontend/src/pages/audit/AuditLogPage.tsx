import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  BarChart3,
  CalendarDays,
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
  RefreshCw,
  RotateCcw,
  Search,
  TrendingUp,
  Users,
  WalletCards,
  X,
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
import {
  getBusinessActivityDashboard,
  type BusinessActivityDashboard,
  type BusinessActivityRow,
} from "@/features/audit/api";
import { handleApiError } from "@/lib/handleApiError";
import { exportCollegeExcel } from "@/lib/collegeExcel";
import { useAuth } from "@/features/auth/authStore";
import { localDateString } from "@/lib/date";

const iso = localDateString;
const defaults = () => {
  const end = new Date(),
    start = new Date();
  start.setDate(end.getDate() - 29);
  return {
    academicYear: "",
    departmentId: "",
    module: "",
    role: "",
    userId: "",
    action: "",
    dateFrom: iso(start),
    dateTo: iso(end),
    search: "",
    sort: "newest",
  };
};
type Filters = ReturnType<typeof defaults>;
const moduleValues = [
  "ADMISSION",
  "STUDENT_SECTION",
  "ATTENDANCE",
  "FEE",
  "ACADEMIC",
  "STAFF",
  "USER",
  "REPORT",
  "DEPARTMENT",
  "COLLEGE",
];
const actions = [
  "CREATE",
  "UPDATE",
  "APPROVE",
  "REJECT",
  "VERIFY",
  "SUBMIT",
  "PRINT",
  "ASSIGN",
  "MARK_ATTENDANCE",
  "VIEW_REPORT",
  "EXPORT",
  "ACTIVATE",
  "DEACTIVATE",
];

export function AuditLogPage() {
  const { user } = useAuth();
  const [draft, setDraft] = useState<Filters>(defaults);
  const [applied, setApplied] = useState<Filters>(defaults);
  const [data, setData] = useState<BusinessActivityDashboard>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [selected, setSelected] = useState<BusinessActivityRow>();
  const [timelineRange, setTimelineRange] = useState("today");
  const [visible, setVisible] = useState(
    new Set(["date", "user", "role", "department", "module", "action", "description", "status"]),
  );
  const load = useCallback((f: Filters, p: number, s: number) => {
    setLoading(true);
    setError("");
    getBusinessActivityDashboard({ ...f, page: p, size: s })
      .then(setData)
      .catch((e) => {
        const m = handleApiError(e).message;
        setError(m);
        toast.error(m);
      })
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => load(applied, page, size), [applied, page, size, load]);
  const departments = data?.departments ?? [];
  const users = useMemo(
    () =>
      uniqueBy(data?.rows ?? [], (r) => r.userId ?? r.user)
        .map((r) => ({ v: String(r.userId ?? ""), l: r.user }))
        .filter((x) => x.v),
    [data],
  );
  const years = ["2026-2027", "2025-2026", "2024-2025"];
  const apply = () => {
    setApplied(draft);
    setPage(0);
  };
  const reset = () => {
    const f = defaults();
    setDraft(f);
    setApplied(f);
    setPage(0);
  };
  const quick = (kind: string) => {
    const now = new Date(),
      from = new Date(now),
      to = new Date(now);
    if (kind === "yesterday") {
      from.setDate(from.getDate() - 1);
      to.setDate(to.getDate() - 1);
    }
    if (kind === "week") from.setDate(from.getDate() - 6);
    if (kind === "month") from.setDate(1);
    if (kind === "30") from.setDate(from.getDate() - 29);
    const f = { ...draft, dateFrom: iso(from), dateTo: iso(to) };
    setDraft(f);
    setApplied(f);
    setPage(0);
  };
  const filterModule = (name: string) => {
    const map: Record<string, string> = {
      Admissions: "ADMISSION",
      Fees: "FEE",
      Attendance: "ATTENDANCE",
      Academic: "ACADEMIC",
      Students: "STUDENT_SECTION",
      Staff: "STAFF",
      Reports: "REPORT",
      Departments: "DEPARTMENT",
      College: "COLLEGE",
    };
    const f = { ...draft, module: map[name] ?? name.toUpperCase() };
    setDraft(f);
    setApplied(f);
    setPage(0);
  };
  const fetchExportRows = async () => {
    const first = await getBusinessActivityDashboard({ ...applied, page: 0, size: 100 });
    const remaining = await Promise.all(
      Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
        getBusinessActivityDashboard({ ...applied, page: index + 1, size: 100 }),
      ),
    );
    return [...first.rows, ...remaining.flatMap((result) => result.rows)];
  };
  const exportLocal = async (type: "csv" | "excel" | "pdf") => {
    try {
      const exportRows = await fetchExportRows();
      const collegeName = user?.collegeName || "All Jadhavar Colleges";
      if (type === "csv") {
        download(
          new Blob(
            [
              [
                "Date,User,Role,Department,Module,Action,Description,Status",
                ...exportRows.map((r) =>
                  [
                    fmt(r.createdAt),
                    r.user,
                    r.role,
                    r.department,
                    r.module,
                    r.action,
                    r.description,
                    r.status,
                  ]
                    .map(q)
                    .join(","),
                ),
              ].join("\n"),
            ],
            { type: "text/csv" },
          ),
          "business-activities.csv",
        );
        return;
      }
      if (type === "excel") {
        await exportCollegeExcel({
          filename: "business-activities.xlsx",
          sheetName: "Activity Register",
          title: "College Business Activity Report",
          collegeName,
          subtitle: "Official ERP audit and activity register",
          metadata: [
            ["Academic Year", applied.academicYear || "All"],
            ["Reporting Period", `${applied.dateFrom} to ${applied.dateTo}`],
            ["Module", applied.module || "All Modules"],
            ["Activities in Export", exportRows.length],
          ],
          headers: [
            "Date & Time",
            "User",
            "Role",
            "Department",
            "Module",
            "Action",
            "Description",
            "Status",
          ],
          rows: exportRows.map((r) => [
            fmt(r.createdAt),
            r.user,
            r.role,
            r.department,
            r.module,
            r.action,
            r.description,
            r.status,
          ]),
          widths: [24, 26, 20, 26, 18, 18, 48, 14],
          orientation: "landscape",
        });
        return;
      }
      const jsPDF = (await import("jspdf")).default,
        auto = (await import("jspdf-autotable")).default,
        doc = new jsPDF({ orientation: "landscape" });
      doc.setFontSize(16);
      doc.setFont("helvetica", "bold");
      doc.text(collegeName, 14, 14);
      doc.setFontSize(12);
      doc.text("College Business Activity Report", 14, 22);
      auto(doc, {
        startY: 28,
        head: [["Date", "User", "Role", "Department", "Module", "Action", "Description", "Status"]],
        body: exportRows.map((r) => [
          fmt(r.createdAt),
          r.user,
          r.role,
          r.department,
          r.module,
          r.action,
          r.description,
          r.status,
        ]),
        headStyles: { fillColor: [37, 99, 235] },
        didDrawPage: (hook) => {
          doc.setFontSize(8);
          doc.setFont("helvetica", "normal");
          doc.text(
            `${collegeName} · Business Activity Report`,
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
      doc.save("business-activities.pdf");
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  const timeline = useMemo(() => {
    const now = new Date(),
      today = iso(now),
      y = new Date(now);
    y.setDate(y.getDate() - 1);
    return (data?.timeline ?? []).filter(
      (r) =>
        timelineRange === "week" ||
        r.createdAt.slice(0, 10) === (timelineRange === "today" ? today : iso(y)),
    );
  }, [data, timelineRange]);
  return (
    <div className="page-container space-y-6 pb-12">
      <header className="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
        <div>
          <p className="text-xs font-bold uppercase tracking-[.18em] text-brand-600">
            Business monitoring
          </p>
          <h1 className="page-title mt-1">Audit Logs</h1>
          <p className="mt-1 text-sm text-slate-500">
            Track important business activities across the college.
          </p>
        </div>
        <div className="flex flex-wrap gap-2 print:hidden">
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
          <Button onClick={() => load(applied, page, size)}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </header>
      <Card className="sticky top-0 z-20 border-slate-200/80 bg-white/95 p-5 shadow-sm backdrop-blur print:hidden">
        <div className="mb-4 flex items-center gap-2 text-sm font-bold">
          <Filter className="h-4 w-4 text-brand-600" />
          Activity filters
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
          <Select
            label="Academic Year"
            value={draft.academicYear}
            set={(v) => setDraft((x) => ({ ...x, academicYear: v }))}
            options={years}
            all="All years"
          />
          <Select
            label="Department"
            value={draft.departmentId}
            set={(v) => setDraft((x) => ({ ...x, departmentId: v }))}
            options={departments
              .filter((x) => x.departmentId)
              .map((x) => ({ v: String(x.departmentId), l: x.department }))}
            all="All departments"
          />
          <Select
            label="Module"
            value={draft.module}
            set={(v) => setDraft((x) => ({ ...x, module: v }))}
            options={moduleValues}
            all="All modules"
          />
          <Select
            label="Role"
            value={draft.role}
            set={(v) => setDraft((x) => ({ ...x, role: v }))}
            options={[
              "PRINCIPAL",
              "HOD",
              "CLASS_TEACHER",
              "SUBJECT_TEACHER",
              "STUDENT_SECTION",
              "FEE_SECTION",
              "GENERAL_STAFF",
            ]}
            all="All roles"
          />
          <Select
            label="User"
            value={draft.userId}
            set={(v) => setDraft((x) => ({ ...x, userId: v }))}
            options={users}
            all="All users"
          />
          <Select
            label="Action Type"
            value={draft.action}
            set={(v) => setDraft((x) => ({ ...x, action: v }))}
            options={actions}
            all="All actions"
          />
          <Field
            label="Date From"
            type="date"
            value={draft.dateFrom}
            set={(v) => setDraft((x) => ({ ...x, dateFrom: v }))}
          />
          <Field
            label="Date To"
            type="date"
            value={draft.dateTo}
            set={(v) => setDraft((x) => ({ ...x, dateTo: v }))}
          />
          <div className="relative sm:col-span-2 lg:col-span-4 xl:col-span-8">
            <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
            <input
              value={draft.search}
              onChange={(e) => setDraft((x) => ({ ...x, search: e.target.value }))}
              placeholder="Search student, user, module, action, description or department"
              className="h-10 w-full rounded-lg border pl-9 pr-3 text-sm"
            />
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Button onClick={apply}>Apply Filters</Button>
          <Button variant="secondary" onClick={reset}>
            <RotateCcw className="mr-2 h-4 w-4" />
            Reset Filters
          </Button>
          {[
            ["today", "Today"],
            ["yesterday", "Yesterday"],
            ["week", "Last 7 Days"],
            ["30", "Last 30 Days"],
            ["month", "This Month"],
          ].map(([k, l]) => (
            <button
              key={k}
              onClick={() => quick(k)}
              className="rounded-full border px-3 py-2 text-xs font-bold text-slate-600 hover:border-brand-300 hover:text-brand-700"
            >
              {l}
            </button>
          ))}
        </div>
      </Card>
      {loading && !data ? (
        <Skeleton />
      ) : error && !data ? (
        <ErrorCard message={error} retry={() => load(applied, page, size)} />
      ) : data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
            <Kpi
              icon={CalendarDays}
              label="Today's Activities"
              value={data.summary.todayActivities}
              sub="Actions completed today"
              tone="blue"
            />
            <Kpi
              icon={CheckCircle2}
              label="Attendance Activities"
              value={data.summary.attendanceActivities}
              sub="Attendance-related work"
              tone="green"
            />
            <Kpi
              icon={GraduationCap}
              label="Admission Activities"
              value={data.summary.admissionActivities}
              sub="Admission workflow actions"
              tone="violet"
            />
            <Kpi
              icon={WalletCards}
              label="Fee Activities"
              value={data.summary.feeActivities}
              sub="Fee and payment actions"
              tone="cyan"
            />
            <Kpi
              icon={AlertTriangle}
              label="Critical Changes"
              value={data.summary.criticalChanges}
              sub="Important changes today"
              tone="red"
            />
            <Kpi
              icon={Clock3}
              label="Pending Approvals"
              value={data.summary.pendingApprovals}
              sub="Admissions awaiting action"
              tone="amber"
            />
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            {data.alerts.map((a) => (
              <button
                key={a.key}
                onClick={() => filterModule(a.module)}
                className="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 p-4 text-left text-amber-900 transition hover:-translate-y-0.5"
              >
                <span>
                  <b className="text-xl">{a.count}</b>
                  <span className="ml-2 text-sm font-semibold">{a.label}</span>
                </span>
                <ChevronRight className="h-4 w-4" />
              </button>
            ))}
          </div>
          <div className="grid gap-6 xl:grid-cols-3">
            <Card className="p-5 xl:col-span-2">
              <Title
                icon={TrendingUp}
                title="Daily activity trend"
                sub="Business activity volume across the selected period"
              />
              <div className="mt-4 h-72">
                <ResponsiveContainer>
                  <LineChart data={data.trend.map((x) => ({ ...x, label: x.date.slice(5) }))}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                    <YAxis />
                    <Tooltip />
                    <Line dataKey="activities" stroke="#2563eb" strokeWidth={3} dot={{ r: 3 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>
            <Card className="p-5">
              <Title
                icon={BarChart3}
                title="Action distribution"
                sub="Create, update, approve and report actions"
              />
              <div className="h-64">
                <ResponsiveContainer>
                  <PieChart>
                    <Pie
                      data={data.actionDistribution}
                      dataKey="value"
                      nameKey="label"
                      innerRadius={55}
                      outerRadius={85}
                    >
                      {["#2563eb", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"].map(
                        (c) => (
                          <Cell key={c} fill={c} />
                        ),
                      )}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </div>
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
            <Card className="p-5">
              <Title
                icon={Clock3}
                title="Activity timeline"
                sub="Latest meaningful business actions"
                action={
                  <select
                    value={timelineRange}
                    onChange={(e) => setTimelineRange(e.target.value)}
                    className="rounded-lg border px-3 py-2 text-xs"
                  >
                    <option value="today">Today</option>
                    <option value="yesterday">Yesterday</option>
                    <option value="week">This Week</option>
                  </select>
                }
              />
              <div className="mt-5 space-y-0">
                {timeline.map((r, i) => (
                  <button
                    key={r.id}
                    onClick={() => setSelected(r)}
                    className="relative flex w-full gap-4 pb-5 text-left"
                  >
                    <span className="relative z-10 grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-100 text-brand-700">
                      <Activity className="h-4 w-4" />
                    </span>
                    {i < timeline.length - 1 && (
                      <i className="absolute left-[17px] top-9 h-full w-px bg-slate-200" />
                    )}
                    <span>
                      <b className="text-sm">
                        {time(r.createdAt)} · {r.title}
                      </b>
                      <p className="text-sm text-slate-600">
                        {r.user} — {r.description}
                      </p>
                      <small className="text-slate-400">
                        {r.department} · {r.module}
                      </small>
                    </span>
                  </button>
                ))}
                {!timeline.length && <Empty />}
              </div>
            </Card>
            <Card className="self-start p-5 xl:sticky xl:top-48">
              <Title
                icon={TrendingUp}
                title="Smart Insights"
                sub="Automatically updated from activity data"
              />
              <div className="mt-4 space-y-3">
                {data.insights.map((x, i) => (
                  <div key={i} className="flex gap-3 rounded-xl border p-3">
                    <i
                      className={`mt-1 h-2.5 w-2.5 shrink-0 rounded-full ${i < 2 ? "bg-emerald-500" : i < 4 ? "bg-amber-500" : "bg-blue-500"}`}
                    />
                    <p className="text-sm font-medium text-slate-700">{x}</p>
                  </div>
                ))}
              </div>
            </Card>
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <Card className="overflow-hidden">
              <div className="border-b p-5">
                <Title
                  icon={FileCheck2}
                  title="Module analytics"
                  sub="Today's, weekly and monthly business activity"
                />
              </div>
              <SimpleTable
                heads={["Module", "Today", "This Week", "This Month", "Action"]}
                rows={data.modules.map((m) => [
                  m.module,
                  m.today,
                  m.week,
                  m.month,
                  <button
                    onClick={() => filterModule(m.module)}
                    className="font-bold text-brand-700"
                  >
                    View <ChevronRight className="inline h-4 w-4" />
                  </button>,
                ])}
              />
            </Card>
            <Card className="overflow-hidden">
              <div className="border-b p-5">
                <Title
                  icon={Users}
                  title="Department analytics"
                  sub="Activity levels and most active users"
                />
              </div>
              <SimpleTable
                heads={["Department", "Activities", "Last Activity", "Most Active User", "Status"]}
                rows={data.departments.map((d) => [
                  <button
                    onClick={() => {
                      if (d.departmentId) {
                        const f = { ...draft, departmentId: String(d.departmentId) };
                        setDraft(f);
                        setApplied(f);
                        setPage(0);
                      }
                    }}
                    className="font-bold text-brand-700"
                  >
                    {d.department}
                  </button>,
                  d.activities,
                  fmt(d.lastActivity),
                  d.mostActiveUser,
                  <Status value={d.status} />,
                ])}
              />
            </Card>
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <Card className="p-5">
              <Title
                icon={BarChart3}
                title="Module-wise activities"
                sub="Compare current-period activity volumes"
              />
              <div className="mt-4 h-72">
                <ResponsiveContainer>
                  <BarChart data={data.modules}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="module" tick={{ fontSize: 10 }} />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="month" fill="#2563eb" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
            <Card className="p-5">
              <Title
                icon={Users}
                title="Department activity distribution"
                sub="Most active departments appear first"
              />
              <div className="mt-5 space-y-4">
                {data.departments.slice(0, 10).map((d, i) => (
                  <Progress
                    key={d.department}
                    label={d.department}
                    value={d.activities}
                    max={Math.max(...data.departments.map((x) => x.activities), 1)}
                    rank={i + 1}
                  />
                ))}
              </div>
            </Card>
          </div>
          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b p-5">
              <Title
                icon={FileText}
                title="Business activity register"
                sub={`${data.totalElements} filtered activities`}
              />
              <div className="flex gap-2">
                <details className="relative">
                  <summary className="cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold">
                    Columns
                  </summary>
                  <div className="absolute right-0 z-20 mt-1 w-48 rounded-xl border bg-white p-3 shadow-xl">
                    {[
                      "date",
                      "user",
                      "role",
                      "department",
                      "module",
                      "action",
                      "description",
                      "status",
                    ].map((c) => (
                      <label key={c} className="flex gap-2 py-1 text-xs capitalize">
                        <input
                          type="checkbox"
                          checked={visible.has(c)}
                          onChange={() => {
                            const n = new Set(visible);
                            if (n.has(c)) n.delete(c);
                            else n.add(c);
                            setVisible(n);
                          }}
                        />
                        {c}
                      </label>
                    ))}
                  </div>
                </details>
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="rounded-lg border px-3 text-xs"
                >
                  <option>20</option>
                  <option>50</option>
                  <option>100</option>
                </select>
              </div>
            </div>
            <ActivityTable rows={data.rows} visible={visible} view={setSelected} />
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

function ActivityTable({
  rows,
  visible,
  view,
}: {
  rows: BusinessActivityRow[];
  visible: Set<string>;
  view: (r: BusinessActivityRow) => void;
}) {
  const cols = [
    "date",
    "user",
    "role",
    "department",
    "module",
    "action",
    "description",
    "status",
  ].filter((c) => visible.has(c));
  return (
    <div className="max-h-[620px] overflow-auto">
      <table className="w-full min-w-[1050px] text-left text-sm">
        <thead className="sticky top-0 z-10 bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            {cols.map((c) => (
              <th key={c} className="px-4 py-3 capitalize">
                {c === "date" ? "Date & Time" : c}
              </th>
            ))}
            <th className="px-4 py-3">Action</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className="border-t hover:bg-slate-50">
              {visible.has("date") && (
                <td className="px-4 py-3 whitespace-nowrap">{fmt(r.createdAt)}</td>
              )}
              {visible.has("user") && <td className="px-4 py-3 font-semibold">{r.user}</td>}
              {visible.has("role") && <td className="px-4 py-3">{r.role}</td>}
              {visible.has("department") && <td className="px-4 py-3">{r.department}</td>}
              {visible.has("module") && (
                <td className="px-4 py-3">
                  <span className="rounded-lg bg-blue-50 px-2 py-1 text-xs font-bold text-blue-700">
                    {r.module}
                  </span>
                </td>
              )}
              {visible.has("action") && <td className="px-4 py-3">{r.action}</td>}
              {visible.has("description") && (
                <td className="max-w-sm px-4 py-3 text-slate-600">{r.description}</td>
              )}
              {visible.has("status") && (
                <td className="px-4 py-3">
                  <Status value={r.status} />
                </td>
              )}
              <td className="px-4 py-3">
                <button
                  onClick={() => view(r)}
                  className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700"
                >
                  <Eye className="mr-1 inline h-3.5 w-3.5" />
                  View
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length && <Empty />}
    </div>
  );
}
function Drawer({ row, close }: { row: BusinessActivityRow; close: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/35" onMouseDown={close}>
      <aside
        onMouseDown={(e) => e.stopPropagation()}
        className="h-full w-full max-w-xl overflow-y-auto bg-white shadow-2xl"
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b bg-white/95 p-5 backdrop-blur">
          <div>
            <h2 className="font-bold">{row.title}</h2>
            <p className="text-xs text-slate-500">Business activity details</p>
          </div>
          <button onClick={close} className="rounded-lg p-2 hover:bg-slate-100">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="space-y-5 p-5">
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              ["Performed By", row.user],
              ["Role", row.role],
              ["Department", row.department],
              ["Module", row.module],
              ["Action", row.action],
              ["Date & Time", fmt(row.createdAt)],
              ["Affected Records", row.affectedRecords],
              ["Status", row.status],
            ].map(([l, v]) => (
              <Info key={l} label={l} value={v} />
            ))}
          </div>
          <Info label="Description" value={row.description} />
          {row.remarks && <Info label="Remarks" value={row.remarks} />}{" "}
          {row.previousValue && row.newValue && (
            <Card className="p-4">
              <p className="text-xs font-bold uppercase text-slate-500">Modified value</p>
              <div className="mt-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
                {row.previousValue}
              </div>
              <div className="my-2 text-center text-slate-400">↓</div>
              <div className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700">
                {row.newValue}
              </div>
            </Card>
          )}
        </div>
      </aside>
    </div>
  );
}
const tone: Record<string, string> = {
  blue: "bg-blue-50 text-blue-600",
  green: "bg-emerald-50 text-emerald-600",
  violet: "bg-violet-50 text-violet-600",
  cyan: "bg-cyan-50 text-cyan-600",
  red: "bg-rose-50 text-rose-600",
  amber: "bg-amber-50 text-amber-600",
};
function Kpi({
  icon: Icon,
  label,
  value,
  sub,
  tone: t,
}: {
  icon: typeof Activity;
  label: string;
  value: number;
  sub: string;
  tone: string;
}) {
  return (
    <Card className="p-5 transition hover:-translate-y-1 hover:shadow-md">
      <span className={`inline-flex rounded-xl p-2.5 ${tone[t]}`}>
        <Icon className="h-5 w-5" />
      </span>
      <b className="mt-4 block text-2xl">{value}</b>
      <p className="mt-1 text-xs font-bold uppercase text-slate-500">{label}</p>
      <p className="mt-2 text-[11px] text-slate-400">{sub}</p>
    </Card>
  );
}
function Title({
  icon: Icon,
  title,
  sub,
  action,
}: {
  icon: typeof Activity;
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
    <label className="text-xs font-semibold text-slate-600">
      {label}
      <select
        value={value}
        onChange={(e) => set(e.target.value)}
        className="mt-1.5 h-10 w-full rounded-lg border bg-white px-3 text-sm"
      >
        <option value="">{all}</option>
        {options.map((o) =>
          typeof o === "string" ? (
            <option key={o} value={o}>
              {pretty(o)}
            </option>
          ) : (
            <option key={o.v} value={o.v}>
              {o.l}
            </option>
          ),
        )}
      </select>
    </label>
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
function SimpleTable({ heads, rows }: { heads: string[]; rows: React.ReactNode[][] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[620px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            {heads.map((h) => (
              <th key={h} className="px-4 py-3">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className="border-t">
              {r.map((v, j) => (
                <td key={j} className="px-4 py-3">
                  {v}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length && <Empty />}
    </div>
  );
}
function Status({ value }: { value: string }) {
  const c =
    value === "SUCCESS" || value === "ACTIVE"
      ? "bg-emerald-100 text-emerald-700"
      : value === "PENDING"
        ? "bg-amber-100 text-amber-700"
        : value === "CANCELLED"
          ? "bg-slate-100 text-slate-600"
          : value === "HIGH" || value === "WARNING"
            ? "bg-rose-100 text-rose-700"
            : "bg-blue-100 text-blue-700";
  return (
    <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${c}`}>{pretty(value)}</span>
  );
}
function Progress({
  label,
  value,
  max,
  rank,
}: {
  label: string;
  value: number;
  max: number;
  rank: number;
}) {
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-sm">
        <b>
          {rank}. {label}
        </b>
        <span>{value}</span>
      </div>
      <div className="h-2.5 rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-brand-500"
          style={{ width: `${(value * 100) / max}%` }}
        />
      </div>
    </div>
  );
}
function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-1 text-sm font-semibold">{value || "—"}</p>
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
        {total} activities · Page {pages ? page + 1 : 0} of {pages}
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
    <div className="grid min-h-36 place-items-center p-8 text-center text-sm text-slate-400">
      <div>
        <Activity className="mx-auto mb-2 h-8 w-8 opacity-40" />
        <b className="text-slate-600">No activities found.</b>
        <p>Try changing filters.</p>
      </div>
    </div>
  );
}
function Skeleton() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-36 rounded-xl bg-slate-100" />
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
        <h2 className="mt-3 font-bold">Unable to load business activities</h2>
        <p className="my-3 text-sm text-slate-500">{message}</p>
        <Button onClick={retry}>Retry</Button>
      </div>
    </Card>
  );
}
function fmt(v: string) {
  return new Date(v).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
}
function time(v: string) {
  return new Date(v).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
}
function pretty(v: string) {
  return v
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
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
