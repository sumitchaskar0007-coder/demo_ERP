import {
  Activity,
  ArrowRight,
  Building2,
  CalendarDays,
  CheckCircle2,
  Clock3,
  GraduationCap,
  LibraryBig,
  Sparkles,
  Users,
} from "lucide-react";
import { useEffect, useState, type ComponentType } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { DonutChart } from "@/components/common/DonutChart";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import * as api from "@/features/dashboard/api";
import { ROLES, ROUTES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { TeacherDashboardTimetable } from "@/features/teacherTimetable/TeacherDashboardTimetable";
import { TeacherAttendanceWidget } from "@/features/attendance/TeacherAttendanceWidget";

const statStyles: Array<{
  icon: ComponentType<{ className?: string }>;
  color: string;
  accent: string;
  chart: string;
}> = [
  { icon: Building2, color: "bg-blue-50 text-blue-600", accent: "bg-blue-500", chart: "#3b82f6" },
  { icon: Users, color: "bg-violet-50 text-violet-600", accent: "bg-violet-500", chart: "#8b5cf6" },
  {
    icon: LibraryBig,
    color: "bg-amber-50 text-amber-600",
    accent: "bg-amber-500",
    chart: "#f59e0b",
  },
  {
    icon: CheckCircle2,
    color: "bg-emerald-50 text-emerald-600",
    accent: "bg-emerald-500",
    chart: "#22c55e",
  },
  { icon: Activity, color: "bg-cyan-50 text-cyan-600", accent: "bg-cyan-500", chart: "#06b6d4" },
  { icon: Sparkles, color: "bg-rose-50 text-rose-600", accent: "bg-rose-500", chart: "#f43f5e" },
];

const principalStatConfig: Record<string, { label: string; to: string; helper: string }> = {
  totalClasses: {
    label: "Total Classes",
    to: ROUTES.academicClasses,
    helper: "View academic classes",
  },
  totalStaff: { label: "Total Staff", to: ROUTES.staff, helper: "View college staff" },
  totalTeachingStaff: {
    label: "Teaching Staff",
    to: ROUTES.staff,
    helper: "View teachers and staff",
  },
  totalStudents: { label: "Total Students", to: ROUTES.studentReport, helper: "View student list" },
  totalFeePending: {
    label: "Fee Pending",
    to: ROUTES.feeAccounts,
    helper: "View outstanding accounts",
  },
  totalSections: {
    label: "Total Sections",
    to: ROUTES.academicSections,
    helper: "View academic sections",
  },
  totalSubjects: { label: "Total Subjects", to: ROUTES.academicSubjects, helper: "View subjects" },
  totalDepartments: {
    label: "Total Departments",
    to: ROUTES.departments,
    helper: "View departments",
  },
  totalFeeCollected: {
    label: "Fee Collected",
    to: ROUTES.feePayments,
    helper: "View payment records",
  },
  rejectedAdmissions: {
    label: "Rejected Admissions",
    to: `${ROUTES.admissionReport}?status=PRINCIPAL_REJECTED`,
    helper: "View rejected applications",
  },
  pendingAdmissions: {
    label: "Pending Admissions",
    to: `${ROUTES.admissionReport}?status=PRINCIPAL_REVIEW_PENDING`,
    helper: "View pending applications",
  },
  approvedAdmissions: {
    label: "Approved Admissions",
    to: `${ROUTES.admissionReport}?status=PRINCIPAL_APPROVED`,
    helper: "View approved applications",
  },
};

const principalStatOrder = [
  "totalDepartments",
  "totalClasses",
  "totalSections",
  "totalSubjects",
  "totalTeachingStaff",
  "totalStaff",
  "totalStudents",
  "pendingAdmissions",
  "approvedAdmissions",
  "rejectedAdmissions",
  "totalFeeCollected",
  "totalFeePending",
];

export function RoleDashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<api.DashboardData | null>(null);

  useEffect(() => {
    if (!user) return;
    const roles = user.roles;
    const request = roles.includes(ROLES.PRINCIPAL)
      ? api.getPrincipalDashboard()
      : roles.includes(ROLES.STUDENT_SECTION)
        ? api.getStudentSectionDashboard()
        : roles.includes(ROLES.FEE_SECTION)
          ? api.getFeeSectionDashboard()
          : roles.includes(ROLES.HOD)
            ? api.getHodDashboard()
            : roles.includes(ROLES.CLASS_TEACHER) || roles.includes(ROLES.SUBJECT_TEACHER)
              ? api.getTeacherDashboard()
              : api.getStudentDashboard();
    request.then(setData).catch((error) => toast.error(handleApiError(error).message));
  }, [user]);

  if (!data || !user) return <Loader label="Loading dashboard..." />;

  const principal = user.roles.includes(ROLES.PRINCIPAL);

  const toNumber = (v: unknown) => {
    if (typeof v === "number" && Number.isFinite(v)) return v;
    if (typeof v === "string") {
      const n = Number(v);
      if (Number.isFinite(n)) return n;
    }
    return 0;
  };

  const stats = Object.entries(data)
    .filter(([key, value]) => typeof value !== "object" && key !== "collegeId")
    .map(([key, value], index) => {
      const numericValue = toNumber(value);
      const config = principal ? principalStatConfig[key] : undefined;
      const money = key === "totalFeeCollected" || key === "totalFeePending";
      return {
        key,
        label: config?.label ?? key.replace(/([A-Z])/g, " $1").trim(),
        value: money
          ? `₹${numericValue.toLocaleString("en-IN")}`
          : numericValue.toLocaleString("en-IN"),
        numericValue,
        to: config?.to,
        helper: config?.helper,
        ...statStyles[index % statStyles.length],
      };
    })
    .sort((a, b) =>
      principal ? principalStatOrder.indexOf(a.key) - principalStatOrder.indexOf(b.key) : 0,
    );
  const primaryRole = user.roles[0]?.replaceAll("_", " ") || "ERP";
  const overviewStats = principal
    ? stats.filter(({ key }) =>
        ["pendingAdmissions", "approvedAdmissions", "rejectedAdmissions"].includes(key),
      )
    : stats.filter(({ key }) => !["totalFeeCollected", "totalFeePending"].includes(key));
  const teacher =
    user.roles.includes(ROLES.CLASS_TEACHER) || user.roles.includes(ROLES.SUBJECT_TEACHER);
  const quickActions = principal
    ? [
        {
          label: "Departments",
          detail: "Manage academic departments",
          to: ROUTES.departments,
          icon: LibraryBig,
        },
        {
          label: "Create Course Year",
          detail: "Set up the academic year",
          to: ROUTES.createCourseYear,
          icon: GraduationCap,
        },
        {
          label: "Create Staff",
          detail: "Add a staff account",
          to: ROUTES.createStaff,
          icon: Users,
        },
      ]
    : teacher
      ? [
          {
            label: "My Timetable",
            detail: "View your weekly teaching schedule",
            to: ROUTES.teacherTimetable,
            icon: CalendarDays,
          },
          {
            label: "Take Attendance",
            detail: "Mark your current scheduled lecture",
            to: ROUTES.teacherAttendance,
            icon: CheckCircle2,
          },
          {
            label: "Open Profile",
            detail: "Review your account details",
            to: ROUTES.profile,
            icon: Users,
          },
        ]
      : [
          {
            label: "Open Profile",
            detail: "Review your account details",
            to: ROUTES.profile,
            icon: Users,
          },
          {
            label: "Dashboard",
            detail: "Refresh your role overview",
            to: ROUTES.dashboard,
            icon: Activity,
          },
        ];

  return (
    <div className="page-container pb-10">
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold text-slate-400">
            Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;{primaryRole}
          </p>
          <h1 className="mt-2 text-2xl font-bold text-slate-900">{primaryRole} Dashboard</h1>
          <p className="mt-1 text-sm text-slate-500">Overview of your current ERP workspace.</p>
        </div>
        <p className="inline-flex items-center gap-2 text-xs font-medium text-slate-400">
          <Clock3 className="h-4 w-4" /> Updated just now
        </p>
      </div>

      <section className="erp-welcome-banner px-5 py-6 sm:px-9 sm:py-7">
        <div className="absolute -right-10 -top-20 hidden h-64 w-64 rounded-full border-[30px] border-blue-500/30 sm:block" />
        <div className="absolute right-52 top-5 hidden h-10 w-10 rotate-45 rounded-lg border-4 border-amber-400/80 sm:block" />
        <div className="relative flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div className="min-w-0">
            <p className="text-xs leading-5 text-blue-100 sm:text-sm">
              {user.collegeName || "College ERP Workspace"}
            </p>
            <h2 className="mt-2 text-2xl font-bold leading-tight sm:text-3xl">
              Welcome back, {user.fullName.split(" ")[0]}
            </h2>
            <p className="mt-2 max-w-md text-sm leading-6 text-blue-100">
              Have a productive day managing your education workspace.
            </p>
          </div>
          <div className="hidden rounded-2xl border border-white/10 bg-white/10 px-5 py-4 text-right backdrop-blur sm:block">
            <p className="text-xs text-blue-200">Account status</p>
            <p className="mt-1 font-bold">{user.status}</p>
          </div>
        </div>
      </section>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map(({ key, label, value, icon: Icon, color, accent, to, helper }) => {
          const content = (
            <Card
              className={`relative h-full overflow-hidden p-5 transition duration-200 ${to ? "group-hover:-translate-y-0.5 group-hover:border-brand-200 group-hover:shadow-lg" : ""}`}
            >
              <span className={`absolute inset-x-0 top-0 h-1 ${accent}`} />
              <div className="flex items-center justify-between">
                <div className={`grid h-11 w-11 place-items-center rounded-xl ${color}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <span className="rounded-full bg-emerald-50 px-2 py-1 text-[10px] font-bold text-emerald-600">
                  Live
                </span>
              </div>
              <p className="mt-4 text-2xl font-bold text-slate-900">{value}</p>
              <p className="mt-1 text-sm font-semibold text-slate-600">{label}</p>
              {to && (
                <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs font-semibold text-brand-600">
                  <span>{helper}</span>
                  <ArrowRight className="h-4 w-4 transition group-hover:translate-x-1" />
                </div>
              )}
            </Card>
          );
          return to ? (
            <Link
              key={key}
              to={to}
              className="group rounded-2xl focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              {content}
            </Link>
          ) : (
            <div key={key}>{content}</div>
          );
        })}
      </div>

      {teacher && <TeacherDashboardTimetable />}
      {teacher && <TeacherAttendanceWidget />}

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <Card className="overflow-hidden">
          <div className="erp-panel-header">
            <div>
              <h2 className="font-bold">
                {principal ? "Admission overview" : "Workspace overview"}
              </h2>
              <p className="mt-1 text-xs text-slate-500">
                {principal ? "Principal review status distribution" : "Live role-specific data"}
              </p>
            </div>
            <Activity className="h-5 w-5 text-brand-600" />
          </div>
          <div className="flex min-h-[300px] items-center justify-center p-6">
            <DonutChart
              centerLabel={principal ? "Admissions" : "ERP Records"}
              segments={overviewStats.map(({ label, numericValue, chart }) => ({
                label,
                value: numericValue,
                color: chart,
              }))}
            />
          </div>
        </Card>

        <Card className="overflow-hidden">
          <div className="border-b px-6 py-5">
            <h2 className="font-bold">Quick actions</h2>
            <p className="mt-1 text-xs text-slate-500">Frequently used tools for your role</p>
          </div>
          <div className="space-y-2 p-4">
            {quickActions.map(({ label, detail, to, icon: Icon }) => (
              <Link
                key={label}
                to={to}
                className="group flex items-center gap-3 rounded-xl p-3 transition hover:bg-slate-50"
              >
                <div className="grid h-11 w-11 place-items-center rounded-xl bg-blue-50 text-brand-600">
                  <Icon className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{label}</p>
                  <p className="truncate text-xs text-slate-400">{detail}</p>
                </div>
                <ArrowRight className="h-4 w-4 text-slate-300 group-hover:text-brand-600" />
              </Link>
            ))}
          </div>
        </Card>
      </div>

      {principal && (
        <Card className="mt-6 p-5">
          <h2 className="font-bold">Principal setup workflow</h2>
          <p className="mt-1 text-sm text-slate-500">
            Department - Course Year - Division - Staff - Class Teacher
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <Link to={ROUTES.departments}>
              <Button variant="secondary">Create Department</Button>
            </Link>
            <Link to={ROUTES.createCourseYear}>
              <Button variant="secondary">Create Course Year</Button>
            </Link>
            <Link to={ROUTES.createDivision}>
              <Button variant="secondary">Create Division</Button>
            </Link>
            <Link to={ROUTES.createStaff}>
              <Button variant="secondary">Create Staff</Button>
            </Link>
            <Link to={ROUTES.divisions}>
              <Button>Assign Class Teacher</Button>
            </Link>
          </div>
        </Card>
      )}
    </div>
  );
}
