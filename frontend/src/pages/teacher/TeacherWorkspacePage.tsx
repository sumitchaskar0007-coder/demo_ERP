import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Activity,
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Eye,
  GraduationCap,
  Hash,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/teacherWorkspace/api";

const input =
  "h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100";
const primary =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50";

export function TeacherWorkspacePage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const tab = params.get("tab") || "overview";
  const [data, setData] = useState<api.Workspace | null>(null),
    [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [search, setSearch] = useState(""),
    [page, setPage] = useState(0),
    [selected, setSelected] = useState<api.Student | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setData(
        await api.getWorkspace({
          search: search || undefined,
          page,
          size: tab === "identifiers" ? 100 : 20,
        }),
      );
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [search, page, tab]);
  useEffect(() => {
    const timer = setTimeout(() => void load(), search ? 250 : 0);
    return () => clearTimeout(timer);
  }, [load, search]);
  const refresh = async () => {
    setRefreshing(true);
    try {
      setData(
        await api.getWorkspace({
          search: search || undefined,
          page,
          size: tab === "identifiers" ? 100 : 20,
          refresh: Date.now(),
        }),
      );
      toast.success("Dashboard refreshed");
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setRefreshing(false);
    }
  };
  const setTab = (next: string) => setParams({ tab: next });
  if (loading && !data) return <Skeleton />;
  if (!data)
    return (
      <Empty
        title="Teacher workspace unavailable"
        text="Please refresh or contact your administrator."
      />
    );
  return (
    <div className="mx-auto max-w-[1500px] space-y-6 px-3 py-4 pb-10 sm:px-6 sm:py-5 lg:px-0 lg:py-0">
      <header className="relative overflow-hidden rounded-3xl border border-blue-400/30 bg-gradient-to-r from-brand-700 via-brand-600 to-sky-500 p-7 text-white shadow-[0_16px_38px_rgba(37,99,235,0.20)]">
        <div className="pointer-events-none absolute -right-16 -top-24 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
        <div className="pointer-events-none absolute bottom-0 left-1/3 h-24 w-80 rounded-full bg-cyan-300/10 blur-3xl" />
        <div className="relative flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <div className="mb-3 inline-flex rounded-full border border-white/20 bg-white/15 px-3 py-1 text-xs font-semibold text-blue-50 backdrop-blur-sm">
              {data.classTeacher && data.subjectTeacher
                ? "Class Teacher · Subject Teacher"
                : data.classTeacher
                  ? "Class Teacher"
                  : "Subject Teacher"}
            </div>
            <h1 className="text-3xl font-bold text-white">Welcome, {data.teacherName}</h1>
            <p className="mt-2 text-sm text-blue-50/90">
              {data.employeeCode} · Your teaching, class and attendance control center
            </p>
          </div>
          <button
            type="button"
            onClick={() => void refresh()}
            disabled={refreshing}
            className="rounded-xl border border-white/25 bg-white/15 px-4 py-2 text-sm font-semibold text-white shadow-sm backdrop-blur-sm transition hover:bg-white/25 disabled:cursor-wait disabled:opacity-70"
          >
            <RefreshCw className={`mr-2 inline h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
            {refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </header>
      {tab === "overview" && <Overview data={data} go={setTab} navigate={navigate} />}{" "}
      {tab === "class" && data.classTeacher && <MyClass data={data} go={setTab} />}{" "}
      {tab === "identifiers" && data.classTeacher && (
        <IdentifierManager students={data.students.content} reload={load} />
      )}{" "}
      {tab === "students" && (
        <Students
          data={data}
          search={search}
          setSearch={(v) => {
            setSearch(v);
            setPage(0);
          }}
          page={page}
          setPage={setPage}
          select={setSelected}
        />
      )}{" "}
      {tab === "attendance" && <Attendance data={data} select={setSelected} />}{" "}
      {tab === "attention" && <Attention rows={data.attention} select={setSelected} />}{" "}
      {tab === "coverage" && <Coverage rows={data.coverage} />}{" "}
      {tab === "workload" && <Workload data={data.workload} />}{" "}
      {tab === "schedule" && <Schedule rows={data.todaySchedule} navigate={navigate} />}{" "}
      {tab === "notices" && <Notices rows={data.notices} />}{" "}
      {tab === "notifications" && <Notifications data={data} reload={load} />}{" "}
      {selected && <StudentModal student={selected} close={() => setSelected(null)} />}
    </div>
  );
}

function Overview({
  data,
  go,
  navigate,
}: {
  data: api.Workspace;
  go: (x: string) => void;
  navigate: (x: string) => void;
}) {
  const k = data.kpis;
  const cards = [
    ["Today's Lectures", k.todayLectures, CalendarDays, "schedule"],
    ["Pending Attendance", k.pendingAttendance, Clock3, "schedule"],
    ["Completed Attendance", k.completedAttendance, CheckCircle2, "attendance"],
    ["My Subjects", k.subjects, BookOpen, "coverage"],
    ["My Divisions", k.divisions, Users, "class"],
    ["Class Strength", k.classStrength ?? "—", GraduationCap, "class"],
    ["Average Attendance", `${k.averageAttendance.toFixed(1)}%`, Activity, "attendance"],
  ] as const;
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
        {cards
          .filter(([label]) => label !== "Class Strength" || data.classTeacher)
          .map(([label, value, Icon, target]) => (
            <button
              onClick={() => go(target)}
              key={label}
              className="group rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-blue-300 hover:shadow-lg"
            >
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-50 text-blue-600">
                <Icon className="h-5 w-5" />
              </span>
              <p className="mt-4 text-2xl font-bold text-slate-900">{value}</p>
              <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
            </button>
          ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-[1.2fr_.8fr]">
        <Panel title="Today's schedule" subtitle="Your live lecture timeline">
          <Schedule rows={data.todaySchedule.slice(0, 4)} navigate={navigate} compact />
        </Panel>
        <Panel title="Pending tasks" subtitle="Items requiring action">
          <div className="space-y-2">
            <Task
              label="Attendance pending"
              value={k.pendingAttendance}
              onClick={() => go("schedule")}
            />
            <Task
              label="Students below 75%"
              value={data.attendance.below75}
              onClick={() => go("attention")}
            />
            <Task
              label="Unread notifications"
              value={data.notifications.filter((n) => n.unread).length}
              onClick={() => go("notifications")}
            />
            <Task
              label="Upcoming lectures"
              value={data.todaySchedule.filter((s) => s.state === "UPCOMING").length}
              onClick={() => go("schedule")}
            />
          </div>
        </Panel>
      </div>
      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title="Attendance snapshot" subtitle="Current class performance">
          <div className="h-64">
            <ResponsiveContainer>
              <AreaChart data={data.dailyTrend.slice(-14)}>
                <defs>
                  <linearGradient id="att" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0" stopColor="#2563eb" stopOpacity={0.35} />
                    <stop offset="1" stopColor="#2563eb" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                <YAxis domain={[0, 100]} />
                <Tooltip />
                <Area type="monotone" dataKey="percentage" stroke="#2563eb" fill="url(#att)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="Recent activities" subtitle="Latest updates in your scope">
          <div className="max-h-64 space-y-3 overflow-y-auto">
            {data.recentActivities.map((a, i) => (
              <div key={`${a.occurredAt}-${i}`} className="flex gap-3">
                <span className="mt-1.5 h-2 w-2 rounded-full bg-blue-500" />
                <div>
                  <p className="text-sm font-medium text-slate-700">{a.message}</p>
                  <p className="text-xs text-slate-400">
                    {new Date(a.occurredAt).toLocaleString()}
                  </p>
                </div>
              </div>
            ))}
            {!data.recentActivities.length && (
              <Empty title="No recent activity" text="Updates will appear here." />
            )}
          </div>
        </Panel>
      </div>
    </>
  );
}
function MyClass({ data, go }: { data: api.Workspace; go: (x: string) => void }) {
  const classDivisionIds = new Set(data.classes.map((classItem) => classItem.divisionId));
  const classInsights = data.divisionInsights.filter((insight) =>
    classDivisionIds.has(insight.divisionId),
  );

  return (
    <>
      <div className="grid gap-5 md:grid-cols-2">
        {data.classes.map((c) => (
          <Panel
            key={c.divisionId}
            title={c.className}
            subtitle={`${c.department} · ${c.academicYear}`}
          >
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Metric label="Students" value={c.totalStudents} />
              <Metric label="Male" value={c.maleStudents} />
              <Metric label="Female" value={c.femaleStudents} />
              <Metric label="Attendance" value={`${c.averageAttendance.toFixed(1)}%`} />
            </div>
          </Panel>
        ))}
      </div>
      <Panel title="My class insights" subtitle="Strength and attendance for your assigned class">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {classInsights.map((d) => (
            <div key={d.divisionId} className="rounded-2xl border border-slate-200 p-5">
              <p className="font-bold text-slate-800">{d.division}</p>
              <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                <Metric label="Students" value={d.totalStudents} />
                <Metric label="Average" value={`${d.averageAttendance}%`} />
                <Metric label="Below 75%" value={d.lowAttendanceStudents} />
              </div>
              <div className="mt-4 h-24">
                <ResponsiveContainer>
                  <AreaChart data={d.trend}>
                    <Area dataKey="percentage" stroke="#7c3aed" fill="#ede9fe" />
                    <XAxis dataKey="label" hide />
                    <YAxis domain={[0, 100]} hide />
                    <Tooltip />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          ))}
        </div>
        <button className={`${primary} mt-5`} onClick={() => go("students")}>
          Open student directory <ArrowRight className="h-4 w-4" />
        </button>
      </Panel>
    </>
  );
}
function Students({
  data,
  search,
  setSearch,
  page,
  setPage,
  select,
}: {
  data: api.Workspace;
  search: string;
  setSearch: (x: string) => void;
  page: number;
  setPage: (x: number) => void;
  select: (x: api.Student) => void;
}) {
  return (
    <Panel
      title="Student directory"
      subtitle={`${data.students.totalElements} students in your assigned divisions`}
    >
      <div className="mb-5">
        <label className="relative block">
          <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
          <input
            className={`${input} w-full pl-9`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Name, admission number, PRN or roll number"
          />
        </label>
      </div>
      <StudentTable rows={data.students.content} select={select} />
      <div className="mt-4 flex items-center justify-between text-sm">
        <span className="text-slate-500">
          Page {data.students.page + 1} of {Math.max(1, data.students.totalPages)}
        </span>
        <div className="flex gap-2">
          <button
            className="rounded-lg border px-3 py-2 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </button>
          <button
            className="rounded-lg border px-3 py-2 disabled:opacity-40"
            disabled={page + 1 >= data.students.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </Panel>
  );
}

function IdentifierManager({
  students,
  reload,
}: {
  students: api.Student[];
  reload: () => Promise<void>;
}) {
  const [query, setQuery] = useState("");
  const [drafts, setDrafts] = useState<Record<number, { prn: string; rollNumber: string }>>({});
  const [saving, setSaving] = useState<number | null>(null);
  useEffect(() => {
    setDrafts(
      Object.fromEntries(
        students.map((student) => [
          student.id,
          { prn: student.prn ?? "", rollNumber: student.rollNumber ?? "" },
        ]),
      ),
    );
  }, [students]);
  const complete = students.filter((student) => student.prn && student.rollNumber).length;
  const visible = students.filter((student) => {
    const value =
      `${student.name} ${student.admissionNumber} ${student.prn ?? ""} ${student.rollNumber ?? ""}`.toLowerCase();
    return value.includes(query.trim().toLowerCase());
  });
  const save = async (student: api.Student) => {
    const draft = drafts[student.id];
    if (!draft?.prn.trim() || !draft.rollNumber.trim()) {
      toast.error("Enter both university PRN and class roll number");
      return;
    }
    setSaving(student.id);
    try {
      await api.saveStudentIdentifiers(student.id, draft.prn.trim(), draft.rollNumber.trim());
      toast.success(`Identifiers saved for ${student.name}`);
      await reload();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setSaving(null);
    }
  };
  return (
    <div className="space-y-5">
      <section className="overflow-hidden rounded-3xl border border-blue-200 bg-white shadow-sm">
        <div className="relative bg-gradient-to-r from-slate-950 via-blue-950 to-blue-700 px-6 py-7 text-white">
          <div className="absolute -right-12 -top-16 h-52 w-52 rounded-full bg-cyan-300/10 blur-2xl" />
          <div className="relative flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
            <div>
              <span className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-semibold">
                <ShieldCheck className="h-3.5 w-3.5" /> Class teacher access only
              </span>
              <h2 className="mt-4 text-2xl font-bold">Student PRN & roll numbers</h2>
              <p className="mt-2 max-w-2xl text-sm text-blue-100">
                Enter the university-issued PRN and your class roll number. Admission numbers stay
                unchanged and are shown only as a reliable student reference.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-2xl border border-white/15 bg-white/10 px-5 py-3 backdrop-blur">
                <p className="text-2xl font-bold">{complete}</p>
                <p className="text-xs text-blue-100">Complete</p>
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-5 py-3 backdrop-blur">
                <p className="text-2xl font-bold">{students.length - complete}</p>
                <p className="text-xs text-blue-100">Pending</p>
              </div>
            </div>
          </div>
        </div>
        <div className="border-b border-slate-200 p-4">
          <label className="relative block">
            <Search className="absolute left-4 top-3.5 h-4 w-4 text-slate-400" />
            <input
              className={`${input} w-full pl-11`}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by student, admission number, PRN or roll number"
            />
          </label>
        </div>
        <div className="divide-y divide-slate-100">
          {visible.map((student) => {
            const draft = drafts[student.id] ?? { prn: "", rollNumber: "" };
            const saved = Boolean(student.prn && student.rollNumber);
            const changed =
              draft.prn.trim() !== (student.prn ?? "") ||
              draft.rollNumber.trim() !== (student.rollNumber ?? "");
            return (
              <div
                key={student.id}
                className="grid gap-4 p-5 transition hover:bg-blue-50/40 xl:grid-cols-[minmax(230px,1.2fr)_minmax(180px,.8fr)_minmax(180px,.8fr)_120px] xl:items-end"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-blue-100 font-bold text-blue-700">
                    {student.name.charAt(0).toUpperCase()}
                  </span>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate font-bold text-slate-900">{student.name}</p>
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${saved ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}
                      >
                        {saved ? "ASSIGNED" : "PENDING"}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      Admission {student.admissionNumber} · {student.division}
                    </p>
                  </div>
                </div>
                <label>
                  <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold text-slate-600">
                    <ShieldCheck className="h-3.5 w-3.5 text-blue-600" /> University PRN
                  </span>
                  <input
                    className={`${input} w-full font-semibold uppercase`}
                    maxLength={60}
                    value={draft.prn}
                    onChange={(event) =>
                      setDrafts((current) => ({
                        ...current,
                        [student.id]: { ...draft, prn: event.target.value },
                      }))
                    }
                    placeholder="Enter PRN"
                  />
                </label>
                <label>
                  <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold text-slate-600">
                    <Hash className="h-3.5 w-3.5 text-violet-600" /> Class roll number
                  </span>
                  <input
                    className={`${input} w-full font-semibold uppercase`}
                    maxLength={60}
                    value={draft.rollNumber}
                    onChange={(event) =>
                      setDrafts((current) => ({
                        ...current,
                        [student.id]: { ...draft, rollNumber: event.target.value },
                      }))
                    }
                    placeholder="Enter roll no."
                  />
                </label>
                <button
                  type="button"
                  className={`${primary} h-11`}
                  disabled={
                    saving === student.id ||
                    !draft.prn.trim() ||
                    !draft.rollNumber.trim() ||
                    (!changed && saved)
                  }
                  onClick={() => void save(student)}
                >
                  <Save className="h-4 w-4" />
                  {saving === student.id ? "Saving..." : saved ? "Update" : "Save"}
                </button>
              </div>
            );
          })}
          {!visible.length && (
            <Empty title="No students found" text="Try a different student or identifier search." />
          )}
        </div>
      </section>
    </div>
  );
}
function Attendance({ data, select }: { data: api.Workspace; select: (x: api.Student) => void }) {
  const m = data.attendance;
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Today's Attendance" value={`${m.today}%`} />
        <MetricCard label="Weekly Attendance" value={`${m.weekly}%`} />
        <MetricCard label="Monthly Attendance" value={`${m.monthly}%`} />
        <MetricCard label="Overall Attendance" value={`${m.overall}%`} />
      </div>
      <div className="grid gap-5 xl:grid-cols-3">
        <Trend title="Daily trend" rows={data.dailyTrend} />
        <Trend title="Weekly trend" rows={data.weeklyTrend} />
        <Trend title="Monthly trend" rows={data.monthlyTrend} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Above 90%" value={m.above90} tone="emerald" />
        <MetricCard label="Between 75–90%" value={m.between75And90} tone="blue" />
        <MetricCard label="Below 75%" value={m.below75} tone="amber" />
        <MetricCard label="Below 60%" value={m.below60} tone="rose" />
      </div>
      <Panel
        title="Student-level drill-down"
        subtitle="Students currently requiring attendance attention"
      >
        <StudentTable rows={data.attention} select={select} />
      </Panel>
    </>
  );
}
function Attention({ rows, select }: { rows: api.Student[]; select: (x: api.Student) => void }) {
  return (
    <Panel
      title="Students requiring attention"
      subtitle="Automatically identified from attendance performance and recent marking activity"
    >
      <StudentTable rows={rows} select={select} issue />
    </Panel>
  );
}
function Coverage({ rows }: { rows: api.Coverage[] }) {
  return (
    <Panel
      title="Subject coverage tracking"
      subtitle="Planned lectures use subject credits and the current academic cycle"
    >
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {rows.map((r, i) => (
          <div key={`${r.subjectId}-${i}`} className="rounded-2xl border border-slate-200 p-5">
            <div className="flex justify-between gap-3">
              <div>
                <p className="font-bold text-slate-800">{r.subject}</p>
                <p className="text-sm text-slate-500">{r.division}</p>
              </div>
              <Badge value={r.status} />
            </div>
            <div className="mt-5 flex justify-between text-sm">
              <span>{r.completedLectures} completed</span>
              <span>{r.remainingLectures} remaining</span>
            </div>
            <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-blue-600"
                style={{ width: `${r.completionPercentage}%` }}
              />
            </div>
            <p className="mt-2 text-right text-xs font-semibold text-slate-500">
              {r.completionPercentage}% of {r.plannedLectures}
            </p>
          </div>
        ))}
        {!rows.length && (
          <Empty
            title="No subject coverage yet"
            text="Coverage appears after timetable lectures are assigned."
          />
        )}
      </div>
    </Panel>
  );
}
function Workload({ data }: { data: api.Workload }) {
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <MetricCard label="Total Subjects" value={data.totalSubjects} />
        <MetricCard label="Total Divisions" value={data.totalDivisions} />
        <MetricCard label="Weekly Lectures" value={data.weeklyLectures} />
        <MetricCard label="Today's Lectures" value={data.todayLectures} />
        <MetricCard label="Pending Attendance" value={data.pendingAttendanceSessions} />
      </div>
      <div className="grid gap-5 lg:grid-cols-2">
        <BarPanel title="Weekly workload" rows={data.weekly} />
        <Panel title="Distribution by subject" subtitle="Share of your scheduled lectures">
          <div className="h-72">
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={data.bySubject}
                  dataKey="value"
                  nameKey="label"
                  innerRadius={55}
                  outerRadius={90}
                  paddingAngle={3}
                >
                  {data.bySubject.map((_, i) => (
                    <Cell
                      key={i}
                      fill={["#2563eb", "#7c3aed", "#0891b2", "#059669", "#d97706"][i % 5]}
                    />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>
    </>
  );
}
function Schedule({
  rows,
  navigate,
  compact = false,
}: {
  rows: api.Schedule[];
  navigate: (x: string) => void;
  compact?: boolean;
}) {
  return (
    <div className="space-y-3">
      {rows.map((r) => (
        <div
          key={r.timetableEntryId}
          className={`flex flex-col gap-3 rounded-2xl border p-4 sm:flex-row sm:items-center ${r.state === "CURRENT" ? "border-blue-400 bg-blue-50" : r.state === "COMPLETED" ? "border-slate-200 bg-slate-50" : "border-slate-200"}`}
        >
          <div className="w-36 text-sm font-bold text-slate-700">{r.time}</div>
          <div className="flex-1">
            <p className="font-bold text-slate-800">{r.subject}</p>
            <p className="text-sm text-slate-500">
              {r.division} · {r.lectureType}
            </p>
          </div>
          <Badge value={r.state} />
          {r.canTakeAttendance && (
            <button className={primary} onClick={() => navigate(ROUTES.teacherAttendance)}>
              Take Attendance
            </button>
          )}
        </div>
      ))}
      {!rows.length && (
        <Empty
          title="No lectures today"
          text={compact ? "Your next scheduled day will appear here." : "Enjoy the open schedule."}
        />
      )}
    </div>
  );
}
function Notices({ rows }: { rows: api.Notice[] }) {
  const [open, setOpen] = useState<number | null>(null);
  return (
    <Panel title="Notices" subtitle="College and department announcements">
      <div className="space-y-3">
        {rows.map((n) => (
          <button
            key={n.id}
            onClick={() => setOpen(open === n.id ? null : n.id)}
            className={`w-full rounded-2xl border p-5 text-left ${n.unread ? "border-blue-300 bg-blue-50/50" : "border-slate-200"}`}
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-bold text-slate-800">
                  {n.title}
                  {n.unread && (
                    <span className="ml-2 rounded-full bg-blue-600 px-2 py-0.5 text-[10px] text-white">
                      NEW
                    </span>
                  )}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {n.createdBy} · {new Date(n.date).toLocaleString()}
                </p>
              </div>
              <Badge value={n.priority} />
            </div>
            {open === n.id && (
              <p className="mt-4 border-t pt-4 text-sm leading-6 text-slate-600">{n.message}</p>
            )}
          </button>
        ))}
        {!rows.length && <Empty title="No notices" text="New notices will appear here." />}
      </div>
    </Panel>
  );
}
function Notifications({ data, reload }: { data: api.Workspace; reload: () => Promise<void> }) {
  const mark = async (id: number) => {
    await api.readNotification(id);
    await reload();
  };
  return (
    <Panel
      title="HOD notifications"
      subtitle="Automatic updates about assignments, students, rolls and timetables"
    >
      <div className="mb-4 flex justify-end">
        <button
          className={primary}
          disabled={!data.notifications.some((n) => n.unread)}
          onClick={() => void api.readAllNotifications().then(reload)}
        >
          Mark all read
        </button>
      </div>
      <div className="space-y-3">
        {data.notifications.map((n) => (
          <button
            onClick={() => n.unread && void mark(n.id)}
            key={n.id}
            className={`flex w-full gap-4 rounded-2xl border p-4 text-left ${n.unread ? "border-blue-300 bg-blue-50" : "border-slate-200"}`}
          >
            <span
              className={`mt-1 h-3 w-3 rounded-full ${n.unread ? "bg-blue-600" : "bg-slate-300"}`}
            />
            <div>
              <p className="font-semibold text-slate-800">{n.message}</p>
              <p className="mt-1 text-xs text-slate-500">
                {n.type.replaceAll("_", " ")} · {new Date(n.createdAt).toLocaleString()}
              </p>
            </div>
          </button>
        ))}
        {!data.notifications.length && (
          <Empty title="No notifications" text="HOD updates will appear automatically." />
        )}
      </div>
    </Panel>
  );
}
function StudentTable({
  rows,
  select,
  issue = false,
}: {
  rows: api.Student[];
  select: (x: api.Student) => void;
  issue?: boolean;
}) {
  return (
    <div>
      <div className="space-y-3 md:hidden">
        {rows.map((s) => (
          <article
            key={s.id}
            className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
          >
            <div className="flex min-w-0 items-start gap-3 p-4">
              <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-blue-50 text-sm font-black uppercase text-blue-700 ring-1 ring-blue-100">
                {s.name
                  .split(/\s+/)
                  .slice(0, 2)
                  .map((part) => part[0])
                  .join("")}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <h3 className="truncate font-bold text-slate-900">{s.name}</h3>
                    <p className="mt-0.5 truncate text-xs text-slate-500">{s.email}</p>
                  </div>
                  <span className="shrink-0">
                    <Badge value={s.status} />
                  </span>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-3 border-y border-slate-100 bg-slate-50/70">
              <StudentMobileMetric label="Roll" value={s.rollNumber || "—"} />
              <StudentMobileMetric label="Division" value={s.division} />
              <StudentMobileMetric
                label="Attendance"
                value={`${s.attendancePercentage}%`}
                tone={
                  s.attendancePercentage < 60
                    ? "text-rose-600"
                    : s.attendancePercentage < 75
                      ? "text-amber-600"
                      : "text-emerald-600"
                }
              />
            </div>

            <div className="p-4">
              <p className="mb-3 break-all text-xs font-medium text-slate-500">
                PRN: <span className="text-slate-700">{s.prn || "Not assigned"}</span>
              </p>
              {issue && s.attentionIssue && (
                <p className="mb-3 rounded-xl bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700">
                  {s.attentionIssue}
                </p>
              )}
              <button
                type="button"
                onClick={() => select(s)}
                className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm transition active:scale-[0.98]"
                aria-label={`View profile for ${s.name}`}
              >
                <Eye className="h-4 w-4" />
                View profile
              </button>
            </div>
          </article>
        ))}
      </div>

      <div className="responsive-table hidden md:block">
        <table>
          <thead>
            <tr>
              <th className="p-3">Roll</th>
              <th>Student</th>
              <th>PRN</th>
              <th>Division</th>
              <th>Attendance</th>
              {issue && <th>Issue</th>}
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((s) => (
              <tr className="border-t" key={s.id}>
                <td className="p-3 font-semibold">{s.rollNumber || "—"}</td>
                <td>
                  <p className="font-semibold text-slate-800">{s.name}</p>
                  <p className="text-xs text-slate-400">{s.email}</p>
                </td>
                <td>{s.prn}</td>
                <td>{s.division}</td>
                <td>
                  <span
                    className={`font-bold ${s.attendancePercentage < 60 ? "text-rose-600" : s.attendancePercentage < 75 ? "text-amber-600" : "text-emerald-600"}`}
                  >
                    {s.attendancePercentage}%
                  </span>
                </td>
                {issue && <td className="text-rose-600">{s.attentionIssue}</td>}
                <td>
                  <Badge value={s.status} />
                </td>
                <td>
                  <button
                    onClick={() => select(s)}
                    className="font-semibold text-blue-600 hover:underline"
                  >
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!rows.length && (
        <Empty title="No students found" text="There are no students matching this view." />
      )}
    </div>
  );
}

function StudentMobileMetric({
  label,
  value,
  tone = "text-slate-800",
}: {
  label: string;
  value: string | number;
  tone?: string;
}) {
  return (
    <div className="min-w-0 border-r border-slate-100 px-2 py-3 text-center last:border-r-0">
      <p className={`truncate text-sm font-black ${tone}`}>{value}</p>
      <p className="mt-1 text-[10px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
    </div>
  );
}
function StudentModal({ student, close }: { student: api.Student; close: () => void }) {
  const [history, setHistory] = useState<api.AttendanceHistory[] | null>(null);
  const [historyPage, setHistoryPage] = useState(0);
  const historyPageSize = 5;
  useEffect(() => {
    setHistory(null);
    setHistoryPage(0);
    api
      .getStudentAttendance(student.id)
      .then(setHistory)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [student.id]);
  const historyTotalPages = Math.max(1, Math.ceil((history?.length ?? 0) / historyPageSize));
  const visibleHistory =
    history?.slice(historyPage * historyPageSize, (historyPage + 1) * historyPageSize) ?? [];
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 p-4" onClick={close}>
      <div
        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xl font-bold text-slate-900">{student.name}</p>
            <p className="text-sm text-slate-500">
              {student.prn} · {student.division}
            </p>
          </div>
          <button onClick={close} className="rounded-lg border px-3 py-1">
            Close
          </button>
        </div>
        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Metric label="Roll number" value={student.rollNumber || "—"} />
          <Metric label="Attendance" value={`${student.attendancePercentage}%`} />
          <Metric label="Gender" value={student.gender} />
          <Metric label="Status" value={student.status} />
        </div>
        <div className="mt-5">
          <p className="mb-3 text-xs font-bold uppercase tracking-wide text-slate-400">
            Attendance history
          </p>
          {history === null ? (
            <p className="text-sm text-slate-400">Loading history…</p>
          ) : history.length ? (
            <div className="space-y-2">
              {visibleHistory.map((r, i) => (
                <div
                  key={`${r.date}-${r.time}-${historyPage * historyPageSize + i}`}
                  className="grid gap-2 rounded-xl bg-slate-50 p-3 text-sm sm:grid-cols-[100px_130px_1fr_auto]"
                >
                  <span>{r.date}</span>
                  <span className="text-slate-500">{r.time}</span>
                  <span>
                    {r.subject} · {r.division}
                  </span>
                  <Badge value={r.status} />
                  {r.remarks && (
                    <span className="text-xs text-slate-500 sm:col-span-4">{r.remarks}</span>
                  )}
                </div>
              ))}
              <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4 text-sm">
                <span className="text-slate-500">
                  Page {historyPage + 1} of {historyTotalPages} · {history.length} records
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-lg border border-slate-200 px-3 py-2 font-medium text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
                    disabled={historyPage === 0}
                    onClick={() => setHistoryPage((current) => current - 1)}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="rounded-lg border border-slate-200 px-3 py-2 font-medium text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
                    disabled={historyPage + 1 >= historyTotalPages}
                    onClick={() => setHistoryPage((current) => current + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <Empty
              title="No attendance history"
              text="No submitted attendance records are available in your assigned scope."
            />
          )}
        </div>
      </div>
    </div>
  );
}
function Trend({ title, rows }: { title: string; rows: { label: string; percentage: number }[] }) {
  return (
    <Panel title={title} subtitle="Submitted attendance">
      <div className="h-64">
        <ResponsiveContainer>
          <AreaChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" tick={{ fontSize: 10 }} />
            <YAxis domain={[0, 100]} />
            <Tooltip />
            <Area type="monotone" dataKey="percentage" stroke="#2563eb" fill="#dbeafe" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Panel>
  );
}
function BarPanel({ title, rows }: { title: string; rows: { label: string; value: number }[] }) {
  return (
    <Panel title={title} subtitle="Scheduled lectures">
      <div className="h-72">
        <ResponsiveContainer>
          <BarChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="value" fill="#2563eb" radius={[8, 8, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Panel>
  );
}
function Panel({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <div className="mb-5">
        <h2 className="text-lg font-bold text-slate-900">{title}</h2>
        <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}
function MetricCard({
  label,
  value,
  tone = "blue",
}: {
  label: string;
  value: string | number;
  tone?: string;
}) {
  const colors: { [k: string]: string } = {
    blue: "bg-blue-50 text-blue-700",
    emerald: "bg-emerald-50 text-emerald-700",
    amber: "bg-amber-50 text-amber-700",
    rose: "bg-rose-50 text-rose-700",
  };
  return (
    <div className={`rounded-2xl p-5 ${colors[tone]}`}>
      <p className="text-3xl font-bold">{value}</p>
      <p className="mt-1 text-sm font-semibold opacity-80">{label}</p>
    </div>
  );
}
function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-xl font-bold text-slate-800">{value}</p>
      <p className="text-xs text-slate-500">{label}</p>
    </div>
  );
}
function Task({ label, value, onClick }: { label: string; value: number; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center justify-between rounded-xl border border-slate-200 p-3 text-sm font-semibold text-slate-700 hover:border-blue-300 hover:bg-blue-50"
    >
      <span>{label}</span>
      <span className="rounded-full bg-slate-100 px-2.5 py-1">{value}</span>
    </button>
  );
}
function Badge({ value }: { value: string }) {
  const good = ["ACTIVE", "COMPLETED", "ON_TRACK", "CURRENT"].includes(value),
    bad = ["BEHIND_SCHEDULE", "URGENT"].includes(value),
    warn = ["UPCOMING", "HIGH"].includes(value);
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${good ? "bg-emerald-50 text-emerald-700" : bad ? "bg-rose-50 text-rose-700" : warn ? "bg-amber-50 text-amber-700" : "bg-slate-100 text-slate-600"}`}
    >
      {value.replaceAll("_", " ")}
    </span>
  );
}
function Empty({ title, text }: { title: string; text: string }) {
  return (
    <div className="py-10 text-center">
      <p className="font-semibold text-slate-700">{title}</p>
      <p className="mt-1 text-sm text-slate-400">{text}</p>
    </div>
  );
}
function Skeleton() {
  return (
    <div className="space-y-5 animate-pulse">
      <div className="h-44 rounded-3xl bg-slate-200" />
      <div className="h-16 rounded-2xl bg-slate-100" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
        {[1, 2, 3, 4].map((x) => (
          <div key={x} className="h-32 rounded-2xl bg-slate-100" />
        ))}
      </div>
    </div>
  );
}
