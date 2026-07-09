import { Activity, ArrowRight, Building2, CalendarDays, CreditCard, GraduationCap, LibraryBig, Plus, Sparkles, UserPlus, Users, WalletCards } from "lucide-react";
import { useEffect, useState, type ComponentType } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { searchColleges } from "@/features/colleges/api";
import { searchDepartments } from "@/features/departments/api";
import { searchUsers } from "@/features/users/api";
import { handleApiError } from "@/lib/handleApiError";
import { ROLES, ROUTES } from "@/lib/constants";

interface Stat { label: string; value: string | number; icon: ComponentType<{ className?: string }>; color: string; }

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
            searchColleges({ size: 1 }), searchColleges({ status: "ACTIVE", size: 1 }),
            searchDepartments({ size: 1 }), searchDepartments({ status: "ACTIVE", size: 1 }),
            searchUsers({ size: 1 }),
          ]);
          setStats([
            { label: "Total Colleges", value: colleges.totalElements, icon: Building2, color: "bg-blue-50 text-blue-600" },
            { label: "Active Colleges", value: activeColleges.totalElements, icon: Activity, color: "bg-emerald-50 text-emerald-600" },
            { label: "Departments", value: departments.totalElements, icon: LibraryBig, color: "bg-indigo-50 text-indigo-600" },
            { label: "Active Departments", value: activeDepartments.totalElements, icon: Sparkles, color: "bg-cyan-50 text-cyan-600" },
            { label: "Users", value: users.totalElements, icon: Users, color: "bg-violet-50 text-violet-600" },
            { label: "Inactive Colleges", value: colleges.totalElements - activeColleges.totalElements, icon: Building2, color: "bg-amber-50 text-amber-600" },
          ]);
        } else {
          const [departments, active] = await Promise.all([
            searchDepartments({ collegeId: user.collegeId || undefined, size: 1 }),
            searchDepartments({ collegeId: user.collegeId || undefined, status: "ACTIVE", size: 1 }),
          ]);
          setStats([
            { label: "My College", value: user.collegeCode || "—", icon: Building2, color: "bg-blue-50 text-blue-600" },
            { label: "Departments", value: departments.totalElements, icon: LibraryBig, color: "bg-indigo-50 text-indigo-600" },
            { label: "Active Departments", value: active.totalElements, icon: Activity, color: "bg-emerald-50 text-emerald-600" },
            { label: "Profile Status", value: user.status, icon: Sparkles, color: "bg-cyan-50 text-cyan-600" },
          ]);
        }
      } catch (error) { toast.error(handleApiError(error).message); }
      finally { setLoading(false); }
    };
    load();
  }, [admin, user]);
  if (!user) return null;
  const quickActions = admin
    ? [{ label: "Add college", to: ROUTES.colleges, icon: Plus }, { label: "Create Principal", to: ROUTES.createPrincipal, icon: UserPlus }, { label: "Manage departments", to: ROUTES.departments, icon: LibraryBig }]
    : [{ label: "Add department", to: ROUTES.departments, icon: Plus }, { label: "View profile", to: ROUTES.profile, icon: Users }];
  const future = [{ label: "Admissions", icon: GraduationCap }, { label: "Fees", icon: CreditCard }, { label: "Attendance", icon: CalendarDays }, { label: "Timetable", icon: WalletCards }];
  return <div className="page-container"><div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-700 via-blue-700 to-indigo-800 p-7 text-white shadow-xl shadow-blue-200/50 sm:p-9"><div className="absolute right-0 top-0 h-64 w-64 rounded-full bg-white/10 blur-3xl" /><div className="relative"><p className="text-sm font-semibold text-blue-100">{admin ? "Super Admin workspace" : user.collegeName}</p><h1 className="mt-2 text-3xl font-bold sm:text-4xl">Welcome back, {user.fullName.split(" ")[0]}</h1><p className="mt-3 text-blue-100">Here is what's happening in your ERP today.</p></div></div>{loading ? <Loader label="Preparing your dashboard…" /> : <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{stats.map(({ label, value, icon: Icon, color }) => <Card key={label} className="flex items-center gap-4 p-5"><div className={`grid h-12 w-12 place-items-center rounded-2xl ${color}`}><Icon className="h-6 w-6" /></div><div><p className="text-sm text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold">{value}</p></div></Card>)}</div>}<div className="mt-6 grid gap-6 xl:grid-cols-[1fr_1.2fr]"><Card className="p-6"><h2 className="text-lg font-bold">Quick actions</h2><p className="mt-1 text-sm text-slate-500">Common tasks for your role.</p><div className="mt-5 space-y-2">{quickActions.map(({ label, to, icon: Icon }) => <Link key={label} to={to} className="flex items-center gap-3 rounded-xl border p-4 transition hover:border-brand-200 hover:bg-brand-50"><div className="rounded-xl bg-brand-50 p-2 text-brand-600"><Icon className="h-5 w-5" /></div><span className="flex-1 text-sm font-semibold">{label}</span><ArrowRight className="h-4 w-4 text-slate-400" /></Link>)}</div></Card><Card className="p-6"><h2 className="text-lg font-bold">Coming next</h2><p className="mt-1 text-sm text-slate-500">Future ERP modules—visible now, enabled later.</p><div className="mt-5 grid gap-3 sm:grid-cols-2">{future.map(({ label, icon: Icon }) => <div key={label} className="flex items-center gap-3 rounded-xl bg-slate-50 p-4 text-slate-400"><Icon className="h-5 w-5" /><span className="flex-1 text-sm font-medium">{label}</span><span className="text-[10px] font-bold uppercase">Soon</span></div>)}</div></Card></div></div>;
}
