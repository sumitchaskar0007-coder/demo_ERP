import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import {
  Activity,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  CalendarCheck,
  CheckCircle2,
  ChevronRight,
  CircleGauge,
  Clock3,
  GraduationCap,
  RefreshCw,
  Search,
  UserCheck,
  Users,
} from "lucide-react";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import { sectionStudents, type StudentRosterItem } from "@/features/academic/api";
import { weeklyTimetableApi, type WeeklyTimetable } from "@/features/academics/api";
import { DivisionTimetable } from "@/pages/academic/CourseYearDivisionPages";
import * as api from "@/features/hod/api";

const tabs = [
  "overview",
  "students",
  "divisions",
  "class-teachers",
  "workload",
  "attendance",
] as const;
const actionClass =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50";
const inputClass =
  "h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none transition focus:border-violet-400 focus:ring-4 focus:ring-violet-100";

export function HodWorkspacePage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const requestedTab = params.get("tab") || "overview";
  const tab = tabs.some((key) => key === requestedTab) ? requestedTab : "overview";
  const [data, setData] = useState<api.Workspace | null>(null),
    [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false);
  const [search, setSearch] = useState(""),
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
      {tab === "overview" && (
        <Overview data={data} setTab={(next) => setParams({ tab: next })} navigate={navigate} />
      )}
      {tab === "students" && (
        <StudentsPanel
          data={data}
          search={search}
          setSearch={setSearch}
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
      {tab === "divisions" && (
        <DivisionsPanel data={data} setTab={(next) => setParams({ tab: next })} />
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
  const [activityPage, setActivityPage] = useState(0);
  const activityPageSize = 6;
  const activityPages = Math.max(1, Math.ceil(data.recentActivity.length / activityPageSize));
  const safeActivityPage = Math.min(activityPage, activityPages - 1);
  const visibleActivity = data.recentActivity.slice(
    safeActivityPage * activityPageSize,
    (safeActivityPage + 1) * activityPageSize,
  );
  const unallocatedStudents = data.totalStudents;
  const divisionsWithoutTeacher = data.divisions.filter((division) => !division.classTeacher);
  const unassignedSubjects = data.subjects.filter((subject) => subject.teacherIds.length === 0);
  const overloadedTeachers = data.teachers.filter((teacher) => teacher.loadStatus === "RED");
  const existingTimetablesNeedingWork = data.timetables.filter(
    (timetable) =>
      timetable.lectures === 0 ||
      timetable.reviewStatus === "DRAFT" ||
      timetable.reviewStatus === "REJECTED",
  );
  const timetableDivisionIds = new Set(data.timetables.map((timetable) => timetable.divisionId));
  const missingTimetableDivisions = data.divisions.filter(
    (division) => !timetableDivisionIds.has(division.id),
  );
  const timetablesNeedingWork =
    existingTimetablesNeedingWork.length + missingTimetableDivisions.length;

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
      value: timetablesNeedingWork,
      description: missingTimetableDivisions.length
        ? `Not created: ${missingTimetableDivisions
            .map((division) => `${division.courseYear} · ${division.name}`)
            .join(", ")}`
        : "Finish drafts and resolve rejected timetable submissions.",
      icon: Clock3,
      action: () =>
        navigate(
          missingTimetableDivisions[0]
            ? `${ROUTES.timetable}?sectionId=${missingTimetableDivisions[0].id}`
            : ROUTES.timetable,
        ),
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
                    <p className="text-sm font-bold text-slate-800">{teacher.weeklyLectures}</p>
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
          {visibleActivity.length ? (
            visibleActivity.map((activity, index) => (
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
        {activityPages > 1 && (
          <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4">
            <p className="text-xs font-medium text-slate-500">
              Page {safeActivityPage + 1} of {activityPages}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={safeActivityPage === 0}
                onClick={() => setActivityPage((page) => Math.max(0, page - 1))}
                className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Previous
              </button>
              <button
                type="button"
                disabled={safeActivityPage >= activityPages - 1}
                onClick={() => setActivityPage((page) => Math.min(activityPages - 1, page + 1))}
                className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </Panel>
    </>
  );
}

type Runner = (task: () => Promise<unknown>, message: string) => Promise<void>;

function DivisionsPanel({ data, setTab }: { data: api.Workspace; setTab: (x: string) => void }) {
  const [courseYearId, setCourseYearId] = useState("");
  const [selectedDivisionId, setSelectedDivisionId] = useState<number | null>(null);
  const [students, setStudents] = useState<StudentRosterItem[]>([]);
  const [timetable, setTimetable] = useState<WeeklyTimetable | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const courseYears = Array.from(
    new Map(
      data.divisions.map((division) => [division.courseYearId, division.courseYear]),
    ).entries(),
  );
  const divisions = data.divisions
    .filter((division) => !courseYearId || division.courseYearId === Number(courseYearId))
    .sort(
      (a, b) =>
        a.courseYear.localeCompare(b.courseYear) ||
        a.name.localeCompare(b.name, undefined, { numeric: true }),
    );
  const allocatedStudents = divisions.reduce((total, division) => total + division.allocated, 0);
  const totalCapacity = divisions.reduce((total, division) => total + division.capacity, 0);
  const selectedDivision = data.divisions.find((division) => division.id === selectedDivisionId);

  useEffect(() => {
    if (!selectedDivisionId) {
      setStudents([]);
      setTimetable(null);
      return;
    }
    let active = true;
    setDetailsLoading(true);
    Promise.all([sectionStudents(selectedDivisionId), weeklyTimetableApi.get(selectedDivisionId)])
      .then(([studentRows, timetableResponse]) => {
        if (!active) return;
        setStudents(studentRows);
        setTimetable(timetableResponse);
      })
      .catch((error) => {
        if (active) toast.error(handleApiError(error).message);
      })
      .finally(() => {
        if (active) setDetailsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [selectedDivisionId]);

  if (selectedDivision) {
    return (
      <DivisionDetails
        division={selectedDivision}
        students={students}
        timetable={timetable}
        loading={detailsLoading}
        onBack={() => setSelectedDivisionId(null)}
        manageClassTeacher={() => setTab("class-teachers")}
      />
    );
  }

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-3">
        <Metric label="Active divisions" value={divisions.length} />
        <Metric label="Allocated students" value={allocatedStudents} />
        <Metric label="Total capacity" value={totalCapacity} />
      </div>
      <Panel
        title="Divisions"
        subtitle={`Browse the active divisions in the ${data.department} department.`}
      >
        <div className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:grid-cols-2">
          <label>
            <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-slate-500">
              Department
            </span>
            <input
              className={`${inputClass} cursor-not-allowed bg-slate-100`}
              value={data.department}
              readOnly
            />
          </label>
          <label>
            <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-slate-500">
              Course year
            </span>
            <select
              className={inputClass}
              value={courseYearId}
              onChange={(event) => setCourseYearId(event.target.value)}
            >
              <option value="">All course years</option>
              {courseYears.map(([id, name]) => (
                <option key={id} value={id}>
                  {name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="responsive-table">
          <table>
            <thead>
              <tr>
                <th>Course year</th>
                <th>Academic year</th>
                <th>Division</th>
                <th>Code</th>
                <th>Students</th>
                <th>Capacity</th>
                <th>Class teacher</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {divisions.map((division) => (
                <tr className="border-t" key={division.id}>
                  <td className="font-semibold text-slate-800">{division.courseYear}</td>
                  <td>{division.academicYear}</td>
                  <td>
                    <button
                      className="font-semibold text-brand-700 hover:underline"
                      onClick={() => setSelectedDivisionId(division.id)}
                    >
                      {division.name}
                    </button>
                  </td>
                  <td>{division.code}</td>
                  <td>{division.allocated}</td>
                  <td>{division.capacity}</td>
                  <td>{division.classTeacher || "Not assigned"}</td>
                  <td>
                    <button
                      className="text-sm font-semibold text-brand-700 hover:underline"
                      onClick={() => setTab("class-teachers")}
                    >
                      Manage class teacher
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!divisions.length && (
            <Empty
              title="No divisions"
              text="No active divisions match the selected course year."
            />
          )}
        </div>
      </Panel>
    </>
  );
}

function DivisionDetails({
  division,
  students,
  timetable,
  loading,
  onBack,
  manageClassTeacher,
}: {
  division: api.Division;
  students: StudentRosterItem[];
  timetable: WeeklyTimetable | null;
  loading: boolean;
  onBack: () => void;
  manageClassTeacher: () => void;
}) {
  return (
    <div>
      <button
        className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700 hover:underline"
        onClick={onBack}
      >
        <ArrowLeft className="h-4 w-4" />
        Back to divisions
      </button>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-950">
            {division.name} ({division.code})
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {division.courseYear} · {division.academicYear}
          </p>
        </div>
        <button className={actionClass} onClick={manageClassTeacher}>
          Manage class teacher
        </button>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <UserCheck className="h-5 w-5 text-brand-600" />
          <p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">
            Class teacher
          </p>
          <p className="mt-1 font-semibold text-slate-900">
            {division.classTeacher || "Not assigned"}
          </p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <GraduationCap className="h-5 w-5 text-brand-600" />
          <p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">Students</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{division.allocated}</p>
          <p className="text-sm text-slate-500">Capacity {division.capacity}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <BookOpen className="h-5 w-5 text-brand-600" />
          <p className="mt-3 text-xs font-bold uppercase tracking-wide text-slate-500">
            Course year
          </p>
          <p className="mt-1 font-semibold text-slate-900">{division.courseYear}</p>
          <p className="text-sm text-slate-500">{division.academicYear}</p>
        </div>
      </div>

      {loading ? (
        <div className="flex min-h-56 items-center justify-center text-slate-500">
          <RefreshCw className="mr-2 h-5 w-5 animate-spin" />
          Loading division details…
        </div>
      ) : (
        <>
          <DivisionTimetable table={timetable} />
          <Panel title="Student list" subtitle={`${students.length} students in this division`}>
            <div className="responsive-table">
              <table>
                <thead>
                  <tr>
                    <th>Roll number</th>
                    <th>Admission number</th>
                    <th>Student</th>
                    <th>Email</th>
                    <th>Phone</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => (
                    <tr className="border-t" key={student.studentProfileId}>
                      <td>{student.rollNumber || "—"}</td>
                      <td>{student.admissionNumber}</td>
                      <td className="font-semibold text-slate-800">{student.fullName}</td>
                      <td>{student.email || "—"}</td>
                      <td>{student.phone || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!students.length && (
                <Empty
                  title="No students assigned"
                  text="Students assigned to this division will appear here."
                />
              )}
            </div>
          </Panel>
        </>
      )}
    </div>
  );
}

function StudentsPanel({
  data,
  search,
  setSearch,
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
  const readyToLoad = !!courseYearFilter;
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
  const availableSeats = targetDivision
    ? Math.max(0, targetDivision.capacity - targetDivision.allocated)
    : 0;
  return (
    <section className="overflow-hidden rounded-3xl border border-violet-100 bg-white shadow-sm">
      <div className="relative overflow-hidden bg-gradient-to-br from-violet-700 via-brand-700 to-blue-700 px-5 py-6 text-white sm:px-7">
        <div className="absolute -right-12 -top-16 h-52 w-52 rounded-full bg-white/10" />
        <div className="absolute -bottom-20 right-32 h-40 w-40 rounded-full bg-cyan-300/10" />
        <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-bold">
              <GraduationCap className="h-3.5 w-3.5" />
              {data.department} department
            </div>
            <h2 className="text-2xl font-bold tracking-tight">Student division allocation</h2>
            <p className="mt-2 max-w-2xl text-sm text-violet-100">
              Choose a course year, select admitted students, and place them into an available
              division.
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs font-semibold">
            {["Course", "Students", "Division"].map((label, index) => {
              const complete =
                (index === 0 && readyToLoad) ||
                (index === 1 && selected.length > 0) ||
                (index === 2 && Boolean(targetDivision));
              return (
                <div className="flex items-center gap-2" key={label}>
                  <span
                    className={`grid h-7 w-7 place-items-center rounded-full ${
                      complete ? "bg-emerald-400 text-emerald-950" : "bg-white/15 text-white"
                    }`}
                  >
                    {index + 1}
                  </span>
                  <span className="hidden sm:inline">{label}</span>
                  {index < 2 && <ChevronRight className="h-4 w-4 text-white/40" />}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="space-y-6 p-4 sm:p-6">
        <div className="grid gap-4 lg:grid-cols-[0.8fr_1.4fr_1fr]">
          <div className="rounded-2xl border border-emerald-100 bg-emerald-50/70 p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-emerald-700">
                  Department scope
                </p>
                <p className="mt-2 font-bold text-slate-900">{data.department}</p>
                <p className="mt-1 text-xs text-slate-500">Locked to your assigned department</p>
              </div>
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-emerald-100 text-emerald-700">
                <CheckCircle2 className="h-5 w-5" />
              </span>
            </div>
          </div>

          <div className="rounded-2xl border border-violet-100 bg-violet-50/50 p-4">
            <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-violet-700">
              1 · Select course year
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {courseYears.map(([id, name]) => {
                const active = courseYearFilter === String(id);
                return (
                  <button
                    className={`rounded-xl border px-3.5 py-2 text-sm font-semibold transition ${
                      active
                        ? "border-violet-600 bg-violet-600 text-white shadow-sm"
                        : "border-violet-100 bg-white text-slate-700 hover:border-violet-300 hover:text-violet-700"
                    }`}
                    key={id}
                    onClick={() => {
                      setCourseYearFilter(String(id));
                      setSearch("");
                      setSelected([]);
                      setTarget("");
                    }}
                  >
                    {name}
                  </button>
                );
              })}
            </div>
          </div>

          <div
            className={`rounded-2xl border p-4 ${
              targetDivision ? "border-blue-200 bg-blue-50/70" : "border-slate-200 bg-slate-50"
            }`}
          >
            <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-blue-700">
              3 · Destination division
            </p>
            <select
              className={`${inputClass} mt-3`}
              value={target}
              disabled={!readyToLoad || !selected.length}
              onChange={(e) => setTarget(e.target.value)}
            >
              <option value="">
                {selected.length ? "Choose destination" : "Select students first"}
              </option>
              {targetDivisions.map((d) => (
                <option key={d.id} value={d.id} disabled={d.allocated >= d.capacity}>
                  Division {d.name} · {Math.max(0, d.capacity - d.allocated)} seats available
                </option>
              ))}
            </select>
            <p className="mt-2 text-xs text-slate-500">
              {targetDivision
                ? `${targetDivision.allocated} of ${targetDivision.capacity} seats filled`
                : "Compatible divisions appear after student selection."}
            </p>
          </div>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-200">
          <div className="flex flex-col gap-4 border-b border-slate-200 bg-slate-50/80 p-4 lg:flex-row lg:items-center">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-violet-700">
                2 · Select students
              </p>
              <p className="mt-1 text-sm text-slate-500">
                Only approved, unallocated students for the selected course are shown.
              </p>
            </div>
            <label className="relative lg:ml-auto lg:w-80">
              <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
              <input
                className={`${inputClass} bg-white pl-9`}
                placeholder="Search name or admission number"
                value={search}
                disabled={!readyToLoad}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
          </div>

          {readyToLoad && visibleStudents.length > 0 && (
            <div
              className={`flex flex-col gap-3 border-b px-4 py-3 sm:flex-row sm:items-center ${
                selected.length ? "border-violet-100 bg-violet-50" : "border-slate-100 bg-white"
              }`}
            >
              <div className="flex items-center gap-3">
                <span
                  className={`grid h-9 min-w-9 place-items-center rounded-xl px-2 text-sm font-bold ${
                    selected.length ? "bg-violet-600 text-white" : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {selected.length}
                </span>
                <div>
                  <p className="text-sm font-semibold text-slate-800">Students selected</p>
                  <p className="text-xs text-slate-500">
                    {targetDivision
                      ? `${availableSeats} seats available in Division ${targetDivision.name}`
                      : "Choose a destination division to continue"}
                  </p>
                </div>
              </div>
              <button
                className={`${actionClass} sm:ml-auto`}
                disabled={
                  busy ||
                  !targetDivision ||
                  !selected.length ||
                  selected.length > availableSeats ||
                  chosen.some((s) => s.allocationStatus !== "UNALLOCATED")
                }
                onClick={() =>
                  void run(() => api.bulkAllocate(Number(target), selected), "Students allocated")
                }
              >
                {busy ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="h-4 w-4" />
                )}
                Allocate {selected.length || ""} student{selected.length === 1 ? "" : "s"}
              </button>
            </div>
          )}

          <div className="responsive-table border-0 shadow-none">
            <table>
              <thead>
                <tr>
                  <th className="w-12 p-3">
                    <input
                      className="h-4 w-4 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                      type="checkbox"
                      aria-label="Select all students"
                      checked={allSelected}
                      disabled={!visibleStudents.length}
                      onChange={() => {
                        setSelected(allSelected ? [] : visibleStudents.map((s) => s.id));
                        setTarget("");
                      }}
                    />
                  </th>
                  <th>Student</th>
                  <th>Admission</th>
                  <th>Course year</th>
                  <th>Allocation</th>
                </tr>
              </thead>
              <tbody>
                {visibleStudents.map((s) => {
                  const isSelected = selected.includes(s.id);
                  return (
                    <tr
                      key={s.id}
                      className={`border-t transition ${
                        isSelected ? "bg-violet-50/70" : "hover:bg-slate-50"
                      }`}
                    >
                      <td className="p-3">
                        <input
                          className="h-4 w-4 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                          type="checkbox"
                          aria-label={`Select ${s.name}`}
                          checked={isSelected}
                          onChange={() => {
                            setSelected((value) =>
                              value.includes(s.id)
                                ? value.filter((studentId) => studentId !== s.id)
                                : [...value, s.id],
                            );
                            setTarget("");
                          }}
                        />
                      </td>
                      <td>
                        <div className="flex items-center gap-3">
                          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-violet-100 to-blue-100 text-sm font-bold text-violet-700">
                            {s.name.charAt(0).toUpperCase()}
                          </span>
                          <div>
                            <p className="font-semibold text-slate-800">{s.name}</p>
                            <p className="text-xs text-slate-400">{s.gender}</p>
                          </div>
                        </div>
                      </td>
                      <td className="font-medium text-slate-600">{s.admissionNumber}</td>
                      <td>{s.courseYear || "—"}</td>
                      <td>
                        <Badge value={s.allocationStatus} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {!visibleStudents.length && (
              <div className="bg-white">
                <Empty
                  title={
                    readyToLoad ? "No unallocated students found" : "Choose a course year to begin"
                  }
                  text={
                    readyToLoad
                      ? "All matching students may already be allocated, or no approved students match your search."
                      : "Select one of the course-year options above to load eligible students."
                  }
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
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
  const [search, setSearch] = useState("");
  const [assignmentFilter, setAssignmentFilter] = useState("ALL");
  const assignedCount = data.divisions.filter((division) => division.classTeacher).length;
  const unassignedCount = data.divisions.length - assignedCount;
  const visibleDivisions = data.divisions
    .filter((division) => {
      const query = search.trim().toLowerCase();
      const matchesSearch =
        !query ||
        division.name.toLowerCase().includes(query) ||
        division.code.toLowerCase().includes(query) ||
        division.courseYear.toLowerCase().includes(query) ||
        division.classTeacher?.toLowerCase().includes(query);
      const matchesStatus =
        assignmentFilter === "ALL" ||
        (assignmentFilter === "ASSIGNED" && Boolean(division.classTeacher)) ||
        (assignmentFilter === "UNASSIGNED" && !division.classTeacher);
      return matchesSearch && matchesStatus;
    })
    .sort(
      (a, b) =>
        Number(Boolean(a.classTeacher)) - Number(Boolean(b.classTeacher)) ||
        a.courseYear.localeCompare(b.courseYear) ||
        a.name.localeCompare(b.name, undefined, { numeric: true }),
    );

  return (
    <section className="overflow-hidden rounded-3xl border border-blue-100 bg-white shadow-sm">
      <div className="relative overflow-hidden border-b border-blue-100 bg-gradient-to-br from-blue-50 via-white to-violet-50 px-5 py-6 sm:px-7">
        <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full border border-blue-100 bg-blue-100/50" />
        <div className="relative flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-blue-100 px-3 py-1 text-xs font-bold text-blue-700">
              <UserCheck className="h-3.5 w-3.5" />
              Division leadership
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-slate-950">
              Class teacher assignments
            </h2>
            <p className="mt-2 max-w-2xl text-sm text-slate-600">
              Assign one responsible class teacher to each division in the {data.department}{" "}
              department.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-2 sm:min-w-[430px]">
            {[
              ["Divisions", data.divisions.length, "text-blue-700 bg-blue-50 border-blue-100"],
              ["Assigned", assignedCount, "text-emerald-700 bg-emerald-50 border-emerald-100"],
              ["Pending", unassignedCount, "text-amber-700 bg-amber-50 border-amber-100"],
            ].map(([label, value, tone]) => (
              <div className={`rounded-2xl border p-3 ${tone}`} key={String(label)}>
                <p className="text-2xl font-bold">{value}</p>
                <p className="text-[11px] font-bold uppercase tracking-wide">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="p-4 sm:p-6">
        <div className="mb-6 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50/80 p-3 md:flex-row md:items-center">
          <label className="relative flex-1">
            <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <input
              className={`${inputClass} bg-white pl-9`}
              placeholder="Search division, course year, code, or teacher"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <div className="flex rounded-xl border border-slate-200 bg-white p-1">
            {[
              ["ALL", "All"],
              ["UNASSIGNED", "Pending"],
              ["ASSIGNED", "Assigned"],
            ].map(([value, label]) => (
              <button
                className={`rounded-lg px-3 py-2 text-xs font-bold transition ${
                  assignmentFilter === value
                    ? "bg-blue-600 text-white shadow-sm"
                    : "text-slate-500 hover:bg-slate-100 hover:text-slate-800"
                }`}
                key={value}
                onClick={() => setAssignmentFilter(value)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {visibleDivisions.map((division) => {
            const selectedTeacher = data.eligibleClassTeachers.find(
              (teacher) => teacher.id === Number(values[division.id]),
            );
            const seatsUsed =
              division.capacity > 0
                ? Math.min(100, (division.allocated / division.capacity) * 100)
                : 0;
            return (
              <article
                className={`group relative overflow-hidden rounded-2xl border bg-white transition hover:-translate-y-0.5 hover:shadow-lg ${
                  division.classTeacher ? "border-emerald-100" : "border-amber-100"
                }`}
                key={division.id}
              >
                <div
                  className={`absolute inset-x-0 top-0 h-1 ${
                    division.classTeacher
                      ? "bg-gradient-to-r from-emerald-400 to-teal-500"
                      : "bg-gradient-to-r from-amber-400 to-orange-500"
                  }`}
                />
                <div className="p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-blue-600">
                        {division.courseYear}
                      </p>
                      <h3 className="mt-1 truncate text-base font-bold text-slate-950">
                        {division.name}
                      </h3>
                      <p className="mt-1 text-xs text-slate-400">
                        {division.code} · {division.academicYear}
                      </p>
                    </div>
                    <span
                      className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase ${
                        division.classTeacher
                          ? "bg-emerald-50 text-emerald-700"
                          : "bg-amber-50 text-amber-700"
                      }`}
                    >
                      <span
                        className={`h-1.5 w-1.5 rounded-full ${
                          division.classTeacher ? "bg-emerald-500" : "bg-amber-500"
                        }`}
                      />
                      {division.classTeacher ? "Assigned" : "Pending"}
                    </span>
                  </div>

                  <div className="mt-5 rounded-xl border border-slate-100 bg-slate-50 p-3">
                    <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
                      Current class teacher
                    </p>
                    <div className="mt-2 flex items-center gap-3">
                      <span
                        className={`grid h-9 w-9 shrink-0 place-items-center rounded-full text-sm font-bold ${
                          division.classTeacher
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-amber-100 text-amber-700"
                        }`}
                      >
                        {division.classTeacher?.charAt(0).toUpperCase() || "?"}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-slate-800">
                          {division.classTeacher || "Not assigned"}
                        </p>
                        <p className="text-xs text-slate-400">
                          {division.classTeacher
                            ? "Responsible for this division"
                            : "Select an available teacher below"}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="mt-4">
                    <div className="mb-1.5 flex items-center justify-between text-xs">
                      <span className="font-medium text-slate-500">Student capacity</span>
                      <span className="font-bold text-slate-700">
                        {division.allocated}/{division.capacity}
                      </span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className={`h-full rounded-full ${
                          seatsUsed >= 100 ? "bg-rose-500" : "bg-blue-500"
                        }`}
                        style={{ width: `${seatsUsed}%` }}
                      />
                    </div>
                  </div>

                  <label className="mt-5 block">
                    <span className="mb-1.5 block text-xs font-semibold text-slate-600">
                      {division.classTeacher ? "Change class teacher" : "Assign class teacher"}
                    </span>
                    <select
                      className={inputClass}
                      value={values[division.id] || ""}
                      onChange={(event) =>
                        setValues((current) => ({
                          ...current,
                          [division.id]: event.target.value,
                        }))
                      }
                    >
                      <option value="">Select available teacher</option>
                      {data.eligibleClassTeachers.map((teacher) => (
                        <option key={teacher.id} value={teacher.id}>
                          {teacher.name} · {teacher.employeeCode}
                        </option>
                      ))}
                    </select>
                  </label>

                  {!data.eligibleClassTeachers.length && (
                    <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
                      No unassigned teachers are currently available in this department.
                    </p>
                  )}

                  <button
                    className="mt-3 inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400 disabled:shadow-none"
                    disabled={busy || !selectedTeacher}
                    onClick={() =>
                      void run(
                        () => api.assignClassTeacher(division.id, selectedTeacher!.id),
                        "Class teacher assigned",
                      )
                    }
                  >
                    {busy ? (
                      <RefreshCw className="h-4 w-4 animate-spin" />
                    ) : (
                      <UserCheck className="h-4 w-4" />
                    )}
                    {selectedTeacher
                      ? `Assign ${selectedTeacher.name}`
                      : division.classTeacher
                        ? "Select a teacher to change"
                        : "Select a teacher to assign"}
                  </button>
                </div>
              </article>
            );
          })}
        </div>

        {data.divisions.length > 0 && !visibleDivisions.length && (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50">
            <Empty
              title="No divisions found"
              text="Try changing the search text or assignment filter."
            />
          </div>
        )}

        {!data.divisions.length && (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50">
            <Empty
              title="No active divisions"
              text="Active divisions for your department will appear here."
            />
          </div>
        )}
      </div>
    </section>
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
        subtitle="Lecture load is calculated from the active weekly timetable."
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
                <span className="text-3xl font-bold">{t.weeklyLectures}</span>
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
