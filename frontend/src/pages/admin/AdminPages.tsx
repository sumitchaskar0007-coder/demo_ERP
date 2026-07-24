import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  ArrowRight,
  Building2,
  CalendarDays,
  CheckCircle2,
  GraduationCap,
  Grid3X3,
  Hash,
  IndianRupee,
  List,
  QrCode,
  Search,
  UserRound,
  Users,
  WalletCards,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { PaymentQrManager } from "@/components/colleges/PaymentQrManager";
import { Select } from "@/components/common/Select";
import { Pagination } from "@/components/common/Pagination";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import { useAuth } from "@/features/auth/authStore";
import * as api from "@/features/admin/api";
import { getActiveColleges, getAllColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { getActiveDepartmentsForAdmin } from "@/features/departments/api";
import type { Department } from "@/features/departments/types";
import type { FeeStructureResponse } from "@/features/fees/types";
import type { PageResponse } from "@/types/api";
import { weeklyTimetableApi, type WeeklyDivision } from "@/features/academics/api";
import type { TeacherDay, TeacherLecture, TeacherTimetable } from "@/features/teacherTimetable/api";
import {
  TeacherDayView,
  TeacherTimetableStat,
  TeacherWeeklyGrid,
} from "@/features/teacherTimetable/TeacherTimetablePage";
const categories = ["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"].map((v) => ({
  label: v,
  value: v,
}));
export function AdminDashboardPage({ principal = false }: { principal?: boolean }) {
  const { user } = useAuth();
  const [d, setD] = useState<api.AdminAnalytics | null>(null);
  const [colleges, setColleges] = useState<College[]>([]);
  const [collegeId, setCollegeId] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setD(null);
    api[principal ? "getPrincipalAnalytics" : "getAdminAnalytics"](
      principal ? {} : { collegeId: collegeId || undefined },
      controller.signal,
    )
      .then(setD)
      .catch((error) => {
        if (!controller.signal.aborted) toast.error(handleApiError(error).message);
      });
    return () => controller.abort();
  }, [collegeId, principal]);
  useEffect(() => {
    if (principal) return;
    getActiveColleges()
      .then(setColleges)
      .catch(() => setColleges([]));
  }, [principal]);

  if (!d)
    return <Loader label={`Preparing ${principal ? "Principal" : "Super Admin"} dashboard...`} />;

  const organizationMetrics = principal
    ? [
        {
          key: "totalStaff",
          label: "College Staff",
          value: d.summary.totalStaff,
          icon: Users,
          color: "bg-blue-50 text-blue-600",
          accent: "bg-blue-500",
        },
        {
          key: "totalStudents",
          label: "College Students",
          value: d.summary.totalStudents,
          icon: GraduationCap,
          color: "bg-cyan-50 text-cyan-600",
          accent: "bg-cyan-500",
        },
        {
          key: "totalFeeCollection",
          label: "Fees Collected",
          value: `₹${d.summary.totalFeeCollection.toLocaleString("en-IN")}`,
          icon: WalletCards,
          color: "bg-emerald-50 text-emerald-600",
          accent: "bg-emerald-500",
        },
        {
          key: "pendingFee",
          label: "Pending Fees",
          value: `₹${d.summary.pendingFee.toLocaleString("en-IN")}`,
          icon: Activity,
          color: "bg-orange-50 text-orange-600",
          accent: "bg-orange-500",
        },
      ]
    : [
        {
          key: "totalStaff",
          label: "Total Staff",
          value: d.summary.totalStaff,
          icon: Users,
          color: "bg-blue-50 text-blue-600",
          accent: "bg-blue-500",
        },
        {
          key: "totalPrincipals",
          label: "Total Principals",
          value: d.summary.totalPrincipals,
          icon: UserRound,
          color: "bg-violet-50 text-violet-600",
          accent: "bg-violet-500",
        },
        {
          key: "totalColleges",
          label: "Total Colleges",
          value: d.summary.totalColleges,
          icon: Building2,
          color: "bg-amber-50 text-amber-600",
          accent: "bg-amber-500",
        },
        {
          key: "activeColleges",
          label: "Active Colleges",
          value: d.summary.activeColleges,
          icon: CheckCircle2,
          color: "bg-emerald-50 text-emerald-600",
          accent: "bg-emerald-500",
        },
        {
          key: "totalStudents",
          label: "Total Students",
          value: d.summary.totalStudents,
          icon: GraduationCap,
          color: "bg-cyan-50 text-cyan-600",
          accent: "bg-cyan-500",
        },
      ];
  const feeTotal = d.summary.totalFeeCollection + d.summary.pendingFee;
  const collectionRate =
    feeTotal > 0 ? Math.round((d.summary.totalFeeCollection / feeTotal) * 100) : 0;

  return (
    <div className="page-container pb-10">
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          {principal && (
            <p className="text-xs font-semibold text-slate-400">
              Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Principal
            </p>
          )}
          <h1 className={`${principal ? "mt-2 " : ""}text-2xl font-bold`}>
            {principal ? "Principal" : "Super Admin"} Dashboard
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {principal
              ? "College academics, students, and finance overview."
              : "Global college, student, and fee overview."}
          </p>
        </div>
        {!principal && (
          <div className="w-full sm:w-72">
            <Select
              aria-label="Dashboard college"
              options={[
                { label: "All colleges", value: "" },
                ...colleges.map((c) => ({ label: c.name, value: c.id })),
              ]}
              value={collegeId}
              onChange={(event) => setCollegeId(event.target.value)}
            />
          </div>
        )}
      </div>

      <section className="erp-welcome-banner px-7 py-7 sm:px-9">
        <div className="absolute -right-10 -top-20 h-64 w-64 rounded-full border-[30px] border-blue-500/30" />
        <div className="absolute right-52 top-5 h-10 w-10 rotate-45 rounded-lg border-4 border-amber-400/80" />
        <div className="relative flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div>
            <p className="text-sm text-blue-100">
              {principal ? user?.collegeName : "Jadhavar ERP Administration"}
            </p>
            <h2 className="mt-2 text-3xl font-bold">
              Welcome back, {user?.fullName?.split(" ")[0] || "Admin"}
            </h2>
            <p className="mt-2 text-sm text-blue-100">
              Have a productive day managing your education workspace.
            </p>
          </div>
        </div>
      </section>

      <section className="mt-6">
        <div className="mb-4">
          <h2 className="text-lg font-bold">Organization Overview</h2>
        </div>
        <div
          className={`grid gap-4 sm:grid-cols-2 ${principal ? "xl:grid-cols-4" : "xl:grid-cols-5"}`}
        >
          {organizationMetrics.map(({ key, label, value, icon: Icon, color, accent }) => (
            <Card className="relative overflow-hidden p-5" key={key}>
              <span className={`absolute inset-x-0 top-0 h-1 ${accent}`} />
              <div className="flex items-center justify-between">
                <div className={`grid h-11 w-11 place-items-center rounded-xl ${color}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <span className="rounded-full bg-emerald-50 px-2 py-1 text-[10px] font-bold text-emerald-600">
                  Live
                </span>
              </div>
              <p className="mt-4 text-2xl font-bold">{value}</p>
              <p className="mt-1 text-xs font-medium text-slate-500">{label}</p>
            </Card>
          ))}
        </div>
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <Card className="overflow-hidden p-6">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="font-bold">Financial Overview</h2>
              <p className="mt-1 text-xs text-slate-500">
                Collected and pending fees from student accounts
              </p>
            </div>
            <WalletCards className="h-5 w-5 text-emerald-600" />
          </div>
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-emerald-50 p-5">
              <p className="text-xs font-bold uppercase tracking-wider text-emerald-700">
                Total Fee Collection
              </p>
              <p className="mt-3 text-3xl font-black text-emerald-900">
                ₹{d.summary.totalFeeCollection.toLocaleString("en-IN")}
              </p>
              <p className="mt-2 text-xs text-emerald-700">Verified amount received</p>
            </div>
            <div className="rounded-2xl bg-orange-50 p-5">
              <p className="text-xs font-bold uppercase tracking-wider text-orange-700">
                Pending Fee
              </p>
              <p className="mt-3 text-3xl font-black text-orange-900">
                ₹{d.summary.pendingFee.toLocaleString("en-IN")}
              </p>
              <p className="mt-2 text-xs text-orange-700">Outstanding student balance</p>
            </div>
          </div>
          <div className="mt-6">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-600">Collection progress</span>
              <b className="text-emerald-600">{collectionRate}%</b>
            </div>
            <div className="mt-2 h-3 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-gradient-to-r from-blue-600 to-emerald-500 transition-all"
                style={{ width: `${collectionRate}%` }}
              />
            </div>
          </div>
        </Card>
        <Card className="overflow-hidden">
          <div className="border-b px-6 py-5">
            <h2 className="font-bold">Quick actions</h2>
            <p className="mt-1 text-xs text-slate-500">Frequently used administration tools</p>
          </div>
          <div className="space-y-2 p-4">
            {(principal
              ? [
                  {
                    label: "Departments",
                    detail: "Manage college departments",
                    to: ROUTES.departments,
                    icon: Building2,
                  },
                  {
                    label: "Fee Structures",
                    detail: "Configure college fee structures",
                    to: ROUTES.feeStructures,
                    icon: WalletCards,
                  },
                  {
                    label: "Pending Fees",
                    detail: "Review outstanding student balances",
                    to: ROUTES.principalPendingFees,
                    icon: Activity,
                  },
                ]
              : [
                  {
                    label: "Create College",
                    detail: "Add a new college workspace",
                    to: "/colleges/create",
                    icon: Building2,
                  },
                  {
                    label: "Create Principal",
                    detail: "Add a principal account",
                    to: "/principals/create",
                    icon: Users,
                  },
                  {
                    label: "Set up Fees",
                    detail: "Configure college fee structures",
                    to: ROUTES.adminFeeSetup,
                    icon: WalletCards,
                  },
                ]
            ).map(({ label, detail, to, icon: Icon }) => (
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

      <DashboardAnalytics analytics={d} principal={principal} />
    </div>
  );
}
function DashboardAnalytics({
  analytics,
  principal = false,
}: {
  analytics: api.AdminAnalytics;
  principal?: boolean;
}) {
  const chartColors = ["#2563eb", "#14b8a6", "#f59e0b", "#8b5cf6", "#f43f5e"];
  const feeCollection = analytics.collegeWiseFeeCollection
    .map((item) => ({ ...item, value: Number(item.value) || 0 }))
    .filter((item) => item.value > 0);
  const feeCollectionTrend = analytics.feeCollectionTrend
    .map((item) => ({ ...item, value: Number(item.value) || 0 }))
    .filter((item) => item.value > 0);
  const admissions = Object.entries(analytics.admissionStatusDistribution)
    .map(([label, value], index) => ({
      label: label
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase()),
      value: Number(value) || 0,
      color: chartColors[index % chartColors.length],
    }))
    .filter((item) => item.value > 0);
  const academics = (principal ? analytics.departmentWiseStudents : analytics.collegeWiseStudents)
    .map((item) => ({ ...item, value: Number(item.value) || 0 }))
    .filter((item) => item.value > 0);
  const staffCards = [
    {
      label: "Total Staff",
      value: analytics.summary.totalStaff ?? 0,
      icon: Users,
      tone: "bg-blue-50 text-blue-600",
    },
    {
      label: "Principals",
      value: analytics.summary.totalPrincipals ?? 0,
      icon: UserRound,
      tone: "bg-violet-50 text-violet-600",
    },
    {
      label: "Students",
      value: analytics.summary.totalStudents ?? 0,
      icon: GraduationCap,
      tone: "bg-amber-50 text-amber-600",
    },
    {
      label: "Active Colleges",
      value: analytics.summary.activeColleges ?? 0,
      icon: Building2,
      tone: "bg-emerald-50 text-emerald-600",
    },
  ];
  const feeBalance = [
    {
      label: "Collected",
      value: Number(analytics.summary.totalFeeCollection) || 0,
      color: "#10b981",
    },
    { label: "Pending", value: Number(analytics.summary.pendingFee) || 0, color: "#f59e0b" },
  ];

  return (
    <section className="mt-6 space-y-6">
      <div className="grid gap-6 xl:grid-cols-2">
        <AnalyticsCard
          title={principal ? "College Fee Collection Trend" : "Fee Collection"}
          subtitle={
            principal
              ? "Monthly verified collections for your college"
              : "College-wise verified collection"
          }
          contentClassName="h-[380px]"
        >
          {principal && feeCollectionTrend.length ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={feeCollectionTrend}
                margin={{ top: 16, right: 16, left: 4, bottom: 4 }}
              >
                <defs>
                  <linearGradient id="principalFeeGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.32} />
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 12 }} />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => `₹${Number(value).toLocaleString("en-IN")}`}
                  width={82}
                />
                <Tooltip
                  formatter={(value) => [
                    `₹${Number(value ?? 0).toLocaleString("en-IN")}`,
                    "Collected",
                  ]}
                  contentStyle={{ borderRadius: 14, borderColor: "#dbeafe" }}
                />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#2563eb"
                  strokeWidth={3}
                  fill="url(#principalFeeGradient)"
                  activeDot={{ r: 6, fill: "#2563eb", stroke: "white", strokeWidth: 3 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : !principal && feeCollection.length ? (
            <CollegeBarList
              items={feeCollection}
              barClassName="bg-blue-600"
              valueLabel="Collection"
              currency
            />
          ) : (
            <ChartEmpty message="No verified fee collections yet" />
          )}
        </AnalyticsCard>

        <AnalyticsCard title="Admissions" subtitle="Application status distribution">
          {admissions.length ? (
            <div className="flex h-full flex-col items-center gap-3 sm:flex-row">
              <div className="h-full min-h-52 flex-1">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={admissions}
                      dataKey="value"
                      nameKey="label"
                      innerRadius={58}
                      outerRadius={82}
                      paddingAngle={3}
                      stroke="none"
                    >
                      {admissions.map((item) => (
                        <Cell key={item.label} fill={item.color} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => Number(value ?? 0).toLocaleString("en-IN")} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <ChartLegend items={admissions} />
            </div>
          ) : (
            <ChartEmpty message="No admission records yet" />
          )}
        </AnalyticsCard>

        <AnalyticsCard
          title="Academics"
          subtitle={
            principal
              ? "Students in your college grouped by department"
              : "Students allocated across colleges"
          }
          contentClassName="h-[380px]"
        >
          {principal && academics.length ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={academics} margin={{ top: 16, right: 16, left: 4, bottom: 4 }}>
                <defs>
                  <linearGradient id="principalStudentGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#14b8a6" stopOpacity={1} />
                    <stop offset="95%" stopColor="#0d9488" stopOpacity={0.72} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 4" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 12 }} />
                <YAxis
                  allowDecimals={false}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 12 }}
                  width={44}
                />
                <Tooltip
                  formatter={(value) => [Number(value ?? 0).toLocaleString("en-IN"), "Students"]}
                  contentStyle={{ borderRadius: 14, borderColor: "#ccfbf1" }}
                />
                <Bar
                  dataKey="value"
                  fill="url(#principalStudentGradient)"
                  radius={[10, 10, 0, 0]}
                  maxBarSize={64}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : !principal && academics.length ? (
            <CollegeBarList
              items={academics}
              barClassName="bg-teal-500"
              valueLabel="Students"
              categoryLabel={principal ? "Department" : "College"}
            />
          ) : (
            <ChartEmpty message="No students allocated yet" />
          )}
        </AnalyticsCard>

        <AnalyticsCard title="Fee Balance" subtitle="Collected and pending student fees">
          {feeBalance.some((item) => item.value > 0) ? (
            <div className="flex h-full flex-col items-center gap-3 sm:flex-row">
              <div className="h-full min-h-52 flex-1">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={feeBalance}
                      dataKey="value"
                      nameKey="label"
                      innerRadius={58}
                      outerRadius={82}
                      paddingAngle={3}
                      stroke="none"
                    >
                      {feeBalance.map((item) => (
                        <Cell key={item.label} fill={item.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value) => `₹${Number(value ?? 0).toLocaleString("en-IN")}`}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <ChartLegend items={feeBalance} currency />
            </div>
          ) : (
            <ChartEmpty message="No fee accounts created yet" />
          )}
        </AnalyticsCard>
      </div>

      {!principal && (
        <div>
          <Card className="p-6">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="font-bold">Staff Overview</h2>
                <p className="mt-1 text-xs text-slate-500">Allocated people and institutions</p>
              </div>
              <CheckCircle2 className="h-5 w-5 text-emerald-600" />
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              {staffCards.map(({ label, value, icon: Icon, tone }) => (
                <div key={label} className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
                  <div className={`grid h-10 w-10 place-items-center rounded-xl ${tone}`}>
                    <Icon className="h-5 w-5" />
                  </div>
                  <p className="mt-4 text-2xl font-black">{value}</p>
                  <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}
    </section>
  );
}

function ChartLegend({
  items,
  currency = false,
}: {
  items: { label: string; value: number; color: string }[];
  currency?: boolean;
}) {
  return (
    <div className="w-full space-y-2 sm:w-48">
      {items.slice(0, 6).map((item) => (
        <div key={item.label} className="flex items-center gap-2 text-xs">
          <span
            className="h-2.5 w-2.5 shrink-0 rounded-full"
            style={{ backgroundColor: item.color }}
          />
          <span className="min-w-0 flex-1 truncate text-slate-500" title={item.label}>
            {item.label}
          </span>
          <b>
            {currency
              ? `₹${item.value.toLocaleString("en-IN")}`
              : item.value.toLocaleString("en-IN")}
          </b>
        </div>
      ))}
    </div>
  );
}

function CollegeBarList({
  items,
  barClassName,
  valueLabel,
  currency = false,
  categoryLabel = "College",
}: {
  items: { label: string; value: number }[];
  barClassName: string;
  valueLabel: string;
  currency?: boolean;
  categoryLabel?: string;
}) {
  const maximum = Math.max(...items.map((item) => item.value), 1);
  const formatValue = (value: number) =>
    currency ? `₹${value.toLocaleString("en-IN")}` : value.toLocaleString("en-IN");

  return (
    <div className="dashboard-chart-scroll h-full overflow-auto pr-1">
      <div
        className="college-bar-chart"
        role="img"
        aria-label={`${categoryLabel}-wise ${valueLabel}`}
      >
        <div className="college-bar-chart__header" aria-hidden="true">
          <span>{categoryLabel} name</span>
          <span>{valueLabel}</span>
          <span className="text-right">Total</span>
        </div>
        {items.map((item) => (
          <div className="college-bar-chart__row" key={item.label}>
            <span className="college-bar-chart__name" title={item.label}>
              {item.label}
            </span>
            <span className="college-bar-chart__track" aria-hidden="true">
              <span
                className={`college-bar-chart__fill ${barClassName}`}
                style={{ width: `${Math.max((item.value / maximum) * 100, 2)}%` }}
              />
            </span>
            <strong className="college-bar-chart__value">{formatValue(item.value)}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function AnalyticsCard({
  title,
  subtitle,
  children,
  contentClassName = "h-64",
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  contentClassName?: string;
}) {
  return (
    <Card className="overflow-hidden p-5 sm:p-6">
      <h2 className="text-base font-bold tracking-tight text-slate-900">{title}</h2>
      <p className="mt-1 text-xs leading-5 text-slate-500">{subtitle}</p>
      <div className={`mt-4 ${contentClassName}`}>{children}</div>
    </Card>
  );
}

function ChartEmpty({ message }: { message: string }) {
  return (
    <div className="grid h-full place-items-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/70 text-center">
      <div>
        <Activity className="mx-auto h-7 w-7 text-slate-300" />
        <p className="mt-3 text-sm font-semibold text-slate-500">{message}</p>
        <p className="mt-1 text-xs text-slate-400">
          This chart updates automatically from the database.
        </p>
      </div>
    </div>
  );
}
export function AdminFeeSetupPage() {
  const [rows, setRows] = useState<FeeStructureResponse[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [feeDivisions, setFeeDivisions] = useState<WeeklyDivision[]>([]);
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
    courseYear: "",
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
    weeklyTimetableApi
      .divisions()
      .then(setFeeDivisions)
      .catch(() => setFeeDivisions([]));
  }, []);

  useEffect(() => {
    void load(0);
  }, [filter]);

  const selectFilterCollege = async (collegeId: string) => {
    setFilter({ ...filter, collegeId, departmentId: "" });
    setFilterDepartments(collegeId ? await getActiveDepartmentsForAdmin(Number(collegeId)) : []);
  };

  const selectCollege = async (collegeId: string) => {
    setV((current) => ({ ...current, collegeId, departmentId: "", courseYear: "" }));
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
    if (!v.courseYear) {
      toast.error("Please select a course year");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        collegeId: Number(v.collegeId),
        departmentId: Number(v.departmentId),
        academicYear: v.academicYear,
        courseYear: v.courseYear,
        studentCategory: v.studentCategory,
        title: v.title || `${v.courseYear} ${v.studentCategory} Fee`,
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
            onChange={(event) => setV({ ...v, departmentId: event.target.value, courseYear: "" })}
          />
          <Select
            label="Course Year"
            value={v.courseYear}
            disabled={!v.departmentId}
            onChange={(event) => setV({ ...v, courseYear: event.target.value })}
            options={[
              {
                label: v.departmentId ? "Select course year" : "Select department first",
                value: "",
              },
              ...Array.from(
                new Set(
                  feeDivisions
                    .filter((item) => item.departmentId === Number(v.departmentId))
                    .map((item) => item.year),
                ),
              ).map((year) => ({ label: year, value: year })),
            ]}
          />
          <Input label="Academic Year" value="2026-27" readOnly disabled />
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
          <Select
            label="Filter by college"
            options={[
              { label: "All colleges", value: "" },
              ...colleges.map((c) => ({ label: c.name, value: c.id })),
            ]}
            value={filter.collegeId}
            onChange={(e) => void selectFilterCollege(e.target.value)}
          />
          <Select
            label="Filter by department"
            options={[
              { label: filter.collegeId ? "All departments" : "Select college first", value: "" },
              ...filterDepartments.map((d) => ({ label: d.name, value: d.id })),
            ]}
            value={filter.departmentId}
            disabled={!filter.collegeId}
            onChange={(e) => setFilter({ ...filter, departmentId: e.target.value })}
          />
          <Select
            label="Filter by caste/category"
            options={[{ label: "All categories", value: "" }, ...categories]}
            value={filter.studentCategory}
            onChange={(e) => setFilter({ ...filter, studentCategory: e.target.value })}
          />
        </div>
        <div className="mt-3 space-y-2">
          {rows.map((r) => (
            <div
              className="flex items-center justify-between gap-3 rounded-xl border p-3"
              key={r.id}
            >
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  onClick={async () => {
                    setEditingId(r.id);
                    setDepartments(await getActiveDepartmentsForAdmin(r.collegeId));
                    setV({
                      collegeId: String(r.collegeId),
                      departmentId: String(r.departmentId),
                      studentCategory: r.studentCategory || "OPEN",
                      academicYear: r.academicYear,
                      courseYear: r.courseYear || "",
                      title: r.title,
                      totalFee: String(r.totalFee),
                      minimumAmountForAdmission: String(r.minimumAmountForAdmission),
                    });
                  }}
                >
                  Edit
                </Button>
                <Button
                  variant="danger"
                  onClick={async () => {
                    if (!window.confirm(`Delete fee structure "${r.title}"?`)) return;
                    try {
                      await api.deleteAdminFeeStructure(r.id);
                      toast.success("Fee structure deleted");
                      void load();
                    } catch (error) {
                      toast.error(handleApiError(error).message);
                    }
                  }}
                >
                  Delete
                </Button>
              </div>
              <span>
                {r.collegeName} · {r.departmentName} · {r.courseYear || "Course year not set"} ·{" "}
                {r.studentCategory || "OPEN"}
              </span>
              <b>₹{r.totalFee}</b>
            </div>
          ))}
        </div>
        {!rows.length && (
          <EmptyState
            title="No fee structures"
            description="No configured fees match these filters."
          />
        )}
        <div className="mt-4">
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onChange={(next) => void load(next)}
          />
        </div>
      </Card>
    </div>
  );
}
export function AdminMoneyPage({
  pending = false,
  principal = false,
}: {
  pending?: boolean;
  principal?: boolean;
}) {
  const { user } = useAuth();
  const [result, setResult] = useState<PageResponse<
    api.FeeCollectionRow | api.PendingFeeRow
  > | null>(null);
  const [page, setPage] = useState(0);
  const [colleges, setColleges] = useState<College[]>([]);
  const [divisions, setDivisions] = useState<WeeklyDivision[]>([]);
  const [filters, setFilters] = useState({
    keyword: "",
    collegeId: principal ? String(user?.collegeId || "") : "",
    departmentId: "",
    courseYearId: "",
    divisionId: "",
  });

  useEffect(() => {
    Promise.all([
      principal ? Promise.resolve([]) : getActiveColleges(),
      weeklyTimetableApi.divisions(),
    ])
      .then(([c, d]) => {
        setColleges(c);
        setDivisions(d);
      })
      .catch(() => undefined);
  }, [principal]);

  useEffect(() => {
    setPage(0);
  }, [pending]);

  useEffect(() => {
    setResult(null);
    const timer = window.setTimeout(
      () =>
        (pending
          ? (principal ? api.getPrincipalPendingFees : api.getPendingFees)({
              ...filters,
              keyword: filters.keyword || undefined,
              collegeId: filters.collegeId || undefined,
              departmentId: filters.departmentId || undefined,
              courseYearId: filters.courseYearId || undefined,
              divisionId: filters.divisionId || undefined,
              page,
              size: 20,
            })
          : (principal ? api.getPrincipalCollections : api.getCollections)({
              ...filters,
              keyword: filters.keyword || undefined,
              collegeId: filters.collegeId || undefined,
              departmentId: filters.departmentId || undefined,
              courseYearId: filters.courseYearId || undefined,
              divisionId: filters.divisionId || undefined,
              page,
              size: 20,
            })
        )
          .then((data) => setResult(data as PageResponse<api.FeeCollectionRow | api.PendingFeeRow>))
          .catch((e) => toast.error(handleApiError(e).message)),
      250,
    );
    return () => window.clearTimeout(timer);
  }, [page, pending, principal, filters]);

  const rows = result?.content ?? [];
  const displayedAmount = rows.reduce(
    (sum, row) =>
      sum +
      moneyValue(
        pending ? (row as api.PendingFeeRow).remainingAmount : (row as api.FeeCollectionRow).amount,
      ),
    0,
  );
  return (
    <div className="page-container">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 className="page-title">{pending ? "Pending Fees" : "Fee Collection"}</h1>
          <p className="page-subtitle">
            {principal
              ? "College and department fee overview."
              : "Global college and department fee overview."}
          </p>
        </div>
        {result && (
          <div
            className={`rounded-2xl px-5 py-3 ${pending ? "bg-orange-50 text-orange-800" : "bg-emerald-50 text-emerald-800"}`}
          >
            <p className="text-[10px] font-bold uppercase tracking-wider">
              {pending ? "Pending Fee" : "Collected Fee"}
            </p>
            <p className="mt-1 text-xl font-black">₹{displayedAmount.toLocaleString("en-IN")}</p>
          </div>
        )}
      </div>
      <Card className="mt-6 p-4">
        <div
          className={`grid gap-3 md:grid-cols-2 ${principal ? "xl:grid-cols-4" : "xl:grid-cols-5"}`}
        >
          <Input
            placeholder="Search student name…"
            icon={<Search className="h-4 w-4" />}
            value={filters.keyword}
            onChange={(e) => {
              setFilters({ ...filters, keyword: e.target.value });
              setPage(0);
            }}
          />
          {!principal && (
            <Select
              options={[
                { label: "All colleges", value: "" },
                ...colleges.map((c) => ({ label: c.name, value: c.id })),
              ]}
              value={filters.collegeId}
              onChange={(e) => {
                setFilters({
                  ...filters,
                  collegeId: e.target.value,
                  departmentId: "",
                  courseYearId: "",
                  divisionId: "",
                });
                setPage(0);
              }}
            />
          )}
          <Select
            options={[
              { label: "All departments", value: "" },
              ...uniqueOptions(
                divisions.filter(
                  (d) => !filters.collegeId || d.collegeId === Number(filters.collegeId),
                ),
                "departmentId",
                "department",
              ),
            ]}
            value={filters.departmentId}
            onChange={(e) => {
              setFilters({
                ...filters,
                departmentId: e.target.value,
                courseYearId: "",
                divisionId: "",
              });
              setPage(0);
            }}
          />
          <Select
            options={[
              { label: "All years/classes", value: "" },
              ...uniqueOptions(scopedDivisions(divisions, filters), "courseYearId", "year"),
            ]}
            value={filters.courseYearId}
            onChange={(e) => {
              setFilters({ ...filters, courseYearId: e.target.value, divisionId: "" });
              setPage(0);
            }}
          />
          <Select
            options={[
              { label: "All divisions", value: "" },
              ...scopedDivisions(divisions, filters)
                .filter(
                  (d) => !filters.courseYearId || d.courseYearId === Number(filters.courseYearId),
                )
                .map((d) => ({ label: d.division, value: d.id })),
            ]}
            value={filters.divisionId}
            onChange={(e) => {
              setFilters({ ...filters, divisionId: e.target.value });
              setPage(0);
            }}
          />
        </div>
      </Card>
      <Card className="mt-6 overflow-hidden">
        {!result ? (
          <Loader />
        ) : rows.length ? (
          <>
            <div className="erp-table-scroll hidden md:block">
              <table className="erp-table">
                <thead>
                  <tr className="border-b bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                    <th className="px-5 py-4">Student</th>
                    <th className="px-5 py-4">College & Department</th>
                    <th className="px-5 py-4">Category</th>
                    {pending ? (
                      <>
                        <th className="px-5 py-4 text-right">Total / Paid</th>
                        <th className="px-5 py-4 text-right">Remaining</th>
                      </>
                    ) : (
                      <>
                        <th className="px-5 py-4">Payment Date</th>
                        <th className="px-5 py-4">Transaction Reference</th>
                        <th className="px-5 py-4 text-right">Amount</th>
                      </>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map((row) =>
                    pending ? (
                      <PendingFeeTableRow key={row.id} row={row as api.PendingFeeRow} />
                    ) : (
                      <CollectionTableRow key={row.id} row={row as api.FeeCollectionRow} />
                    ),
                  )}
                </tbody>
              </table>
            </div>
            <div className="grid gap-3 p-4 md:hidden">
              {rows.map((row) =>
                pending ? (
                  <PendingFeeCard key={row.id} row={row as api.PendingFeeRow} />
                ) : (
                  <CollectionCard key={row.id} row={row as api.FeeCollectionRow} />
                ),
              )}
            </div>
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

function scopedDivisions(
  divisions: WeeklyDivision[],
  filters: { collegeId: string; departmentId: string },
) {
  return divisions.filter(
    (item) =>
      (!filters.collegeId || item.collegeId === Number(filters.collegeId)) &&
      (!filters.departmentId || item.departmentId === Number(filters.departmentId)),
  );
}
function uniqueOptions(
  items: WeeklyDivision[],
  idKey: "departmentId" | "courseYearId",
  labelKey: "department" | "year",
) {
  const options = Array.from(
    new Map(items.map((item) => [item[idKey], item[labelKey]])).entries(),
  ).map(([value, label]) => ({ label, value }));

  if (idKey === "courseYearId") {
    options.sort(
      (left, right) =>
        academicYearSequence(left.label) - academicYearSequence(right.label) ||
        left.label.localeCompare(right.label, undefined, { numeric: true }),
    );
  }

  return options;
}

function academicYearSequence(label: string) {
  const normalized = label.toLowerCase();
  const namedYears: Array<[RegExp, number]> = [
    [/\b(first|fy)\b/, 1],
    [/\b(second|sy)\b/, 2],
    [/\b(third|ty)\b/, 3],
    [/\bfourth\b/, 4],
    [/\bfifth\b/, 5],
    [/\bsixth\b/, 6],
  ];
  const namedYear = namedYears.find(([pattern]) => pattern.test(normalized));
  if (namedYear) return namedYear[1];

  const numericYear = normalized.match(/\byear\s*(\d+)\b|\b(\d+)(?:st|nd|rd|th)\s+year\b/);
  return numericYear ? Number(numericYear[1] ?? numericYear[2]) : Number.MAX_SAFE_INTEGER;
}

function StudentIdentity({ name, detail }: { name: string; detail?: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-brand-50 font-bold text-brand-700">
        {name.charAt(0).toUpperCase()}
      </div>
      <div>
        <p className="font-semibold text-slate-900">{name}</p>
        {detail && <p className="mt-0.5 text-xs text-slate-400">{detail}</p>}
      </div>
    </div>
  );
}
function CategoryBadge({ value }: { value: string }) {
  return (
    <span className="inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-[11px] font-bold text-blue-700">
      {value}
    </span>
  );
}

function moneyValue(value: number | null | undefined) {
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : 0;
}

function formatMoney(value: number | null | undefined) {
  return moneyValue(value).toLocaleString("en-IN");
}

function CollectionTableRow({ row }: { row: api.FeeCollectionRow }) {
  return (
    <tr className="transition hover:bg-slate-50/70">
      <td className="px-5 py-4">
        <StudentIdentity name={row.studentName} detail={`Payment #${row.id}`} />
      </td>
      <td className="px-5 py-4">
        <p className="text-sm font-semibold text-slate-700">{row.collegeName}</p>
        <p className="mt-0.5 text-xs text-slate-400">{row.departmentName}</p>
      </td>
      <td className="px-5 py-4">
        <CategoryBadge value={row.studentCategory} />
      </td>
      <td className="px-5 py-4 text-sm text-slate-600">
        {new Date(`${row.paymentDate}T00:00:00`).toLocaleDateString("en-IN", {
          day: "2-digit",
          month: "short",
          year: "numeric",
        })}
      </td>
      <td className="px-5 py-4">
        <span className="rounded-lg bg-slate-100 px-2.5 py-1.5 font-mono text-xs text-slate-600">
          {row.transactionReference}
        </span>
      </td>
      <td className="px-5 py-4 text-right text-base font-black text-emerald-600">
        ₹{formatMoney(row.amount)}
      </td>
    </tr>
  );
}
function PendingFeeTableRow({ row }: { row: api.PendingFeeRow }) {
  return (
    <tr className="transition hover:bg-slate-50/70">
      <td className="px-5 py-4">
        <StudentIdentity name={row.studentName} detail={row.admissionNumber} />
      </td>
      <td className="px-5 py-4">
        <p className="text-sm font-semibold text-slate-700">{row.collegeName}</p>
        <p className="mt-0.5 text-xs text-slate-400">{row.departmentName}</p>
      </td>
      <td className="px-5 py-4">
        <CategoryBadge value={row.studentCategory} />
      </td>
      <td className="px-5 py-4 text-right">
        <p className="text-sm font-semibold">₹{formatMoney(row.totalFee)}</p>
        <p className="text-xs text-emerald-600">₹{formatMoney(row.paidAmount)} paid</p>
      </td>
      <td className="px-5 py-4 text-right text-base font-black text-orange-600">
        ₹{formatMoney(row.remainingAmount)}
      </td>
    </tr>
  );
}
function CollectionCard({ row }: { row: api.FeeCollectionRow }) {
  return (
    <div className="rounded-2xl border border-slate-100 p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <StudentIdentity
          name={row.studentName}
          detail={`${row.collegeName} · ${row.departmentName}`}
        />
        <CategoryBadge value={row.studentCategory} />
      </div>
      <div className="mt-4 grid gap-2 rounded-xl bg-slate-50 p-3 text-xs text-slate-500">
        <span className="flex items-center gap-2">
          <CalendarDays className="h-3.5 w-3.5" />
          {new Date(`${row.paymentDate}T00:00:00`).toLocaleDateString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric",
          })}
        </span>
        <span className="flex items-center gap-2">
          <Hash className="h-3.5 w-3.5" />
          <span className="break-all font-mono">{row.transactionReference}</span>
        </span>
      </div>
      <div className="mt-4 flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-400">Verified payment</span>
        <span className="flex items-center text-xl font-black text-emerald-600">
          <IndianRupee className="h-4 w-4" />
          {formatMoney(row.amount)}
        </span>
      </div>
    </div>
  );
}
function PendingFeeCard({ row }: { row: api.PendingFeeRow }) {
  const totalFee = Number(row.totalFee) || 0;
  const paidAmount = Number(row.paidAmount) || 0;
  const paidRate = totalFee > 0 ? Math.round((paidAmount / totalFee) * 100) : 0;
  return (
    <div className="rounded-2xl border border-slate-100 p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <StudentIdentity
          name={row.studentName}
          detail={`${row.collegeName} · ${row.departmentName}`}
        />
        <CategoryBadge value={row.studentCategory} />
      </div>
      <div className="mt-4 flex justify-between text-xs">
        <span className="text-slate-500">Paid ₹{formatMoney(row.paidAmount)}</span>
        <b className="text-orange-600">₹{formatMoney(row.remainingAmount)} pending</b>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
        <div className="h-full rounded-full bg-emerald-500" style={{ width: `${paidRate}%` }} />
      </div>
      <p className="mt-2 text-right text-[10px] font-bold text-slate-400">
        {paidRate}% of ₹{formatMoney(row.totalFee)} paid
      </p>
    </div>
  );
}
export function AdminAnalyticsPage({ principal = false }: { principal?: boolean }) {
  const { user } = useAuth();
  const [data, setData] = useState<api.AdminAnalytics | null>(null);
  const [colleges, setColleges] = useState<College[]>([]);
  const [divisions, setDivisions] = useState<WeeklyDivision[]>([]);
  const [qrManagerOpen, setQrManagerOpen] = useState(false);
  const [filters, setFilters] = useState({
    collegeId: principal ? String(user?.collegeId || "") : "",
    departmentId: "",
    courseYearId: "",
    divisionId: "",
  });
  useEffect(() => {
    Promise.all([
      principal ? Promise.resolve([]) : getActiveColleges(),
      weeklyTimetableApi.divisions(),
    ]).then(([c, d]) => {
      setColleges(c);
      setDivisions(d);
    });
  }, [principal]);
  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    api[principal ? "getPrincipalAnalytics" : "getAdminAnalytics"](
      {
        collegeId: principal ? undefined : filters.collegeId || undefined,
        departmentId: filters.departmentId || undefined,
        courseYearId: filters.courseYearId || undefined,
        divisionId: filters.divisionId || undefined,
      },
      controller.signal,
    )
      .then(setData)
      .catch((e) => {
        if (!controller.signal.aborted) toast.error(handleApiError(e).message);
      });
    return () => controller.abort();
  }, [filters, principal]);
  return (
    <div className="page-container pb-10">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Analytics</h1>
          <p className="page-subtitle">Filtered institutional, admission and fee analytics.</p>
        </div>
        {principal && (
          <Button variant="secondary" onClick={() => setQrManagerOpen(true)}>
            <QrCode className="h-4 w-4" /> Change Payment QR
          </Button>
        )}
      </div>
      <Modal
        open={qrManagerOpen}
        onClose={() => setQrManagerOpen(false)}
        title="Change College Payment QR"
        description="This QR code is shown to students when they submit fee payment proof."
        size="xl"
      >
        <PaymentQrManager />
      </Modal>
      <Card className="mt-6 p-4">
        <div
          className={`grid gap-3 md:grid-cols-2 ${principal ? "xl:grid-cols-3" : "xl:grid-cols-4"}`}
        >
          {!principal && (
            <Select
              options={[
                { label: "All colleges", value: "" },
                ...colleges.map((c) => ({ label: c.name, value: c.id })),
              ]}
              value={filters.collegeId}
              onChange={(e) =>
                setFilters({
                  collegeId: e.target.value,
                  departmentId: "",
                  courseYearId: "",
                  divisionId: "",
                })
              }
            />
          )}
          <Select
            options={[
              { label: "All departments", value: "" },
              ...uniqueOptions(
                divisions.filter(
                  (d) => !filters.collegeId || d.collegeId === Number(filters.collegeId),
                ),
                "departmentId",
                "department",
              ),
            ]}
            value={filters.departmentId}
            onChange={(e) =>
              setFilters({
                ...filters,
                departmentId: e.target.value,
                courseYearId: "",
                divisionId: "",
              })
            }
          />
          <Select
            options={[
              { label: "All years/classes", value: "" },
              ...uniqueOptions(scopedDivisions(divisions, filters), "courseYearId", "year"),
            ]}
            value={filters.courseYearId}
            onChange={(e) =>
              setFilters({ ...filters, courseYearId: e.target.value, divisionId: "" })
            }
          />
          <Select
            options={[
              { label: "All divisions", value: "" },
              ...scopedDivisions(divisions, filters)
                .filter(
                  (d) => !filters.courseYearId || d.courseYearId === Number(filters.courseYearId),
                )
                .map((d) => ({ label: d.division, value: d.id })),
            ]}
            value={filters.divisionId}
            onChange={(e) => setFilters({ ...filters, divisionId: e.target.value })}
          />
        </div>
      </Card>
      {data ? (
        <DashboardAnalytics analytics={data} principal={principal} />
      ) : (
        <Loader label="Loading analytics…" />
      )}
    </div>
  );
}

export function AdminLectureLoadPage() {
  const [rows, setRows] = useState<api.LectureLoadRow[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [courseYears, setCourseYears] = useState<api.AdminCourseYearOption[]>([]);
  const [divisions, setDivisions] = useState<WeeklyDivision[]>([]);
  const [staffId, setStaffId] = useState("");
  const [teacherSearch, setTeacherSearch] = useState("");
  const [scheduleSearch, setScheduleSearch] = useState("");
  const [table, setTable] = useState<TeacherTimetable | null>(null);
  const [tableLoading, setTableLoading] = useState(false);
  const [view, setView] = useState<"weekly" | "daily">("weekly");
  const [selectedDay, setSelectedDay] = useState("MONDAY");
  const [filters, setFilters] = useState({
    collegeId: "",
    departmentId: "",
    courseYearId: "",
    divisionId: "",
  });

  useEffect(() => {
    Promise.all([getAllColleges(), weeklyTimetableApi.divisions()])
      .then(([c, d]) => {
        setColleges(c);
        setDivisions(d);
      })
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setDepartments([]);
    if (!filters.collegeId) return;

    getActiveDepartmentsForAdmin(Number(filters.collegeId))
      .then((response) => {
        if (!cancelled) setDepartments(response);
      })
      .catch((error) => toast.error(handleApiError(error).message));
    return () => {
      cancelled = true;
    };
  }, [filters.collegeId]);

  useEffect(() => {
    let cancelled = false;
    setCourseYears([]);
    if (!filters.collegeId || !filters.departmentId) return;

    api
      .getCourseYearOptions(Number(filters.collegeId), Number(filters.departmentId))
      .then((response) => {
        if (!cancelled) setCourseYears(response);
      })
      .catch((error) => toast.error(handleApiError(error).message));
    return () => {
      cancelled = true;
    };
  }, [filters.collegeId, filters.departmentId]);

  useEffect(() => {
    const controller = new AbortController();
    api
      .getLectureLoad(
        {
          collegeId: filters.collegeId || undefined,
          departmentId: filters.departmentId || undefined,
          courseYearId: filters.courseYearId || undefined,
          divisionId: filters.divisionId || undefined,
        },
        controller.signal,
      )
      .then(setRows)
      .catch((e) => {
        if (!controller.signal.aborted) toast.error(handleApiError(e).message);
      });
    return () => controller.abort();
  }, [filters]);

  useEffect(() => {
    const controller = new AbortController();
    if (!staffId) {
      setTable(null);
      return;
    }
    setTableLoading(true);
    api
      .getStaffTimetable(Number(staffId), controller.signal)
      .then((response) => {
        setTable(response);
        setSelectedDay(
          ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"].includes(
            response.currentDay,
          )
            ? response.currentDay
            : "MONDAY",
        );
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setTable(null);
        toast.error(handleApiError(error).message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setTableLoading(false);
      });
    return () => controller.abort();
  }, [staffId]);

  const clearTeacher = () => {
    setStaffId("");
    setTable(null);
    setScheduleSearch("");
  };
  const normalizedTeacherSearch = teacherSearch.trim().toLowerCase();
  const staffOptions = rows
    .filter(
      (row) =>
        !normalizedTeacherSearch ||
        `${row.staffName} ${row.employeeCode}`.toLowerCase().includes(normalizedTeacherSearch),
    )
    .map((row) => ({
      value: row.staffId,
      label: `${row.staffName} (${row.employeeCode})`,
    }));
  const normalizedScheduleSearch = scheduleSearch.trim().toLowerCase();
  const matches = (lecture?: TeacherLecture) =>
    !normalizedScheduleSearch ||
    Boolean(
      lecture &&
        [lecture.subject, lecture.department, lecture.year, lecture.division].some((value) =>
          value.toLowerCase().includes(normalizedScheduleSearch),
        ),
    );
  const lectureMap = useMemo(
    () =>
      new Map(
        (table?.lectures ?? []).map((lecture) => [
          `${lecture.dayOfWeek}:${lecture.periodKey}`,
          lecture,
        ]),
      ),
    [table],
  );
  const dayData: TeacherDay | null = table
    ? {
        day: selectedDay,
        periods: table.periods,
        lectures: table.lectures.filter((lecture) => lecture.dayOfWeek === selectedDay),
      }
    : null;

  return (
    <div className="page-container min-w-0 space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-600">
            <CalendarDays className="h-4 w-4" />
            Admin teacher workload
          </div>
          <h1 className="page-title">Staff Lecture Load</h1>
          <p className="page-subtitle">
            Find a teacher and view their read-only weekly teaching schedule.
          </p>
        </div>
        {table && (
          <div className="flex rounded-xl border bg-white p-1 shadow-sm dark:bg-slate-900">
            <Button
              variant={view === "weekly" ? "primary" : "secondary"}
              onClick={() => setView("weekly")}
            >
              <Grid3X3 className="h-4 w-4" />
              Weekly Grid
            </Button>
            <Button
              variant={view === "daily" ? "primary" : "secondary"}
              onClick={() => setView("daily")}
            >
              <List className="h-4 w-4" />
              Daily View
            </Button>
          </div>
        )}
      </div>

      <Card className="mt-6 p-4">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <Select
            label="College"
            options={[
              { label: "All colleges", value: "" },
              ...colleges.map((c) => ({ label: c.name, value: c.id })),
            ]}
            value={filters.collegeId}
            onChange={(e) => {
              clearTeacher();
              setFilters({
                collegeId: e.target.value,
                departmentId: "",
                courseYearId: "",
                divisionId: "",
              });
            }}
          />
          <Select
            label="Department"
            disabled={!filters.collegeId}
            options={[
              { label: filters.collegeId ? "All departments" : "Select college first", value: "" },
              ...departments.map((department) => ({
                label: department.name,
                value: department.id,
              })),
            ]}
            value={filters.departmentId}
            onChange={(e) => {
              clearTeacher();
              setFilters({
                ...filters,
                departmentId: e.target.value,
                courseYearId: "",
                divisionId: "",
              });
            }}
          />
          <Select
            label="Year / Class"
            disabled={!filters.departmentId}
            options={[
              { label: "All years/classes", value: "" },
              ...courseYears.map((courseYear) => ({
                label: courseYear.displayName,
                value: courseYear.id,
              })),
            ]}
            value={filters.courseYearId}
            onChange={(e) => {
              clearTeacher();
              setFilters({ ...filters, courseYearId: e.target.value, divisionId: "" });
            }}
          />
          <Select
            label="Division"
            disabled={!filters.courseYearId}
            options={[
              { label: "All divisions", value: "" },
              ...scopedDivisions(divisions, filters)
                .filter(
                  (d) => !filters.courseYearId || d.courseYearId === Number(filters.courseYearId),
                )
                .map((d) => ({ label: d.division, value: d.id })),
            ]}
            value={filters.divisionId}
            onChange={(e) => {
              clearTeacher();
              setFilters({ ...filters, divisionId: e.target.value });
            }}
          />
        </div>
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <Input
            label="Search teacher"
            placeholder="Search by teacher name or employee ID..."
            icon={<Search className="h-4 w-4" />}
            value={teacherSearch}
            onChange={(event) => {
              setTeacherSearch(event.target.value);
              clearTeacher();
            }}
          />
          <Select
            label="Teacher"
            options={[
              {
                label: staffOptions.length ? "Select teacher" : "No matching teachers",
                value: "",
              },
              ...staffOptions,
            ]}
            value={staffId}
            onChange={(e) => setStaffId(e.target.value)}
          />
        </div>
      </Card>

      {tableLoading ? (
        <Loader label="Loading teacher lecture schedule..." />
      ) : !table ? (
        <Card>
          <EmptyState
            title="Select a teacher"
            description="Search or choose a teacher above to view their lecture load."
          />
        </Card>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <TeacherTimetableStat label="Teacher Name" value={table.teacherName} />
            <TeacherTimetableStat label="Employee ID" value={table.employeeId} />
            <TeacherTimetableStat
              label="Total Weekly Lectures"
              value={String(table.totalWeeklyLectures)}
            />
            <TeacherTimetableStat
              label="Today's Lectures"
              value={String(table.todayLectureCount)}
            />
          </div>

          <Card className="p-4">
            <Input
              placeholder="Search subject, department, year, or division..."
              value={scheduleSearch}
              onChange={(event) => setScheduleSearch(event.target.value)}
              icon={<Search className="h-4 w-4" />}
            />
          </Card>

          {table.lectures.length === 0 ? (
            <Card>
              <EmptyState
                title="No timetable assigned"
                description="This teacher has no scheduled lectures."
              />
            </Card>
          ) : view === "weekly" ? (
            <TeacherWeeklyGrid table={table} lectureMap={lectureMap} matches={matches} />
          ) : (
            <div className="space-y-4">
              <Card className="p-4">
                <Select
                  label="Day"
                  value={selectedDay}
                  onChange={(event) => setSelectedDay(event.target.value)}
                  options={["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"].map(
                    (day) => ({
                      value: day,
                      label: day[0] + day.slice(1).toLowerCase(),
                    }),
                  )}
                />
              </Card>
              {dayData && (
                <TeacherDayView data={dayData} matches={matches} currentDay={table.currentDay} />
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
