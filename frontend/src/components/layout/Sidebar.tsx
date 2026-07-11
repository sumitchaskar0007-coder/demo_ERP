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
  const isFeeSection = isRole([ROLES.FEE_SECTION]);
  const isStudent = isRole([ROLES.STUDENT]);
  const nav = isAdmin
    ? [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
        { label: "Colleges", to: ROUTES.colleges, icon: Building2 },
        { label: "Principals", to: ROUTES.principals, icon: UserPlus },
        { label: "Staff", to: ROUTES.staff, icon: Users },
        { label: "Students", to: ROUTES.students, icon: GraduationCap },
        { label: "Fee Setup", to: ROUTES.adminFeeSetup, icon: CreditCard },
        { label: "Fee Collection", to: ROUTES.adminFeeCollection, icon: WalletCards },
        { label: "Pending Fees", to: ROUTES.adminPendingFees, icon: CreditCard },
        { label: "Analytics", to: ROUTES.adminAnalytics, icon: BarChart3 },
        { label: "Account", to: ROUTES.account, icon: UserRound },
      ]
    : isPrincipal
    ? [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
        { label: "Departments", to: ROUTES.departments, icon: LibraryBig },
        { label: "Staff", to: ROUTES.staff, icon: Users },
        { label: "Create Student Section Staff", to: ROUTES.createStudentSectionStaff, icon: UserPlus },
        { label: "Create Fee Section Staff", to: ROUTES.createFeeSectionStaff, icon: UserPlus },
        { label: "Fee Structures", to: ROUTES.feeStructures, icon: CreditCard },
        { label: "Fee Dashboard", to: ROUTES.feeSectionDashboard, icon: WalletCards },
        { label: "Payments", to: ROUTES.feePayments, icon: CreditCard },
        { label: "Student Section Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
        { label: "Principal Review Queue", to: ROUTES.principalReviewReady, icon: FileText },
        { label: "Final Admissions", to: ROUTES.finalAdmissions, icon: FileText },
        { label: "Academic Classes", to: ROUTES.academicClasses, icon: GraduationCap },
        { label: "Sections", to: ROUTES.academicSections, icon: Users },
        { label: "Subjects", to: ROUTES.academicSubjects, icon: LibraryBig },
        { label: "Reports", to: ROUTES.admissionReport, icon: BarChart3 },
        { label: "Audit Logs", to: ROUTES.auditLogs, icon: FileText },
        { label: "Account", to: ROUTES.account, icon: UserRound },
        { label: "Profile", to: ROUTES.profile, icon: UserRound },
      ]
    : isStudentSection
    ? [
        { label: "Dashboard", to: ROUTES.studentSectionDashboard, icon: LayoutDashboard },
        { label: "Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
        { label: "Admission Report", to: ROUTES.admissionReport, icon: BarChart3 },
        { label: "Account", to: ROUTES.account, icon: UserRound },
        { label: "Profile", to: ROUTES.profile, icon: UserRound },
      ]
    : isFeeSection
    ? [
        { label: "Fee Dashboard", to: ROUTES.feeSectionDashboard, icon: LayoutDashboard },
        { label: "Fee Accounts", to: ROUTES.feeAccounts, icon: WalletCards },
        { label: "Payments", to: ROUTES.feePayments, icon: CreditCard },
        { label: "Fee Report", to: ROUTES.feeReport, icon: BarChart3 },
        { label: "Account", to: ROUTES.account, icon: UserRound },
        { label: "Profile", to: ROUTES.profile, icon: UserRound },
      ]
    : isStudent
    ? [
        { label: "Dashboard", to: ROUTES.studentDashboard, icon: LayoutDashboard },
        { label: "My Admission", to: ROUTES.studentAdmission, icon: FileText },
        { label: "My Fees", to: ROUTES.studentFees, icon: WalletCards },
        { label: "My Payments", to: ROUTES.studentPayments, icon: CreditCard },
        { label: "My Timetable", to: ROUTES.studentTimetable, icon: CalendarDays },
        { label: "My Attendance", to: ROUTES.studentAttendance, icon: BarChart3 },
        { label: "Account", to: ROUTES.account, icon: UserRound },
        { label: "My Profile", to: ROUTES.studentProfile, icon: UserRound },
      ]
    : [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
      ];
  const sectionLabel = isAdmin ? "Admin" : isPrincipal ? "Principal" : isStudentSection ? "Student Section" : isFeeSection ? "Accountant" : isStudent ? "Student" : "Menu";
  const roleFuture = isStudentSection
    ? [{ label: "Fee Verification", icon: CreditCard }, { label: "Documents", icon: FileText }, { label: "Reports", icon: BarChart3 }]
    : isStudent
      ? [{ label: "Fee Payment", icon: CreditCard }, { label: "Attendance", icon: CalendarDays }, { label: "Timetable", icon: WalletCards }, { label: "Results", icon: Printer }]
      : futureItems;

  return (
    <aside className={cn("flex h-full flex-col border-r border-blue-100/80 bg-white shadow-[8px_0_35px_rgba(37,99,235,0.04)] transition-all duration-300", !mobile && (collapsed ? "w-20" : "w-[260px]"))}>
      <div className={cn("flex h-20 items-center border-b px-5", collapsed && !mobile ? "justify-center" : "gap-3")}>
        <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 text-lg font-black text-white shadow-lg shadow-blue-500/20">
          J
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
                "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-semibold transition-all duration-200 ease-out",
                isActive ? "bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg shadow-blue-500/20" : "text-slate-600 hover:bg-blue-50 hover:text-blue-700",
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
