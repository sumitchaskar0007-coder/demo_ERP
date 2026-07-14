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
  Bell,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { BrandLogo } from "@/components/common/BrandLogo";
import { useAuth } from "@/features/auth/authStore";
import { ROLES, ROUTES } from "@/lib/constants";
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
  const isOtherStaff = isRole([ROLES.HOD, ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER]);
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
        { label: "Notices", to: ROUTES.notices, icon: Bell },
        { label: "Account", to: ROUTES.account, icon: UserRound },
      ]
    : isPrincipal
      ? [
          { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
          { label: "Departments", to: ROUTES.departments, icon: LibraryBig },
          { label: "Course Years", to: ROUTES.courseYears, icon: GraduationCap },
          { label: "Divisions", to: ROUTES.divisions, icon: Users },
          { label: "Staff", to: ROUTES.staff, icon: Users },
          { label: "Create Staff", to: ROUTES.createStaff, icon: UserPlus },
          { label: "Class Teacher Assignment", to: ROUTES.divisions, icon: UserRound },
          { label: "Fee Structures", to: ROUTES.feeStructures, icon: CreditCard },
          { label: "Fee Dashboard", to: ROUTES.feeSectionDashboard, icon: WalletCards },
          { label: "Payments", to: ROUTES.feePayments, icon: CreditCard },
          { label: "Student Section Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
          { label: "Principal Review Queue", to: ROUTES.principalReviewReady, icon: FileText },
          { label: "Final Admissions", to: ROUTES.finalAdmissions, icon: FileText },
          { label: "Academic", to: ROUTES.academicClasses, icon: GraduationCap },
          { label: "Subjects", to: ROUTES.academicSubjects, icon: LibraryBig },
          { label: "Allocate Students", to: ROUTES.studentAllocation, icon: Users },
          { label: "Reports", to: ROUTES.admissionReport, icon: BarChart3 },
          { label: "Audit Logs", to: ROUTES.auditLogs, icon: FileText },
          { label: "Notices", to: ROUTES.notices, icon: Bell },
          { label: "Account", to: ROUTES.account, icon: UserRound },
          { label: "Profile", to: ROUTES.profile, icon: UserRound },
        ]
      : isStudentSection
        ? [
            { label: "Dashboard", to: ROUTES.studentSectionDashboard, icon: LayoutDashboard },
            { label: "Admissions", to: ROUTES.studentSectionAdmissions, icon: GraduationCap },
            { label: "Admission Report", to: ROUTES.admissionReport, icon: BarChart3 },
            { label: "Notices", to: ROUTES.notices, icon: Bell },
            { label: "Account", to: ROUTES.account, icon: UserRound },
            { label: "Profile", to: ROUTES.profile, icon: UserRound },
          ]
        : isFeeSection
          ? [
              { label: "Fee Dashboard", to: ROUTES.feeSectionDashboard, icon: LayoutDashboard },
              { label: "Fee Accounts", to: ROUTES.feeAccounts, icon: WalletCards },
              { label: "Payments", to: ROUTES.feePayments, icon: CreditCard },
              { label: "Fee Report", to: ROUTES.feeReport, icon: BarChart3 },
              { label: "Notices", to: ROUTES.notices, icon: Bell },
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
                { label: "My Class", to: ROUTES.studentClass, icon: GraduationCap },
                { label: "Notices", to: ROUTES.notices, icon: Bell },
                { label: "Account", to: ROUTES.account, icon: UserRound },
                { label: "My Profile", to: ROUTES.studentProfile, icon: UserRound },
              ]
            : isOtherStaff
              ? [
                  { label: "My Class", to: ROUTES.classTeacherClass, icon: GraduationCap },
                  { label: "Notices", to: ROUTES.notices, icon: Bell },
                  { label: "Profile", to: ROUTES.profile, icon: UserRound },
                ]
              : [{ label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard }];
  const sectionLabel = isAdmin
    ? "Admin"
    : isPrincipal
      ? "Principal"
      : isStudentSection
        ? "Student Section"
        : isFeeSection
          ? "Accountant"
          : isStudent
            ? "Student"
            : isOtherStaff
              ? "Staff"
              : "Menu";
  const roleFuture = isStudentSection
    ? [
        { label: "Fee Verification", icon: CreditCard },
        { label: "Documents", icon: FileText },
        { label: "Reports", icon: BarChart3 },
      ]
    : isStudent
      ? [
          { label: "Fee Payment", icon: CreditCard },
          { label: "Attendance", icon: CalendarDays },
          { label: "Timetable", icon: WalletCards },
          { label: "Results", icon: Printer },
        ]
      : futureItems;

  return (
    <aside className={cn("flex h-full flex-col border-r border-slate-200/80 bg-white transition-all duration-300", !mobile && (collapsed ? "w-20" : "w-64"))}>
      <div className={cn("flex h-20 items-center border-b border-slate-100 px-5", collapsed && !mobile ? "justify-center" : "gap-3")}>
        {collapsed && !mobile ? <BrandLogo compact /> : <BrandLogo className="w-[185px]" />}
      </div>
      <div className="flex-1 overflow-y-auto px-3 py-5">
        <p className={cn("mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400", collapsed && !mobile && "sr-only")}>{sectionLabel}</p>
        <nav className="space-y-1">
          {nav.map(({ label, to, icon: Icon }) => (
            <NavLink key={to} to={to} onClick={onNavigate} title={collapsed && !mobile ? label : undefined} className={({ isActive }) => cn("relative flex h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition", isActive ? "bg-[#eef1ff] text-brand-700 before:absolute before:-left-3 before:h-6 before:w-1 before:rounded-r-full before:bg-brand-600" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900", collapsed && !mobile && "justify-center")}>
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
      {!mobile && <button onClick={onToggle} className="flex h-12 items-center justify-center border-t border-slate-100 text-slate-400 hover:bg-slate-50 hover:text-slate-700" aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}>{collapsed ? <ChevronRight className="h-5 w-5" /> : <><ChevronLeft className="mr-2 h-5 w-5" /><span className="text-sm">Collapse</span></>}</button>}
    </aside>
  );
}
