import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import {
  Activity,
  BookOpen,
  CalendarCheck,
  Check,
  ChevronRight,
  ClipboardCheck,
  GraduationCap,
  LayoutDashboard,
  RefreshCw,
  Search,
  Users,
} from "lucide-react";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/hod/api";

const tabs = [
  ["overview", "Overview", LayoutDashboard],
  ["students", "Student allocation", Users],
  ["rolls", "Roll numbers", GraduationCap],
  ["class-teachers", "Class teachers", Users],
  ["workload", "Workload", Activity],
  ["attendance", "Attendance", ClipboardCheck],
] as const;
const actionClass =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50";
const secondaryClass =
  "inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50";
const inputClass =
  "h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none transition focus:border-violet-400 focus:ring-4 focus:ring-violet-100";

export function HodWorkspacePage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const requestedTab = params.get("tab") || "overview";
  const tab = tabs.some(([key]) => key === requestedTab) ? requestedTab : "overview";
  const [data, setData] = useState<api.Workspace | null>(null),
    [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false);
  const [search, setSearch] = useState(""),
    [allocationDepartment, setAllocationDepartment] = useState(""),
    [courseYearFilter, setCourseYearFilter] = useState("");
  const [selected, setSelected] = useState<number[]>([]),
    [target, setTarget] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setData(
        await api.workspace({
          search: search || undefined,
          courseYearId: courseYearFilter || undefined,
          allocationStatus: "UNALLOCATED",
          size: 100,
        }),
      );
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [search, courseYearFilter]);
  useEffect(() => {
    const timer = setTimeout(() => void load(), search ? 250 : 0);
    return () => clearTimeout(timer);
  }, [load, search]);
  const run = async (task: () => Promise<unknown>, message: string) => {
    setBusy(true);
    try {
      await task();
      toast.success(message);
      setSelected([]);
      setTarget("");
      await load();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setBusy(false);
    }
  };
  if (loading && !data)
    return (
      <div className="flex min-h-[55vh] items-center justify-center text-slate-500">
        <RefreshCw className="mr-2 h-5 w-5 animate-spin" />
        Loading HOD workspace…
      </div>
    );
  if (!data)
    return (
      <Empty title="Workspace unavailable" text="The department workspace could not be loaded." />
    );
  return (
    <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
      <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-slate-950 via-violet-950 to-indigo-900 p-6 text-white shadow-xl md:p-8">
        <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">
          <div>
            <div className="mb-3 inline-flex rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-violet-100">
              Department control center
            </div>
            <h1 className="text-3xl font-bold tracking-tight">{data.department} HOD Workspace</h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Allocate students and faculty, review academic delivery, and monitor department
              performance.
            </p>
          </div>
          <button
            className="rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold hover:bg-white/20"
            onClick={() => void load()}
          >
            <RefreshCw className="mr-2 inline h-4 w-4" />
            Refresh
          </button>
        </div>
      </div>
      <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
        <div className="flex min-w-max gap-1">
          {tabs.map(([key, label, Icon]) => (
            <button
              key={key}
              onClick={() => setParams({ tab: key })}
              className={`flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-semibold ${tab === key ? "bg-violet-600 text-white shadow" : "text-slate-600 hover:bg-slate-100"}`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>
      </div>
      {tab === "overview" && <Overview data={data} setTab={(next) => setParams({ tab: next })} />}
      {tab === "students" && (
        <StudentsPanel
          data={data}
          search={search}
          setSearch={setSearch}
          allocationDepartment={allocationDepartment}
          setAllocationDepartment={setAllocationDepartment}
          courseYearFilter={courseYearFilter}
          setCourseYearFilter={setCourseYearFilter}
          selected={selected}
          setSelected={setSelected}
          target={target}
          setTarget={setTarget}
          busy={busy}
          run={run}
        />
      )}
      {tab === "rolls" && <RollPanel divisions={data.divisions} busy={busy} run={run} />}
      {tab === "class-teachers" && <ClassTeachersPanel data={data} busy={busy} run={run} />}
      {tab === "workload" && <WorkloadPanel teachers={data.teachers} />}
      {tab === "attendance" && (
        <div className="grid gap-5 lg:grid-cols-2">
          <Panel
            title="Attendance analytics"
            subtitle="Open the department report for daily, student and subject trends."
          >
            <div className="rounded-2xl bg-emerald-50 p-6">
              <p className="text-4xl font-bold text-emerald-700">
                {data.summary.averageAttendance.toFixed(1)}%
              </p>
              <p className="mt-1 text-sm text-emerald-800">
                Department average across submitted attendance
              </p>
            </div>
            <button
              className={`${actionClass} mt-5`}
              onClick={() => navigate(ROUTES.attendanceReport)}
            >
              Open attendance reports <ChevronRight className="h-4 w-4" />
            </button>
          </Panel>
          <Panel
            title="Attendance controls"
            subtitle="Attendance records remain attached to their original class session when a student changes division."
          >
            <div className="space-y-3 text-sm text-slate-600">
              <Info label="Transfer-safe history" value="Enabled" />
              <Info label="Scope" value={data.department} />
              <Info label="Reporting" value="Daily · Monthly · Student · Subject" />
            </div>
          </Panel>
        </div>
      )}
    </div>
  );
}

function Overview({ data, setTab }: { data: api.Workspace; setTab: (x: string) => void }) {
  const cards = [
    ["Students", data.summary.totalStudents, GraduationCap, "bg-blue-50 text-blue-700"],
    ["Teachers", data.summary.totalTeachers, Users, "bg-violet-50 text-violet-700"],
    ["Divisions", data.summary.totalDivisions, LayoutDashboard, "bg-amber-50 text-amber-700"],
    ["Subjects", data.summary.totalSubjects, BookOpen, "bg-emerald-50 text-emerald-700"],
    ["Classes today", data.summary.classesRunningToday, CalendarCheck, "bg-cyan-50 text-cyan-700"],
    ["Pending tasks", data.summary.pendingTasks, ClipboardCheck, "bg-rose-50 text-rose-700"],
  ] as const;
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
        {cards.map(([label, value, Icon, color]) => (
          <div key={label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className={`mb-4 flex h-10 w-10 items-center justify-center rounded-xl ${color}`}>
              <Icon className="h-5 w-5" />
            </div>
            <p className="text-2xl font-bold text-slate-900">{value}</p>
            <p className="text-sm text-slate-500">{label}</p>
          </div>
        ))}
      </div>
      <div className="grid gap-5 lg:grid-cols-[1.3fr_.7fr]">
        <Panel title="Division capacity" subtitle="Live allocation against approved capacity">
          <div className="space-y-4">
            {data.divisions.map((d) => (
              <div key={d.id}>
                <div className="mb-1.5 flex justify-between text-sm">
                  <span className="font-semibold text-slate-700">
                    {d.courseYear} · {d.name}
                  </span>
                  <span className="text-slate-500">
                    {d.allocated}/{d.capacity}
                  </span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full ${d.allocated >= d.capacity ? "bg-rose-500" : "bg-violet-500"}`}
                    style={{ width: `${Math.min(100, (d.allocated / d.capacity) * 100)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </Panel>
        <Panel title="Quick actions" subtitle="Continue with common department tasks">
          <div className="grid gap-2">
            {[
              ["Allocate students", "students"],
              ["Generate roll numbers", "rolls"],
              ["Assign subjects", "subjects"],
            ].map(([label, key]) => (
              <button
                key={key}
                onClick={() => setTab(key)}
                className="flex items-center justify-between rounded-xl border border-slate-200 p-3 text-left text-sm font-semibold text-slate-700 hover:border-violet-300 hover:bg-violet-50"
              >
                {label}
                <ChevronRight className="h-4 w-4" />
              </button>
            ))}
          </div>
        </Panel>
      </div>
      <Panel title="Recent activity" subtitle="Latest department allocation changes">
        <div className="divide-y divide-slate-100">
          {data.recentActivity.length ? (
            data.recentActivity.map((a, i) => (
              <div className="flex gap-3 py-3" key={`${a.occurredAt}-${i}`}>
                <div className="mt-1 h-2.5 w-2.5 rounded-full bg-violet-500" />
                <div>
                  <p className="text-sm font-medium text-slate-700">{a.message}</p>
                  <p className="text-xs text-slate-400">
                    {new Date(a.occurredAt).toLocaleString()}
                  </p>
                </div>
              </div>
            ))
          ) : (
            <Empty
              title="No recent changes"
              text="Allocation and class teacher activity will appear here."
            />
          )}
        </div>
      </Panel>
    </>
  );
}

type Runner = (task: () => Promise<unknown>, message: string) => Promise<void>;
function StudentsPanel({
  data,
  search,
  setSearch,
  allocationDepartment,
  setAllocationDepartment,
  courseYearFilter,
  setCourseYearFilter,
  selected,
  setSelected,
  target,
  setTarget,
  busy,
  run,
}: {
  data: api.Workspace;
  search: string;
  setSearch: (x: string) => void;
  allocationDepartment: string;
  setAllocationDepartment: (x: string) => void;
  courseYearFilter: string;
  setCourseYearFilter: (x: string) => void;
  selected: number[];
  setSelected: React.Dispatch<React.SetStateAction<number[]>>;
  target: string;
  setTarget: (x: string) => void;
  busy: boolean;
  run: Runner;
}) {
  const courseYears = Array.from(
    new Map(
      data.divisions.map((division) => [division.courseYearId, division.courseYear]),
    ).entries(),
  );
  const readyToLoad = allocationDepartment === String(data.departmentId) && !!courseYearFilter;
  const visibleStudents = readyToLoad
    ? data.students.filter(
        (student) =>
          String(student.courseYearId) === courseYearFilter &&
          student.allocationStatus === "UNALLOCATED",
      )
    : [];
  const chosen = data.students.filter((s) => selected.includes(s.id));
  const selectedCourseYears = [...new Set(chosen.map((s) => s.courseYearId).filter(Boolean))];
  const selectedAcademicYears = [
    ...new Set(chosen.map((s) => s.academicYear?.replace(/[^0-9]/g, "")).filter(Boolean)),
  ];
  const hasSingleAdmittedCourse =
    chosen.length > 0 &&
    selectedCourseYears.length === 1 &&
    selectedAcademicYears.length === 1 &&
    chosen.every((s) => s.courseYearId && s.academicYear);
  const targetDivisions = hasSingleAdmittedCourse
    ? data.divisions.filter(
        (division) =>
          division.courseYearId === selectedCourseYears[0] &&
          division.academicYear.replace(/[^0-9]/g, "") === selectedAcademicYears[0],
      )
    : [];
  const targetDivision = targetDivisions.find((d) => d.id === Number(target));
  const allSelected =
    visibleStudents.length > 0 && visibleStudents.every((s) => selected.includes(s.id));
  return (
    <Panel
      title="Student division allocation"
      subtitle="Select the department and course year to load unallocated students, then assign an existing division."
    >
      <div className="mb-5 grid gap-3 rounded-2xl border border-violet-100 bg-violet-50/60 p-4 md:grid-cols-3">
        <label>
          <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-slate-500">
            1. Department
          </span>
          <select
            className={inputClass}
            value={allocationDepartment}
            onChange={(e) => {
              setAllocationDepartment(e.target.value);
              setCourseYearFilter("");
              setSelected([]);
              setTarget("");
            }}
          >
            <option value="">Select department</option>
            <option value={data.departmentId}>{data.department}</option>
          </select>
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-slate-500">
            2. Course year
          </span>
          <select
            className={inputClass}
            value={courseYearFilter}
            disabled={!allocationDepartment}
            onChange={(e) => {
              setCourseYearFilter(e.target.value);
              setSelected([]);
              setTarget("");
            }}
          >
            <option value="">Select FY / SY / TY</option>
            {courseYears.map(([id, name]) => (
              <option key={id} value={id}>
                {name.match(/\b(FY|SY|TY)\b/i)?.[1].toUpperCase() ?? name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-slate-500">
            3. Division
          </span>
          <select
            className={inputClass}
            value={target}
            disabled={!readyToLoad || !selected.length}
            onChange={(e) => setTarget(e.target.value)}
          >
            <option value="">Select created division</option>
            {targetDivisions.map((d) => (
              <option key={d.id} value={d.id}>
                Division {d.name} ({d.allocated}/{d.capacity})
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="mb-5">
        <label className="relative">
          <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
          <input
            className={`${inputClass} pl-9`}
            placeholder="Search student or admission number"
            value={search}
            disabled={!readyToLoad}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        <select className={inputClass} value="" onChange={() => undefined} hidden>
          <option value="">All divisions</option>
          {data.divisions.map((d) => (
            <option value={d.id} key={d.id}>
              {d.courseYear} · {d.name}
            </option>
          ))}
        </select>
        <select className={inputClass} value="" onChange={() => undefined} hidden>
          <option value="">All allocations</option>
          <option value="UNALLOCATED">Unallocated</option>
          <option value="ALLOCATED">Allocated</option>
        </select>
      </div>
      <div className="mb-4 flex flex-col gap-3 rounded-2xl bg-slate-50 p-3 lg:flex-row lg:items-center">
        <span className="text-sm font-semibold text-slate-700">{selected.length} selected</span>
        <button
          className={`${actionClass} lg:ml-auto`}
          disabled={
            busy ||
            !targetDivision ||
            !target ||
            !selected.length ||
            chosen.some((s) => s.allocationStatus !== "UNALLOCATED")
          }
          onClick={() =>
            void run(() => api.bulkAllocate(Number(target), selected), "Students allocated")
          }
        >
          Assign division
        </button>
      </div>
      <div className="responsive-table">
        <table>
          <thead>
            <tr>
              <th className="p-3">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={() => {
                    setSelected(allSelected ? [] : visibleStudents.map((s) => s.id));
                    setTarget("");
                  }}
                />
              </th>
              <th>Student</th>
              <th>Admission</th>
              <th>Course year</th>
              <th>Division</th>
              <th>Roll number</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {visibleStudents.map((s) => (
              <tr key={s.id} className="border-t">
                <td className="p-3">
                  <input
                    type="checkbox"
                    checked={selected.includes(s.id)}
                    onChange={() => {
                      setSelected((v) =>
                        v.includes(s.id) ? v.filter((x) => x !== s.id) : [...v, s.id],
                      );
                      setTarget("");
                    }}
                  />
                </td>
                <td className="font-semibold text-slate-800">
                  {s.name}
                  <div className="text-xs font-normal text-slate-400">{s.gender}</div>
                </td>
                <td>{s.admissionNumber}</td>
                <td>{s.courseYear || "—"}</td>
                <td>{s.division || "Not allocated"}</td>
                <td>{s.rollNumber || "Pending"}</td>
                <td>
                  <Badge value={s.allocationStatus} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!visibleStudents.length && (
          <Empty
            title={
              readyToLoad ? "No unallocated students found" : "Select department and course year"
            }
            text={
              readyToLoad
                ? "All matching students may already be allocated, or no approved students match your search."
                : "Choose a department, then FY, SY, or TY to load unallocated students."
            }
          />
        )}
      </div>
    </Panel>
  );
}

function RollPanel({
  divisions,
  busy,
  run,
}: {
  divisions: api.Division[];
  busy: boolean;
  run: Runner;
}) {
  const [section, setSection] = useState(""),
    [strategy, setStrategy] = useState("ALPHABETICAL_NAME"),
    [rows, setRows] = useState<api.RollPreview[]>([]);
  const preview = async () => {
    if (!section) return;
    try {
      setRows(await api.previewRolls(Number(section), strategy));
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  return (
    <Panel
      title="Roll number generation"
      subtitle="Preview the sequence, edit individual values, then confirm the batch."
    >
      <div className="grid gap-3 md:grid-cols-[1fr_240px_auto]">
        <select
          className={inputClass}
          value={section}
          onChange={(e) => {
            setSection(e.target.value);
            setRows([]);
          }}
        >
          <option value="">Select division</option>
          {divisions.map((d) => (
            <option key={d.id} value={d.id}>
              {d.courseYear} · {d.name}
            </option>
          ))}
        </select>
        <select
          className={inputClass}
          value={strategy}
          onChange={(e) => setStrategy(e.target.value)}
        >
          <option value="ALPHABETICAL_NAME">Alphabetical name</option>
          <option value="ADMISSION_DATE">Admission date</option>
          <option value="ADMISSION_NUMBER">Admission number</option>
          <option value="MERIT_RANK">Merit rank / admission</option>
        </select>
        <button className={secondaryClass} disabled={!section} onClick={() => void preview()}>
          Preview
        </button>
      </div>
      {rows.length > 0 && (
        <>
          <div className="responsive-table mt-5">
            <table>
              <thead>
                <tr>
                  <th className="p-3">Student</th>
                  <th>Admission</th>
                  <th>Current</th>
                  <th>Proposed roll number</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr className="border-t" key={r.studentId}>
                    <td className="p-3 font-semibold">{r.studentName}</td>
                    <td>{r.admissionNumber}</td>
                    <td>{r.currentRollNumber || "—"}</td>
                    <td>
                      <input
                        className={`${inputClass} max-w-52`}
                        value={r.proposedRollNumber}
                        onChange={(e) =>
                          setRows((v) =>
                            v.map((x, j) =>
                              j === i ? { ...x, proposedRollNumber: e.target.value } : x,
                            ),
                          )
                        }
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button
            className={`${actionClass} mt-4`}
            disabled={busy || rows.some((r) => !r.proposedRollNumber.trim())}
            onClick={() =>
              void run(
                () =>
                  api.confirmRolls(
                    Number(section),
                    rows.map((r) => ({ studentId: r.studentId, rollNumber: r.proposedRollNumber })),
                  ),
                "Roll numbers generated",
              )
            }
          >
            <Check className="h-4 w-4" />
            Confirm roll numbers
          </button>
        </>
      )}
    </Panel>
  );
}

function ClassTeachersPanel({
  data,
  busy,
  run,
}: {
  data: api.Workspace;
  busy: boolean;
  run: Runner;
}) {
  const [values, setValues] = useState<Record<number, string>>({});
  return (
    <Panel
      title="Class teacher assignments"
      subtitle="Each active teacher can own at most one division."
    >
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {data.divisions.map((d) => (
          <div className="rounded-2xl border border-slate-200 p-5" key={d.id}>
            <p className="font-bold text-slate-800">
              {d.courseYear} · {d.name}
            </p>
            <p className="mb-4 text-sm text-slate-500">
              Current: {d.classTeacher || "Not assigned"}
            </p>
            <select
              className={inputClass}
              value={values[d.id] || ""}
              onChange={(e) => setValues((v) => ({ ...v, [d.id]: e.target.value }))}
            >
              <option value="">Select teacher</option>
              {data.eligibleClassTeachers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
            {!data.eligibleClassTeachers.length && (
              <p className="mt-2 text-xs text-amber-700">
                No unassigned teachers are available in this department.
              </p>
            )}
            <button
              className={`${actionClass} mt-3 w-full`}
              disabled={busy || !values[d.id]}
              onClick={() =>
                void run(
                  () => api.assignClassTeacher(d.id, Number(values[d.id])),
                  "Class teacher assigned",
                )
              }
            >
              Assign class teacher
            </button>
          </div>
        ))}
      </div>
    </Panel>
  );
}

function WorkloadPanel({ teachers }: { teachers: api.Teacher[] }) {
  const total = teachers.reduce((n, t) => n + t.weeklyLectures, 0);
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-3">
        <Metric label="Department lectures" value={total} />
        <Metric
          label="Average per teacher"
          value={teachers.length ? Math.round(total / teachers.length) : 0}
        />
        <Metric label="At capacity" value={teachers.filter((t) => t.loadStatus === "RED").length} />
      </div>
      <Panel
        title="Teacher workload"
        subtitle="Lecture load is calculated from the active weekly timetable (maximum 24)."
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {teachers.map((t) => (
            <div className="rounded-2xl border border-slate-200 p-5" key={t.id}>
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-bold text-slate-800">{t.name}</p>
                  <p className="text-xs text-slate-500">
                    {t.employeeCode} · {t.staffType.replaceAll("_", " ")}
                  </p>
                </div>
                <span
                  className={`h-3 w-3 rounded-full ${t.loadStatus === "RED" ? "bg-rose-500" : t.loadStatus === "YELLOW" ? "bg-amber-500" : "bg-emerald-500"}`}
                />
              </div>
              <div className="mt-5 flex items-end justify-between">
                <span className="text-3xl font-bold">
                  {t.weeklyLectures}
                  <small className="text-sm font-medium text-slate-400"> / 24</small>
                </span>
                <span className="text-xs text-slate-500">{t.remainingCapacity} remaining</span>
              </div>
              <div className="mt-3 h-2 rounded-full bg-slate-100">
                <div
                  className={`h-full rounded-full ${t.loadStatus === "RED" ? "bg-rose-500" : t.loadStatus === "YELLOW" ? "bg-amber-500" : "bg-emerald-500"}`}
                  style={{ width: `${Math.min(100, (t.weeklyLectures / 24) * 100)}%` }}
                />
              </div>
              <div className="mt-4 flex gap-4 text-xs text-slate-500">
                <span>{t.subjects} subjects</span>
                <span>{t.divisions} divisions</span>
              </div>
            </div>
          ))}
        </div>
      </Panel>
    </>
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
function Empty({ title, text }: { title: string; text: string }) {
  return (
    <div className="py-10 text-center">
      <p className="font-semibold text-slate-700">{title}</p>
      <p className="mt-1 text-sm text-slate-400">{text}</p>
    </div>
  );
}
function Badge({ value }: { value: string }) {
  const good = ["ALLOCATED", "APPROVED", "ACTIVE"].includes(value),
    warn = ["SUBMITTED", "CHANGES_REQUESTED", "UNALLOCATED"].includes(value);
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${good ? "bg-emerald-50 text-emerald-700" : warn ? "bg-amber-50 text-amber-700" : "bg-slate-100 text-slate-600"}`}
    >
      {value.replaceAll("_", " ")}
    </span>
  );
}
function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-3xl font-bold text-slate-900">{value}</p>
      <p className="text-sm text-slate-500">{label}</p>
    </div>
  );
}
function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between rounded-xl bg-slate-50 p-3">
      <span>{label}</span>
      <b className="text-slate-800">{value}</b>
    </div>
  );
}
