import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  CalendarCheck2,
  CheckCircle2,
  Clock3,
  RefreshCw,
  Search,
  UserCheck,
  Users,
  WifiOff,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import {
  getBusinessActivityDashboard,
  type BusinessActivityDashboard,
  type TeacherEngagementRow,
} from "@/features/audit/api";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { localDateString } from "@/lib/date";

type TeacherFilter = "ALL" | "NOT_LOGGED_IN" | "LOW_USAGE" | "ATTENDANCE_PENDING" | "ACTIVE";

const filterOptions: Array<{ value: TeacherFilter; label: string }> = [
  { value: "ALL", label: "All teachers" },
  { value: "NOT_LOGGED_IN", label: "Not logged in today" },
  { value: "LOW_USAGE", label: "Low ERP usage" },
  { value: "ATTENDANCE_PENDING", label: "Attendance pending" },
  { value: "ACTIVE", label: "Active today" },
];

export function AuditLogPage() {
  const { user } = useAuth();
  const [data, setData] = useState<BusinessActivityDashboard>();
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [department, setDepartment] = useState("");
  const [filter, setFilter] = useState<TeacherFilter>("ALL");

  const load = useCallback(() => {
    setLoading(true);
    const today = new Date();
    const from = new Date(today);
    from.setDate(today.getDate() - 29);
    getBusinessActivityDashboard({
      dateFrom: localDateString(from),
      dateTo: localDateString(today),
      size: 30,
    })
      .then(setData)
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => load(), [load]);

  const teachers = data?.teachers ?? [];
  const departments = useMemo(
    () => Array.from(new Set(teachers.map((teacher) => teacher.department))).sort(),
    [teachers],
  );
  const scopedTeachers = useMemo(
    () => (department ? teachers.filter((teacher) => teacher.department === department) : teachers),
    [department, teachers],
  );
  const visibleTeachers = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return scopedTeachers.filter((teacher) => {
      if (
        keyword &&
        !`${teacher.teacher} ${teacher.employeeCode} ${teacher.department} ${teacher.role}`
          .toLowerCase()
          .includes(keyword)
      )
        return false;
      if (filter === "NOT_LOGGED_IN" && teacher.loggedInToday) return false;
      if (filter === "LOW_USAGE" && !["LOW_USAGE", "INACTIVE_7_DAYS"].includes(teacher.usageStatus))
        return false;
      if (filter === "ATTENDANCE_PENDING" && teacher.attendanceRemaining === 0) return false;
      if (filter === "ACTIVE" && !teacher.loggedInToday) return false;
      return true;
    });
  }, [filter, query, scopedTeachers]);
  const attentionTeachers = scopedTeachers
    .filter(
      (teacher) =>
        !teacher.loggedInToday ||
        teacher.attendanceRemaining > 0 ||
        ["LOW_USAGE", "INACTIVE_7_DAYS"].includes(teacher.usageStatus),
    )
    .slice(0, 6);
  const attendanceTeachers = scopedTeachers
    .filter((teacher) => teacher.scheduledLectures > 0)
    .sort((a, b) => b.attendanceSubmitted - a.attendanceSubmitted)
    .slice(0, 6);

  if (loading && !data) {
    return (
      <div className="page-container">
        <Loader label="Loading teacher activity…" />
      </div>
    );
  }

  const summary = {
    totalTeachers: scopedTeachers.length,
    loggedInToday: scopedTeachers.filter((teacher) => teacher.loggedInToday).length,
    notLoggedInToday: scopedTeachers.filter((teacher) => !teacher.loggedInToday).length,
    scheduledLecturesToday: scopedTeachers.reduce(
      (total, teacher) => total + teacher.scheduledLectures,
      0,
    ),
    attendanceCompletedToday: scopedTeachers.reduce(
      (total, teacher) => total + teacher.attendanceSubmitted,
      0,
    ),
    attendanceRemainingToday: scopedTeachers.reduce(
      (total, teacher) => total + teacher.attendanceRemaining,
      0,
    ),
  };

  return (
    <div className="page-container space-y-6 pb-12">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
            Daily staff monitoring
          </p>
          <h1 className="page-title mt-1">Teacher Activity & ERP Usage</h1>
          <p className="mt-1 text-sm text-slate-500">
            See who logged in, who submitted attendance, and who may need support today.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            aria-label="Filter by department"
            value={department}
            onChange={(event) => setDepartment(event.target.value)}
            className="h-10 min-w-48 rounded-xl border border-blue-100 bg-white px-3 text-sm font-medium text-slate-700"
          >
            <option value="">All departments</option>
            {departments.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <span className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-800">
            {user?.collegeName ?? "Your college"}
          </span>
          <span className="rounded-xl border bg-white px-4 py-2 text-sm font-medium text-slate-600">
            {new Intl.DateTimeFormat("en-IN", {
              weekday: "short",
              day: "2-digit",
              month: "short",
              year: "numeric",
            }).format(new Date())}
          </span>
          <Button variant="secondary" onClick={load} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        <Metric icon={Users} label="Teaching Staff" value={summary.totalTeachers} tone="blue" />
        <Metric
          icon={UserCheck}
          label="Logged In Today"
          value={summary.loggedInToday}
          tone="green"
        />
        <Metric
          icon={WifiOff}
          label="Not Logged In"
          value={summary.notLoggedInToday}
          tone="orange"
        />
        <Metric
          icon={CalendarCheck2}
          label="Today’s Lectures"
          value={summary.scheduledLecturesToday}
          tone="blue"
        />
        <Metric
          icon={CheckCircle2}
          label="Attendance Done"
          value={summary.attendanceCompletedToday}
          tone="cyan"
        />
        <Metric
          icon={Clock3}
          label="Attendance Remaining"
          value={summary.attendanceRemainingToday}
          tone="red"
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <Card className="overflow-hidden">
          <PanelHeader
            icon={AlertTriangle}
            title="Needs attention"
            subtitle="Teachers not logged in, using ERP infrequently, or with scheduled attendance remaining"
            tone="amber"
          />
          <div className="divide-y">
            {attentionTeachers.map((teacher) => (
              <TeacherCompactRow key={teacher.staffId} teacher={teacher} attention />
            ))}
            {!attentionTeachers.length && (
              <EmptyState
                icon={CheckCircle2}
                title="Everything looks good"
                text="No teacher currently needs attention."
              />
            )}
          </div>
        </Card>

        <Card className="overflow-hidden">
          <PanelHeader
            icon={CalendarCheck2}
            title="Today’s attendance work"
            subtitle="Scheduled lectures and attendance completed by teachers today"
            tone="blue"
          />
          <div className="divide-y">
            {attendanceTeachers.map((teacher) => (
              <TeacherCompactRow key={teacher.staffId} teacher={teacher} />
            ))}
            {!attendanceTeachers.length && (
              <EmptyState
                icon={Clock3}
                title="No attendance activity yet"
                text="Today’s scheduled lectures will appear here."
              />
            )}
          </div>
        </Card>
      </div>

      <Card className="overflow-hidden">
        <div className="border-b p-5">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <h2 className="text-lg font-bold text-slate-950">Teacher usage monitor</h2>
              <p className="mt-1 text-sm text-slate-500">
                Clear daily status for every active HOD and teacher in the college.
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search teacher…"
                  className="h-10 w-full rounded-xl border border-slate-200 pl-9 pr-3 text-sm focus:border-blue-400 focus:outline-none"
                />
              </label>
              <select
                value={filter}
                onChange={(event) => setFilter(event.target.value as TeacherFilter)}
                className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
              >
                {filterOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[980px] text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-5 py-3">Teacher</th>
                <th className="px-5 py-3">Department</th>
                <th className="px-5 py-3">Today’s login</th>
                <th className="px-5 py-3">Last login</th>
                <th className="px-5 py-3">7-day usage</th>
                <th className="px-5 py-3">Attendance today</th>
                <th className="px-5 py-3">Usage status</th>
              </tr>
            </thead>
            <tbody>
              {visibleTeachers.map((teacher) => (
                <tr key={teacher.staffId} className="border-t hover:bg-blue-50/30">
                  <td className="px-5 py-4">
                    <p className="font-bold text-slate-900">{teacher.teacher}</p>
                    <p className="mt-0.5 text-xs text-slate-400">{teacher.employeeCode}</p>
                  </td>
                  <td className="px-5 py-4">
                    <p className="font-medium text-slate-700">{teacher.department}</p>
                    <p className="mt-0.5 text-xs text-slate-400">{teacher.role}</p>
                  </td>
                  <td className="px-5 py-4">
                    <Badge
                      label={teacher.loggedInToday ? "Logged in" : "Not logged in"}
                      tone={teacher.loggedInToday ? "green" : "red"}
                    />
                  </td>
                  <td className="whitespace-nowrap px-5 py-4 text-slate-600">
                    {formatDateTime(teacher.lastLoginAt)}
                  </td>
                  <td className="px-5 py-4">
                    <UsageDays days={teacher.loginDaysLast7} />
                  </td>
                  <td className="px-5 py-4">
                    <AttendanceStatus teacher={teacher} />
                  </td>
                  <td className="px-5 py-4">
                    <UsageStatus status={teacher.usageStatus} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!visibleTeachers.length && (
            <EmptyState
              icon={Search}
              title="No teachers found"
              text="Try changing the search or status filter."
            />
          )}
        </div>
        <div className="border-t bg-slate-50 px-5 py-3 text-xs text-slate-500">
          Remaining attendance is calculated as today’s scheduled timetable lectures minus submitted
          attendance. “No lectures today” means the teacher has no timetable entry for the selected
          department today.
        </div>
      </Card>
    </div>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: typeof Users;
  label: string;
  value: number;
  tone: "blue" | "green" | "orange" | "cyan" | "red" | "amber";
}) {
  const styles = {
    blue: "border-blue-100 bg-blue-50 text-blue-700",
    green: "border-emerald-100 bg-emerald-50 text-emerald-700",
    orange: "border-orange-100 bg-orange-50 text-orange-700",
    cyan: "border-cyan-100 bg-cyan-50 text-cyan-700",
    red: "border-rose-100 bg-rose-50 text-rose-700",
    amber: "border-amber-100 bg-amber-50 text-amber-700",
  };
  return (
    <Card className="p-4">
      <span className={`grid h-10 w-10 place-items-center rounded-xl border ${styles[tone]}`}>
        <Icon className="h-5 w-5" />
      </span>
      <p className="mt-3 text-2xl font-black text-slate-950">{value}</p>
      <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
    </Card>
  );
}

function PanelHeader({
  icon: Icon,
  title,
  subtitle,
  tone,
}: {
  icon: typeof Users;
  title: string;
  subtitle: string;
  tone: "amber" | "blue" | "slate";
}) {
  const styles = {
    amber: "bg-amber-50 text-amber-700",
    blue: "bg-blue-50 text-blue-700",
    slate: "bg-slate-50 text-slate-700",
  };
  return (
    <div className="flex items-start gap-3 border-b p-5">
      <span className={`grid h-10 w-10 shrink-0 place-items-center rounded-xl ${styles[tone]}`}>
        <Icon className="h-5 w-5" />
      </span>
      <div>
        <h2 className="font-bold text-slate-950">{title}</h2>
        <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>
      </div>
    </div>
  );
}

function TeacherCompactRow({
  teacher,
  attention = false,
}: {
  teacher: TeacherEngagementRow;
  attention?: boolean;
}) {
  return (
    <div className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="font-semibold text-slate-900">{teacher.teacher}</p>
        <p className="mt-0.5 text-xs text-slate-500">
          {teacher.department} · {teacher.role}
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {attention && !teacher.loggedInToday && <Badge label="Not logged in" tone="red" />}
        {attention && teacher.attendanceRemaining > 0 && (
          <Badge label={`${teacher.attendanceRemaining} attendance remaining`} tone="amber" />
        )}
        {!attention && <AttendanceStatus teacher={teacher} />}
        <span className="text-xs text-slate-400">{teacher.loginDaysLast7}/7 usage days</span>
      </div>
    </div>
  );
}

function AttendanceStatus({ teacher }: { teacher: TeacherEngagementRow }) {
  if (teacher.attendanceStatus === "COMPLETED")
    return (
      <Badge
        label={`${teacher.attendanceSubmitted}/${teacher.scheduledLectures} done`}
        tone="green"
      />
    );
  if (teacher.attendanceStatus === "PARTIAL")
    return (
      <Badge
        label={`${teacher.attendanceSubmitted} done · ${teacher.attendanceRemaining} remaining`}
        tone="amber"
      />
    );
  if (teacher.attendanceStatus === "PENDING")
    return <Badge label={`${teacher.attendanceRemaining} remaining`} tone="red" />;
  return <Badge label="No lectures today" tone="slate" />;
}

function UsageStatus({ status }: { status: TeacherEngagementRow["usageStatus"] }) {
  const values = {
    REGULAR: { label: "Regular user", tone: "green" as const },
    ACTIVE_TODAY: { label: "Active today", tone: "blue" as const },
    LOW_USAGE: { label: "Low usage", tone: "amber" as const },
    INACTIVE_7_DAYS: { label: "Inactive 7 days", tone: "red" as const },
  };
  return <Badge {...values[status]} />;
}

function UsageDays({ days }: { days: number }) {
  return (
    <div>
      <div className="flex gap-1" aria-label={`${days} of 7 days used`}>
        {Array.from({ length: 7 }, (_, index) => (
          <i
            key={index}
            className={`h-2.5 w-3 rounded-sm ${index < days ? "bg-blue-500" : "bg-slate-200"}`}
          />
        ))}
      </div>
      <p className="mt-1 text-xs text-slate-400">{days} of 7 days</p>
    </div>
  );
}

function Badge({
  label,
  tone,
}: {
  label: string;
  tone: "green" | "red" | "amber" | "blue" | "slate";
}) {
  const styles = {
    green: "bg-emerald-50 text-emerald-700 ring-emerald-100",
    red: "bg-rose-50 text-rose-700 ring-rose-100",
    amber: "bg-amber-50 text-amber-700 ring-amber-100",
    blue: "bg-blue-50 text-blue-700 ring-blue-100",
    slate: "bg-slate-100 text-slate-600 ring-slate-200",
  };
  return (
    <span
      className={`inline-flex w-fit whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${styles[tone]}`}
    >
      {label}
    </span>
  );
}

function EmptyState({
  icon: Icon,
  title,
  text,
}: {
  icon: typeof Users;
  title: string;
  text: string;
}) {
  return (
    <div className="px-5 py-10 text-center">
      <Icon className="mx-auto h-7 w-7 text-slate-300" />
      <p className="mt-3 font-semibold text-slate-700">{title}</p>
      <p className="mt-1 text-xs text-slate-400">{text}</p>
    </div>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "Never logged in";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
