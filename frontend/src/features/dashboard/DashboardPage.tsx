import {
  Activity,
  ArrowRight,
  Building2,
  CheckCircle2,
  Clock3,
  GraduationCap,
  LibraryBig,
  Plus,
  Sparkles,
  UserPlus,
  Users,
} from "lucide-react";
import { useEffect, useState, type ComponentType } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { DonutChart } from "@/components/common/DonutChart";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { searchColleges } from "@/features/colleges/api";
import { searchDepartments } from "@/features/departments/api";
import { searchUsers } from "@/features/users/api";
import { handleApiError } from "@/lib/handleApiError";
import { ROLES, ROUTES } from "@/lib/constants";

interface Stat {
  label: string;
  value: string | number;
  icon: ComponentType<{ className?: string }>;
  color: string;
  accent: string;
}

export function DashboardPage() {
  const { user, isRole } = useAuth();
  const admin = isRole([ROLES.SUPER_ADMIN]);
  const [stats, setStats] = useState<Stat[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      if (!user) return;
      setLoading(true);
      try {
        if (admin) {
          const [colleges, activeColleges, departments, activeDepartments, users] = await Promise.all([
            searchColleges({ size: 1 }),
            searchColleges({ status: "ACTIVE", size: 1 }),
            searchDepartments({ size: 1 }),
            searchDepartments({ status: "ACTIVE", size: 1 }),
            searchUsers({ size: 1 }),
          ]);
          setStats([
            { label: "Total Colleges", value: colleges.totalElements, icon: Building2, color: "bg-blue-50 text-blue-600", accent: "bg-blue-500" },
            { label: "Active Colleges", value: activeColleges.totalElements, icon: Activity, color: "bg-emerald-50 text-emerald-600", accent: "bg-emerald-500" },
            { label: "Departments", value: departments.totalElements, icon: LibraryBig, color: "bg-amber-50 text-amber-600", accent: "bg-amber-500" },
            { label: "Active Departments", value: activeDepartments.totalElements, icon: Sparkles, color: "bg-cyan-50 text-cyan-600", accent: "bg-cyan-500" },
            { label: "System Users", value: users.totalElements, icon: Users, color: "bg-violet-50 text-violet-600", accent: "bg-violet-500" },
            { label: "Inactive Colleges", value: colleges.totalElements - activeColleges.totalElements, icon: Building2, color: "bg-rose-50 text-rose-600", accent: "bg-rose-500" },
          ]);
        } else {
          const [departments, active] = await Promise.all([
            searchDepartments({ collegeId: user.collegeId || undefined, size: 1 }),
            searchDepartments({ collegeId: user.collegeId || undefined, status: "ACTIVE", size: 1 }),
          ]);
          setStats([
            { label: "My College", value: user.collegeCode || "—", icon: Building2, color: "bg-blue-50 text-blue-600", accent: "bg-blue-500" },
            { label: "Departments", value: departments.totalElements, icon: LibraryBig, color: "bg-amber-50 text-amber-600", accent: "bg-amber-500" },
            { label: "Active Departments", value: active.totalElements, icon: Activity, color: "bg-emerald-50 text-emerald-600", accent: "bg-emerald-500" },
            { label: "Profile Status", value: user.status, icon: CheckCircle2, color: "bg-cyan-50 text-cyan-600", accent: "bg-cyan-500" },
          ]);
        }
      } catch (error) {
        toast.error(handleApiError(error).message);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [admin, user]);

  if (!user) return null;

  const quickActions = admin
    ? [
        { label: "Add college", detail: "Create a new college profile", to: ROUTES.colleges, icon: Plus, color: "bg-blue-50 text-blue-600" },
        { label: "Create Principal", detail: "Add a principal account", to: ROUTES.createPrincipal, icon: UserPlus, color: "bg-violet-50 text-violet-600" },
        { label: "Manage departments", detail: "Review academic departments", to: ROUTES.departments, icon: LibraryBig, color: "bg-amber-50 text-amber-600" },
      ]
    : [
        { label: "Add department", detail: "Create an academic department", to: ROUTES.departments, icon: Plus, color: "bg-blue-50 text-blue-600" },
        { label: "Manage staff", detail: "View your college staff", to: ROUTES.staff, icon: Users, color: "bg-violet-50 text-violet-600" },
        { label: "Review admissions", detail: "Open the review-ready queue", to: ROUTES.principalReviewReady, icon: GraduationCap, color: "bg-emerald-50 text-emerald-600" },
      ];

  const roleTitle = admin ? "Super Admin Dashboard" : "Principal Dashboard";
  const overviewLabels = admin
    ? ["Total Colleges", "Active Colleges", "Departments", "Active Departments", "System Users"]
    : ["Departments", "Active Departments"];
  const overviewColors = ["#4361ee", "#22c55e", "#f59e0b", "#06b6d4", "#8b5cf6"];
  const overviewSegments = overviewLabels.map((label, index) => ({
    label,
    value: Number(stats.find((item) => item.label === label)?.value || 0),
    color: overviewColors[index],
  }));

  return (
    <div className="page-container pb-10">
      <div className="mb-6 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold text-slate-400">Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;{admin ? "Super Admin" : "Principal"}</p>
          <h1 className="mt-2 text-2xl font-bold text-slate-900">{roleTitle}</h1>
          <p className="mt-1 text-sm text-slate-500">Overview of your current ERP workspace.</p>
        </div>
        <p className="inline-flex items-center gap-2 text-xs font-medium text-slate-400"><Clock3 className="h-4 w-4" />Updated just now</p>
      </div>

      <section className="erp-welcome-banner px-7 py-7 sm:px-9">
        <div className="absolute -right-10 -top-20 h-64 w-64 rounded-full border-[30px] border-blue-500/30" />
        <div className="absolute right-52 top-5 h-10 w-10 rotate-45 rounded-lg border-4 border-amber-400/80" />
        <div className="relative flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div><p className="text-sm text-blue-100">{admin ? "Jadhavr ERP Administration" : user.collegeName}</p><h2 className="mt-2 text-3xl font-bold">Welcome back, {user.fullName.split(" ")[0]}</h2><p className="mt-2 text-sm text-blue-100">Have a productive day managing your education workspace.</p></div>
          <div className="hidden rounded-2xl border border-white/10 bg-white/10 px-5 py-4 text-right backdrop-blur sm:block"><p className="text-xs text-blue-200">Account status</p><p className="mt-1 font-bold">{user.status}</p></div>
        </div>
      </section>

      {loading ? <Loader label="Preparing your dashboard…" /> : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
          {stats.map(({ label, value, icon: Icon, color, accent }) => (
            <Card key={label} className="relative overflow-hidden p-5 shadow-[0_8px_30px_rgba(15,23,42,0.05)]">
              <span className={`absolute inset-x-0 top-0 h-1 ${accent}`} />
              <div className="flex items-center justify-between"><div className={`grid h-11 w-11 place-items-center rounded-xl ${color}`}><Icon className="h-5 w-5" /></div><span className="rounded-full bg-emerald-50 px-2 py-1 text-[10px] font-bold text-emerald-600">Live</span></div>
              <p className="mt-4 text-2xl font-bold text-slate-900">{value}</p><p className="mt-1 text-xs font-medium text-slate-500">{label}</p>
            </Card>
          ))}
        </div>
      )}

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b px-6 py-5"><div><h2 className="font-bold text-slate-900">System overview</h2><p className="mt-1 text-xs text-slate-500">Live data from your existing ERP modules</p></div><Activity className="h-5 w-5 text-brand-600" /></div>
          <div className="flex min-h-[300px] items-center justify-center p-6"><DonutChart segments={overviewSegments} centerLabel="ERP Records" /></div>
        </Card>

        <Card className="overflow-hidden">
          <div className="border-b px-6 py-5"><h2 className="font-bold text-slate-900">Quick actions</h2><p className="mt-1 text-xs text-slate-500">Frequently used tools for your role</p></div>
          <div className="space-y-2 p-4">
            {quickActions.map(({ label, detail, to, icon: Icon, color }) => (
              <Link key={label} to={to} className="group flex items-center gap-3 rounded-xl p-3 transition hover:bg-slate-50">
                <div className={`grid h-11 w-11 shrink-0 place-items-center rounded-xl ${color}`}><Icon className="h-5 w-5" /></div>
                <div className="min-w-0 flex-1"><p className="text-sm font-semibold text-slate-800">{label}</p><p className="truncate text-xs text-slate-400">{detail}</p></div><ArrowRight className="h-4 w-4 text-slate-300 transition group-hover:translate-x-1 group-hover:text-brand-600" />
              </Link>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
