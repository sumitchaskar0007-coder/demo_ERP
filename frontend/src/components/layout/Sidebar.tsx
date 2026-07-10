import {
  BarChart3,
  Building2,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CreditCard,
  GraduationCap,
  LayoutDashboard,
  LibraryBig,
  UserPlus,
  Users,
  UserRound,
  WalletCards,
  FileText,
  Printer,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { APP_NAME, ROLES, ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  mobile?: boolean;
  onNavigate?: () => void;
}

const futureItems = [
  { label: "Fees", icon: CreditCard },
  { label: "Attendance", icon: CalendarDays },
  { label: "Timetable", icon: WalletCards },
  { label: "Analytics", icon: BarChart3 },
];

export function Sidebar({ collapsed, onToggle, mobile, onNavigate }: SidebarProps) {
  const { isRole } = useAuth();
  const isAdmin = isRole([ROLES.SUPER_ADMIN]);
  const isPrincipal = isRole([ROLES.PRINCIPAL]);
  const isStudentSection = isRole([ROLES.STUDENT_SECTION]);
  const isStudent = isRole([ROLES.STUDENT]);
  const nav = isAdmin
    ? [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
        { label: "Colleges", to: ROUTES.colleges, icon: Building2 },
        { label: "Departments", to: ROUTES.departments, icon: LibraryBig },
        { label: "Users", to: ROUTES.users, icon: Users },
        { label: "Create Principal", to: ROUTES.createPrincipal, icon: UserPlus },
        { label: "Staff", to: ROUTES.staff, icon: Users },
        { label: "Create Student Section Staff", to: ROUTES.createStudentSectionStaff, icon: UserPlus },
        { label: "Student Section Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
        { label: "Principal Review Queue", to: ROUTES.principalReviewReady, icon: FileText },
      ]
    : isPrincipal
    ? [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
        { label: "Departments", to: ROUTES.departments, icon: LibraryBig },
        { label: "Staff", to: ROUTES.staff, icon: Users },
        { label: "Create Student Section Staff", to: ROUTES.createStudentSectionStaff, icon: UserPlus },
        { label: "Student Section Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
        { label: "Principal Review Queue", to: ROUTES.principalReviewReady, icon: FileText },
        { label: "Profile", to: ROUTES.profile, icon: UserRound },
      ]
    : isStudentSection
    ? [
        { label: "Dashboard", to: ROUTES.studentSectionDashboard, icon: LayoutDashboard },
        { label: "Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
        { label: "Profile", to: ROUTES.profile, icon: UserRound },
      ]
    : isStudent
    ? [
        { label: "Dashboard", to: ROUTES.studentDashboard, icon: LayoutDashboard },
        { label: "My Admission", to: ROUTES.studentAdmission, icon: FileText },
        { label: "My Profile", to: ROUTES.studentProfile, icon: UserRound },
      ]
    : [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
      ];
  const sectionLabel = isAdmin ? "Admin" : isPrincipal ? "Principal" : isStudentSection ? "Student Section" : isStudent ? "Student" : "Menu";
  const roleFuture = isStudentSection
    ? [{ label: "Fee Verification", icon: CreditCard }, { label: "Documents", icon: FileText }, { label: "Reports", icon: BarChart3 }]
    : isStudent
      ? [{ label: "Fee Payment", icon: CreditCard }, { label: "Attendance", icon: CalendarDays }, { label: "Timetable", icon: WalletCards }, { label: "Results", icon: Printer }]
      : futureItems;

  return (
    <aside className={cn("flex h-full flex-col border-r bg-white transition-all", !mobile && (collapsed ? "w-20" : "w-64"))}>
      <div className={cn("flex h-20 items-center border-b px-5", collapsed && !mobile ? "justify-center" : "gap-3")}>
        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-indigo-700 text-white shadow-lg shadow-blue-200">
          <GraduationCap className="h-6 w-6" />
        </div>
        {(!collapsed || mobile) && <div><p className="font-bold text-slate-900">{APP_NAME}</p><p className="text-[11px] font-medium uppercase tracking-wider text-slate-400">College Management</p></div>}
      </div>
      <div className="flex-1 overflow-y-auto px-3 py-5">
        <p className={cn("mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400", collapsed && !mobile && "sr-only")}>{sectionLabel}</p>
        <nav className="space-y-1">
          {nav.map(({ label, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onNavigate}
              title={collapsed && !mobile ? label : undefined}
              className={({ isActive }) => cn(
                "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition",
                isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                collapsed && !mobile && "justify-center",
              )}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {(!collapsed || mobile) && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="my-5 border-t" />
        <p className={cn("mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400", collapsed && !mobile && "sr-only")}>Future modules</p>
        <div className="space-y-1">
          {roleFuture.filter((item) => isAdmin || item.label !== "Analytics").map(({ label, icon: Icon }) => (
            <div key={label} title={`${label} — coming soon`} className={cn("flex h-10 cursor-not-allowed items-center gap-3 rounded-xl px-3 text-sm text-slate-400", collapsed && !mobile && "justify-center")}>
              <Icon className="h-4 w-4 shrink-0" />
              {(!collapsed || mobile) && <><span className="flex-1">{label}</span><span className="rounded bg-slate-100 px-1.5 py-0.5 text-[9px] font-semibold uppercase">Soon</span></>}
            </div>
          ))}
        </div>
      </div>
      {!mobile && (
        <button onClick={onToggle} className="flex h-12 items-center justify-center border-t text-slate-400 hover:bg-slate-50 hover:text-slate-700" aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}>
          {collapsed ? <ChevronRight className="h-5 w-5" /> : <><ChevronLeft className="mr-2 h-5 w-5" /><span className="text-sm">Collapse</span></>}
        </button>
      )}
    </aside>
  );
}
