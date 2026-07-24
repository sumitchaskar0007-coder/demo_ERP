import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import {
  Activity,
  ArrowRight,
  BookOpen,
  CalendarCheck,
  ChevronRight,
  CircleGauge,
  ClipboardCheck,
  Clock3,
  GraduationCap,
  LayoutDashboard,
  RefreshCw,
  Search,
  UserCheck,
  Users,
} from "lucide-react";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/hod/api";

const tabs = [
  ["overview", "Overview", LayoutDashboard],
  ["students", "Student allocation", Users],
  ["class-teachers", "Class teachers", Users],
  ["workload", "Workload", Activity],
  ["attendance", "Attendance", ClipboardCheck],
] as const;
const actionClass =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50";
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
    <div className="mx-auto max-w-[1500px] space-y-6 px-3 py-4 pb-10 sm:px-6 sm:py-5 lg:px-0 lg:py-0">
      <div className="overflow-hidden rounded-2xl border border-brand-100 bg-gradient-to-r from-brand-50 via-white to-blue-50 p-6 shadow-sm md:p-7">
        <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-brand-100 px-3 py-1 text-xs font-bold text-brand-700">
              <CircleGauge className="h-3.5 w-3.5" />
              Department control centre
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-950 md:text-3xl">
              {data.department} HOD Workspace
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-600">
              Review today&apos;s priorities and manage your department from one place.
            </p>
          </div>
          <button
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-4 py-2.5 text-sm font-semibold text-brand-700 shadow-sm transition hover:bg-brand-50"
            onClick={() => void load()}
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
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
              className={`flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-semibold ${tab === key ? "bg-brand-600 text-white shadow-sm" : "text-slate-600 hover:bg-slate-100"}`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>
      </div>
      {tab === "overview" && (
        <Overview data={data} setTab={(next) => setParams({ tab: next })} navigate={navigate} />
      )}
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

function Overview({
  data,
  setTab,
  navigate,
}: {
  data: api.Workspace;
  setTab: (x: string) => void;
  navigate: (to: string) => void;
}) {
  const unallocatedStudents = data.totalStudents;
  const divisionsWithoutTeacher = data.divisions.filter((division) => !division.classTeacher);
  const unassignedSubjects = data.subjects.filter((subject) => subject.teacherIds.length === 0);
  const overloadedTeachers = data.teachers.filter((teacher) => teacher.loadStatus === "RED");
  const timetablesNeedingWork = data.timetables.filter(
    (timetable) =>
      timetable.lectures === 0 ||
      timetable.reviewStatus === "DRAFT" ||
      timetable.reviewStatus === "REJECTED",
  );

  const stats = [
    {
      label: "Department students",
      value: data.summary.totalStudents,
      detail: `${unallocatedStudents} awaiting division`,
      icon: GraduationCap,
      tone: "bg-blue-50 text-blue-700",
      action: () => setTab("students"),
    },
    {
      label: "Teaching staff",
      value: data.summary.totalTeachers,
      detail: `${overloadedTeachers.length} need workload review`,
      icon: Users,
      tone: "bg-violet-50 text-violet-700",
      action: () => setTab("workload"),
    },
    {
      label: "Attendance",
      value: `${data.summary.averageAttendance.toFixed(1)}%`,
      detail: data.summary.averageAttendance < 75 ? "Below 75% target" : "Department average",
      icon: UserCheck,
      tone:
        data.summary.averageAttendance < 75
          ? "bg-rose-50 text-rose-700"
          : "bg-emerald-50 text-emerald-700",
      action: () => setTab("attendance"),
    },
    {
      label: "Classes today",
      value: data.summary.classesRunningToday,
      detail: `${data.summary.totalDivisions} active divisions`,
      icon: CalendarCheck,
      tone: "bg-cyan-50 text-cyan-700",
      action: () => navigate(ROUTES.timetable),
    },
  ];

  const attention = [
    {
      label: "Students awaiting division",
      value: unallocatedStudents,
      description: "Allocate admitted students to an active division.",
      icon: GraduationCap,
      action: () => setTab("students"),
    },
    {
      label: "Divisions without class teacher",
      value: divisionsWithoutTeacher.length,
      description: "Assign an eligible teacher to each uncovered division.",
      icon: UserCheck,
      action: () => setTab("class-teachers"),
    },
    {
      label: "Subjects without teacher",
      value: unassignedSubjects.length,
      description: "Complete teaching assignments before timetable planning.",
      icon: BookOpen,
      action: () => navigate(ROUTES.subjectTeacherAssignments),
    },
    {
      label: "Timetables needing attention",
      value: timetablesNeedingWork.length,
      description: "Finish drafts and resolve rejected timetable submissions.",
      icon: Clock3,
      action: () => navigate(ROUTES.timetable),
    },
  ];

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map(({ label, value, detail, icon: Icon, tone, action }) => (
          <button
            key={label}
            onClick={action}
            className="group rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md"
          >
            <div className="flex items-start justify-between gap-4">
              <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${tone}`}>
                <Icon className="h-5 w-5" />
              </div>
              <ArrowRight className="h-4 w-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand-600" />
            </div>
            <p className="mt-4 text-2xl font-bold text-slate-950">{value}</p>
            <p className="mt-0.5 text-sm font-semibold text-slate-700">{label}</p>
            <p className="mt-1 text-xs text-slate-500">{detail}</p>
          </button>
        ))}
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.2fr_.8fr]">
        <Panel title="Needs your attention" subtitle="Priority academic tasks for your department">
          <div className="grid gap-3 sm:grid-cols-2">
            {attention.map(({ label, value, description, icon: Icon, action }) => (
              <button
                key={label}
                onClick={action}
                className="group rounded-xl border border-slate-200 p-4 text-left transition hover:border-brand-200 hover:bg-brand-50/40"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-50 text-amber-700">
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                      value > 0 ? "bg-rose-50 text-rose-700" : "bg-emerald-50 text-emerald-700"
                    }`}
                  >
                    {value}
                  </span>
                </div>
                <p className="mt-3 text-sm font-bold text-slate-800">{label}</p>
                <p className="mt-1 text-xs leading-5 text-slate-500">{description}</p>
                <span className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-brand-600">
                  {value > 0 ? "Review now" : "View records"}
                  <ChevronRight className="h-3.5 w-3.5 transition group-hover:translate-x-0.5" />
                </span>
              </button>
            ))}
          </div>
        </Panel>

        <Panel title="Quick actions" subtitle="Common department operations">
          <div className="grid gap-2">
            {[
              {
                label: "Allocate students",
                description: "Assign students to course years and divisions",
                icon: GraduationCap,
                action: () => setTab("students"),
              },
              {
                label: "Teaching assignments",
                description: "Allocate subjects to eligible teachers",
                icon: BookOpen,
                action: () => navigate(ROUTES.subjectTeacherAssignments),
              },
              {
                label: "Manage timetable",
                description: "Plan the department teaching schedule",
                icon: CalendarCheck,
                action: () => navigate(ROUTES.timetable),
              },
            ].map(({ label, description, icon: Icon, action }) => (
              <button
                key={label}
                onClick={action}
                className="group flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-left transition hover:border-brand-200 hover:bg-brand-50/50"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
                  <Icon className="h-4.5 w-4.5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-slate-800">{label}</p>
                  <p className="truncate text-xs text-slate-500">{description}</p>
                </div>
                <ChevronRight className="h-4 w-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand-600" />
              </button>
            ))}
          </div>
        </Panel>
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.1fr_.9fr]">
        <Panel title="Division capacity" subtitle="Current allocation against approved capacity">
          <div className="space-y-4">
            {data.divisions.length ? (
              data.divisions.map((division) => {
                const usage =
                  division.capacity > 0
                    ? Math.min(100, (division.allocated / division.capacity) * 100)
                    : 0;
                return (
                  <div key={division.id}>
                    <div className="mb-1.5 flex justify-between gap-3 text-sm">
                      <span className="truncate font-semibold text-slate-700">
                        {division.courseYear} · {division.name}
                      </span>
                      <span className="shrink-0 text-slate-500">
                        {division.allocated}/{division.capacity}
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className={`h-full rounded-full ${
                          usage >= 100
                            ? "bg-rose-500"
                            : usage >= 85
                              ? "bg-amber-500"
                              : "bg-brand-500"
                        }`}
                        style={{ width: `${usage}%` }}
                      />
                    </div>
                  </div>
                );
              })
            ) : (
              <Empty title="No divisions" text="Active divisions will appear here." />
            )}
          </div>
        </Panel>

        <Panel title="Teacher workload" subtitle="Highest weekly lecture allocation">
          <div className="space-y-2">
            {[...data.teachers]
              .sort((a, b) => b.weeklyLectures - a.weeklyLectures)
              .slice(0, 5)
              .map((teacher) => (
                <button
                  key={teacher.id}
                  onClick={() => setTab("workload")}
                  className="flex w-full items-center gap-3 rounded-xl border border-slate-100 px-3 py-2.5 text-left hover:bg-slate-50"
                >
                  <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-50 text-sm font-bold text-brand-700">
                    {teacher.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-slate-800">{teacher.name}</p>
                    <p className="text-xs text-slate-500">
                      {teacher.subjects} subjects · {teacher.divisions} divisions
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-slate-800">{teacher.weeklyLectures}/24</p>
                    <p
                      className={`text-[11px] font-bold ${
                        teacher.loadStatus === "RED"
                          ? "text-rose-600"
                          : teacher.loadStatus === "YELLOW"
                            ? "text-amber-600"
                            : "text-emerald-600"
                      }`}
                    >
                      {teacher.loadStatus === "RED"
                        ? "Overloaded"
                        : teacher.loadStatus === "YELLOW"
                          ? "Near capacity"
                          : "Available"}
                    </p>
                  </div>
                </button>
              ))}
            {!data.teachers.length && (
              <Empty title="No teachers" text="Teaching staff will appear here." />
            )}
          </div>
        </Panel>
      </div>

      <Panel title="Recent activity" subtitle="Latest department allocation changes">
        <div className="divide-y divide-slate-100">
          {data.recentActivity.length ? (
            data.recentActivity.slice(0, 6).map((activity, index) => (
              <div className="flex items-start gap-3 py-3" key={`${activity.occurredAt}-${index}`}>
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                  <Activity className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-700">{activity.message}</p>
                  <p className="mt-0.5 text-xs text-slate-400">
                    {new Date(activity.occurredAt).toLocaleString()}
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
