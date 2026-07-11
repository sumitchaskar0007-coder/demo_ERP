import {
  ArrowRight,
  BellRing,
  Building2,
  CalendarDays,
  GraduationCap,
  LibraryBig,
  Plus,
  UserPlus,
  Users,
  WalletCards,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { DonutChartCard, type DonutDatum } from "@/components/dashboard/DonutChartCard";
import { FeeCollectionCard } from "@/components/dashboard/FeeCollectionCard";
import { SectionHeader } from "@/components/dashboard/SectionHeader";
import { StatCard } from "@/components/dashboard/StatCard";
import { useAuth } from "@/features/auth/authStore";
import { searchColleges } from "@/features/colleges/api";
import { searchDepartments } from "@/features/departments/api";
import { searchStaff } from "@/features/staff/api";
import { searchUsers } from "@/features/users/api";
import { getFeeDashboard } from "@/features/fees/api";
import type { FeeDashboardResponse } from "@/features/fees/types";
import { ROLES, ROUTES } from "@/lib/constants";

interface Counts {
  colleges: number;
  departments: number;
  users: number;
  staff: number;
}
const emptyCounts: Counts = { colleges: 0, departments: 0, users: 0, staff: 0 };
const emptyFees: FeeDashboardResponse = {
  totalFeeAccounts: 0,
  pendingPayments: 0,
  verifiedPayments: 0,
  rejectedPayments: 0,
  totalCollectedAmount: 0,
  totalPendingAmount: 0,
  todayCollectedAmount: 0,
};
function pageTotal(result: PromiseSettledResult<unknown>) {
  if (result.status !== "fulfilled" || !result.value || typeof result.value !== "object") return 0;
  const total = (result.value as { totalElements?: unknown }).totalElements;
  return typeof total === "number" ? total : 0;
}

export function DashboardPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN]);
  const [counts, setCounts] = useState<Counts>(emptyCounts);
  const [fees, setFees] = useState<FeeDashboardResponse>(emptyFees);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!user) return;
    let active = true;
    const requests = admin
      ? [
          searchColleges({ size: 1 }),
          searchDepartments({ size: 1 }),
          searchUsers({ size: 1 }),
          searchStaff({ size: 1 }),
          getFeeDashboard(),
        ]
      : [
          Promise.resolve(null),
          searchDepartments({ collegeId: user.collegeId || undefined, size: 1 }),
          Promise.resolve(null),
          searchStaff({ collegeId: user.collegeId || undefined, size: 1 }),
          getFeeDashboard(),
        ];
    Promise.allSettled(requests)
      .then(([colleges, departments, users, staff, feeResult]) => {
        if (!active) return;
        setCounts({
          colleges: pageTotal(colleges),
          departments: pageTotal(departments),
          users: pageTotal(users),
          staff: pageTotal(staff),
        });
        if (feeResult.status === "fulfilled" && feeResult.value)
          setFees(feeResult.value as FeeDashboardResponse);
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [admin, user]);

  const distribution = useMemo<DonutDatum[]>(
    () => [
      { name: "Departments", value: counts.departments, color: "#2563eb" },
      { name: "Staff", value: counts.staff, color: "#14b8a6" },
      { name: "Users", value: counts.users, color: "#7c3aed" },
      { name: "Colleges", value: counts.colleges, color: "#fb7c3c" },
    ],
    [counts],
  );
  if (!user) return null;
  const firstName = user.fullName.split(" ")[0];
  const stats = admin
    ? [
        {
          title: "Colleges",
          value: counts.colleges,
          subtitle: "Institutions in ERP",
          icon: Building2,
          accent: "bg-blue-50 text-blue-600",
        },
        {
          title: "Departments",
          value: counts.departments,
          subtitle: "Academic departments",
          icon: LibraryBig,
          accent: "bg-teal-50 text-teal-600",
        },
        {
          title: "Users",
          value: counts.users,
          subtitle: "Role-based accounts",
          icon: Users,
          accent: "bg-purple-50 text-purple-600",
        },
        {
          title: "Fee Collection",
          value: `Rs. ${fees.totalCollectedAmount.toLocaleString("en-IN")}`,
          subtitle: `${fees.pendingPayments} pending payments`,
          icon: WalletCards,
          accent: "bg-orange-50 text-orange-600",
        },
      ]
    : [
        {
          title: "College",
          value: user.collegeCode || "—",
          subtitle: user.collegeName || "Assigned institution",
          icon: Building2,
          accent: "bg-blue-50 text-blue-600",
        },
        {
          title: "Departments",
          value: counts.departments,
          subtitle: "Your academic units",
          icon: LibraryBig,
          accent: "bg-teal-50 text-teal-600",
        },
        {
          title: "Staff",
          value: counts.staff,
          subtitle: "Active staff directory",
          icon: Users,
          accent: "bg-purple-50 text-purple-600",
        },
        {
          title: "Fee Collection",
          value: `Rs. ${fees.totalCollectedAmount.toLocaleString("en-IN")}`,
          subtitle: `${fees.pendingPayments} pending payments`,
          icon: WalletCards,
          accent: "bg-orange-50 text-orange-600",
        },
      ];
  const actions = admin
    ? [
        { label: "Manage Colleges", to: ROUTES.colleges, icon: Building2 },
        { label: "Create Principal", to: ROUTES.createPrincipal, icon: UserPlus },
        { label: "Manage Departments", to: ROUTES.departments, icon: LibraryBig },
      ]
    : [
        { label: "Add Department", to: ROUTES.departments, icon: Plus },
        { label: "Manage Staff", to: ROUTES.staff, icon: Users },
        { label: "View Profile", to: ROUTES.profile, icon: GraduationCap },
      ];
  return (
    <div className="page-container space-y-7">
      <section className="relative overflow-hidden rounded-3xl border border-blue-100 bg-gradient-to-r from-blue-50 via-teal-50 to-orange-50 p-7 shadow-card sm:p-9">
        <div className="absolute -right-10 -top-16 h-52 w-52 rounded-full bg-blue-300/20 blur-2xl" />
        <div className="absolute bottom-0 left-0 h-full w-1.5 bg-gradient-to-b from-blue-600 to-cyan-500" />
        <div className="relative">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">
            {admin ? "Super Admin" : "Principal"} workspace
          </p>
          <h1 className="mt-3 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
            Welcome back, {firstName}
          </h1>
          <p className="mt-3 max-w-xl text-slate-600">
            Here is what's happening in your college ERP today. Manage operations and monitor
            important activity from one place.
          </p>
        </div>
      </section>
      <section>
        <SectionHeader
          title="Overview"
          subtitle={loading ? "Updating live metrics…" : "A quick snapshot of your ERP"}
        />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats.map((stat) => (
            <StatCard key={stat.title} {...stat} />
          ))}
        </div>
      </section>
      <section className="grid gap-5 xl:grid-cols-3">
        <DonutChartCard
          title="Attendance Overview"
          subtitle="Attendance module coming soon"
          centerLabel="0%"
          data={[
            { name: "Present", value: 0, color: "#22c55e" },
            { name: "Absent", value: 0, color: "#ef4444" },
            { name: "Late", value: 0, color: "#f59e0b" },
            { name: "Leave", value: 0, color: "#7c3aed" },
          ]}
        />
        <FeeCollectionCard
          collected={fees.totalCollectedAmount}
          pending={fees.totalPendingAmount}
        />
        <DonutChartCard
          title="ERP Distribution"
          subtitle="Available organization data"
          centerLabel={String(distribution.reduce((sum, item) => sum + item.value, 0))}
          data={distribution}
        />
      </section>
      <section className="grid gap-5 lg:grid-cols-3">
        <InfoCard
          icon={BellRing}
          title="Recent Notices"
          message="No notices published yet."
          action="Notices module coming soon"
        />
        <InfoCard
          icon={CalendarDays}
          title="Holidays"
          message="No holidays added yet."
          action="Holiday calendar coming soon"
        />
        <InfoCard
          icon={CalendarDays}
          title="Calendar Events"
          message="No events added yet."
          action="Events module coming soon"
        />
      </section>
      <section>
        <SectionHeader title="Quick Actions" subtitle="Frequently used tools for your role" />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {actions.map(({ label, to, icon: Icon }) => (
            <Link key={label} to={to}>
              <Card className="group flex items-center gap-4 p-5">
                <div className="rounded-2xl bg-blue-50 p-3 text-blue-600">
                  <Icon className="h-5 w-5" />
                </div>
                <span className="flex-1 font-bold text-slate-800">{label}</span>
                <ArrowRight className="h-5 w-5 text-slate-300 transition-transform group-hover:translate-x-1 group-hover:text-blue-600" />
              </Card>
            </Link>
          ))}
        </div>
      </section>
      <Card className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center">
        <div className="rounded-2xl bg-slate-100 p-3 text-slate-500">
          <CalendarDays />
        </div>
        <div className="flex-1">
          <h2 className="font-bold text-slate-900">Timetable Snapshot</h2>
          <p className="mt-1 text-sm text-slate-500">Timetable module coming soon.</p>
        </div>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold uppercase tracking-wide text-slate-500">
          Future
        </span>
      </Card>
    </div>
  );
}

function InfoCard({
  icon: Icon,
  title,
  message,
  action,
}: {
  icon: typeof BellRing;
  title: string;
  message: string;
  action: string;
}) {
  return (
    <Card className="p-6">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-blue-50 p-2.5 text-blue-600">
          <Icon className="h-5 w-5" />
        </div>
        <h2 className="font-bold text-slate-900">{title}</h2>
      </div>
      <p className="mt-5 text-sm text-slate-500">{message}</p>
      <button disabled className="mt-5 text-xs font-bold text-slate-400">
        {action}
      </button>
    </Card>
  );
}
