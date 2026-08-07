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
  Users,
  UserRound,
  WalletCards,
  FileText,
  Bell,
  CheckCircle2,
  BookOpen,
  Clock3,
} from "lucide-react";
import { NavLink, useLocation } from "react-router-dom";
import { BrandLogo } from "@/components/common/BrandLogo";
import { useAuth } from "@/features/auth/authStore";
import { useStudentAcademicAccess } from "@/features/academics/StudentAcademicAccessContext";
import { ROLES, ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  mobile?: boolean;
  onNavigate?: () => void;
}

export function Sidebar({ collapsed, onToggle, mobile, onNavigate }: SidebarProps) {
  const location = useLocation();
  const { isRole } = useAuth();
  const { divisionAllocated } = useStudentAcademicAccess();
  const isAdmin = isRole([ROLES.SUPER_ADMIN]);
  const isPrincipal = isRole([ROLES.PRINCIPAL]);
  const isStudentSection = isRole([ROLES.STUDENT_SECTION]);
  const isFeeSection = isRole([ROLES.FEE_SECTION]);
  const isStudent = isRole([ROLES.STUDENT]);
  const isOtherStaff = isRole([ROLES.HOD, ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER]);
  const isHod = isRole([ROLES.HOD]);
  const isTeacher = isRole([ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER]);
  const isClassTeacher = isRole([ROLES.CLASS_TEACHER]);
  const nav = isAdmin
    ? [
        { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
        { label: "Colleges", to: ROUTES.colleges, icon: Building2 },
        { label: "User Management", to: ROUTES.adminPeople, icon: Users },
        { label: "Fees", to: ROUTES.adminFees, icon: WalletCards },
        { label: "Analytics", to: ROUTES.adminInsights, icon: BarChart3 },
        { label: "Notices", to: ROUTES.notices, icon: Bell },
        { label: "Administration", to: ROUTES.adminAdministration, icon: FileText },
      ]
    : isPrincipal
      ? [
          { label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard },
          { label: "Students", to: ROUTES.students, icon: GraduationCap },
          { label: "Staff", to: ROUTES.staff, icon: Users },
          {
            label: "Admissions",
            to: ROUTES.studentSectionAdmissions,
            icon: GraduationCap,
          },
          { label: "Academics", to: ROUTES.principalAcademics, icon: LibraryBig },
          { label: "Fees", to: ROUTES.principalFees, icon: WalletCards },
          { label: "Reports & Analytics", to: ROUTES.principalReports, icon: BarChart3 },
          { label: "Notices", to: ROUTES.notices, icon: Bell },
          {
            label: "Administration",
            to: ROUTES.principalAdministration,
            icon: FileText,
          },
        ]
      : isStudentSection
        ? [
            { label: "Dashboard", to: ROUTES.studentSectionDashboard, icon: LayoutDashboard },
            {
              label: "Admission Records",
              to: ROUTES.studentSectionAdmissions,
              icon: GraduationCap,
            },
            { label: "Admission Report", to: ROUTES.admissionReport, icon: BarChart3 },
            { label: "Documents", to: ROUTES.studentSectionDocuments, icon: FileText },
            { label: "Notices", to: ROUTES.notices, icon: Bell },
            { label: "Profile", to: ROUTES.profile, icon: UserRound },
          ]
        : isFeeSection
          ? [
              { label: "Dashboard", to: ROUTES.feeOfficerWorkspace, icon: LayoutDashboard },
              {
                label: "Fee Accounts",
                to: `${ROUTES.feeOfficerWorkspace}?tab=accounts`,
                icon: WalletCards,
              },
              {
                label: "Pending Verifications",
                to: `${ROUTES.feeOfficerWorkspace}?tab=pending`,
                icon: Clock3,
              },
              {
                label: "Verified Payments",
                to: `${ROUTES.feeOfficerWorkspace}?tab=verified`,
                icon: CheckCircle2,
              },
              {
                label: "Rejected Payments",
                to: `${ROUTES.feeOfficerWorkspace}?tab=rejected`,
                icon: FileText,
              },
              {
                label: "Payment History",
                to: `${ROUTES.feeOfficerWorkspace}?tab=history`,
                icon: CreditCard,
              },
              {
                label: "Pending Dues",
                to: `${ROUTES.feeOfficerWorkspace}?tab=dues`,
                icon: WalletCards,
              },
              {
                label: "Fee Reports",
                to: `${ROUTES.feeOfficerWorkspace}?tab=reports`,
                icon: BarChart3,
              },
              { label: "Notices", to: ROUTES.notices, icon: Bell },
              { label: "Profile", to: ROUTES.profile, icon: UserRound },
            ]
          : isStudent
            ? [
                { label: "Dashboard", to: ROUTES.studentDashboard, icon: LayoutDashboard },
                { label: "My Admission", to: ROUTES.studentAdmission, icon: FileText },
                { label: "My Fees", to: ROUTES.studentFees, icon: WalletCards },
                ...(divisionAllocated
                  ? [
                      { label: "My Timetable", to: ROUTES.studentTimetable, icon: CalendarDays },
                      { label: "My Attendance", to: ROUTES.studentAttendance, icon: BarChart3 },
                      { label: "My Class", to: ROUTES.studentClass, icon: GraduationCap },
                      { label: "Notices", to: ROUTES.notices, icon: Bell },
                    ]
                  : []),
                { label: "My Profile", to: ROUTES.studentProfile, icon: UserRound },
              ]
            : isOtherStaff
              ? [
                  ...(!isHod
                    ? [
                        {
                          label: "Dashboard",
                          to: isTeacher ? ROUTES.teacherWorkspace : ROUTES.dashboard,
                          icon: LayoutDashboard,
                        },
                      ]
                    : []),
                  ...(isHod
                    ? [
                        { label: "HOD Overview", to: ROUTES.hodWorkspace, icon: LayoutDashboard },
                        {
                          label: "Student Allocation",
                          to: `${ROUTES.hodWorkspace}?tab=students`,
                          icon: Users,
                        },
                        {
                          label: "Teaching Assignments",
                          to: ROUTES.subjectTeacherAssignments,
                          icon: Users,
                        },
                        {
                          label: "Class Teachers",
                          to: `${ROUTES.hodWorkspace}?tab=class-teachers`,
                          icon: UserRound,
                        },
                        {
                          label: "Workload",
                          to: `${ROUTES.hodWorkspace}?tab=workload`,
                          icon: BarChart3,
                        },
                        {
                          label: "Manage Timetable",
                          to: ROUTES.timetable,
                          icon: CalendarDays,
                        },
                      ]
                    : []),
                  ...(isClassTeacher
                    ? [
                        {
                          label: "My Class",
                          to: `${ROUTES.teacherWorkspace}?tab=class`,
                          icon: GraduationCap,
                        },
                        {
                          label: "PRN & Roll Numbers",
                          to: `${ROUTES.teacherWorkspace}?tab=identifiers`,
                          icon: FileText,
                        },
                      ]
                    : []),
                  ...(isTeacher
                    ? [
                        {
                          label: "Student Directory",
                          to: `${ROUTES.teacherWorkspace}?tab=students`,
                          icon: Users,
                        },
                        {
                          label: "Attendance Analytics",
                          to: `${ROUTES.teacherWorkspace}?tab=attendance`,
                          icon: BarChart3,
                        },
                        {
                          label: "Needs Attention",
                          to: `${ROUTES.teacherWorkspace}?tab=attention`,
                          icon: Bell,
                        },
                        {
                          label: "Subject Coverage",
                          to: `${ROUTES.teacherWorkspace}?tab=coverage`,
                          icon: BookOpen,
                        },
                        {
                          label: "Workload",
                          to: `${ROUTES.teacherWorkspace}?tab=workload`,
                          icon: BarChart3,
                        },
                        {
                          label: "Today's Schedule",
                          to: `${ROUTES.teacherWorkspace}?tab=schedule`,
                          icon: CalendarDays,
                        },
                        {
                          label: "Notices",
                          to: `${ROUTES.teacherWorkspace}?tab=notices`,
                          icon: Bell,
                        },
                        {
                          label: "Notifications",
                          to: `${ROUTES.teacherWorkspace}?tab=notifications`,
                          icon: Bell,
                        },
                        { label: "My Timetable", to: ROUTES.teacherTimetable, icon: CalendarDays },
                        {
                          label: "Take Attendance",
                          to: ROUTES.teacherAttendance,
                          icon: CheckCircle2,
                        },
                      ]
                    : []),
                  ...(isClassTeacher || isRole([ROLES.HOD])
                    ? [
                        {
                          label: "Attendance Reports",
                          to: ROUTES.attendanceReport,
                          icon: BarChart3,
                        },
                      ]
                    : []),
                  ...(!isTeacher ? [{ label: "Notices", to: ROUTES.notices, icon: Bell }] : []),
                  { label: "Profile", to: ROUTES.profile, icon: UserRound },
                ]
              : [{ label: "Dashboard", to: ROUTES.dashboard, icon: LayoutDashboard }];
  const visibleNav = nav;
  const roleFuture: Array<{ label: string; icon: typeof FileText }> = [];
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
  const isCurrentLink = (to: string, routerActive: boolean) => {
    const [targetPath, targetQuery = ""] = to.split("?");
    const currentParams = new URLSearchParams(location.search);
    if (targetQuery) {
      if (location.pathname !== targetPath) return false;
      const targetParams = new URLSearchParams(targetQuery);
      return [...targetParams.entries()].every(([key, value]) => currentParams.get(key) === value);
    }
    if (location.pathname === targetPath && currentParams.has("tab")) {
      return currentParams.get("tab") === "overview";
    }
    if (isPrincipal) {
      const principalGroups: Record<string, boolean> = {
        [ROUTES.studentSectionAdmissions]:
          location.pathname.startsWith("/student-section/admissions") ||
          location.pathname.startsWith("/principal/admissions") ||
          location.pathname === ROUTES.studentSectionDocuments,
        [ROUTES.principalAcademics]:
          location.pathname.startsWith("/departments") ||
          location.pathname.startsWith("/principal/course-years") ||
          location.pathname.startsWith("/principal/divisions") ||
          location.pathname.startsWith("/academic/subjects") ||
          location.pathname === ROUTES.timetable,
        [ROUTES.principalFees]:
          location.pathname.startsWith("/fee-structures") ||
          location.pathname.startsWith("/principal/fee-"),
        [ROUTES.attendanceReport]: location.pathname === ROUTES.attendanceReport,
        [ROUTES.principalReports]:
          location.pathname === ROUTES.principalAnalytics ||
          location.pathname.startsWith("/reports/"),
        [ROUTES.principalAdministration]:
          location.pathname === ROUTES.auditLogs ||
          location.pathname === ROUTES.account ||
          location.pathname === ROUTES.accountChangePassword ||
          location.pathname === ROUTES.profile,
      };
      if (principalGroups[to]) return true;
    }
    if (isAdmin) {
      const adminGroups: Record<string, boolean> = {
        [ROUTES.adminPeople]:
          location.pathname.startsWith("/principals") ||
          location.pathname.startsWith("/users/") ||
          location.pathname.startsWith("/staff") ||
          location.pathname.startsWith("/students"),
        [ROUTES.adminFees]:
          location.pathname === ROUTES.adminFeeSetup ||
          location.pathname === ROUTES.adminFeeCollection ||
          location.pathname === ROUTES.adminPendingFees,
        [ROUTES.adminInsights]:
          location.pathname === ROUTES.adminAnalytics ||
          location.pathname === ROUTES.adminLectureLoad,
        [ROUTES.adminAdministration]:
          location.pathname === ROUTES.account ||
          location.pathname === ROUTES.accountChangePassword ||
          location.pathname === ROUTES.profile,
      };
      if (adminGroups[to]) return true;
    }
    return routerActive;
  };

  return (
    <aside
      className={cn(
        "flex h-full flex-col border-r border-slate-200/80 bg-white transition-all duration-300",
        !mobile && (collapsed ? "w-20" : "w-64"),
      )}
    >
      <div
        className={cn(
          "flex h-16 shrink-0 items-center border-b border-slate-100 px-5 lg:h-20",
          collapsed && !mobile ? "justify-center" : "gap-3",
        )}
      >
        {collapsed && !mobile ? <BrandLogo compact /> : <BrandLogo className="w-[185px]" />}
      </div>
      <div className="flex-1 overflow-y-auto px-3 py-5">
        <p
          className={cn(
            "mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400",
            collapsed && !mobile && "sr-only",
          )}
        >
          {sectionLabel}
        </p>
        <nav className="space-y-1">
          {visibleNav.map(({ label, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onNavigate}
              title={collapsed && !mobile ? label : undefined}
              className={({ isActive }) => {
                const active = isCurrentLink(to, isActive);
                return cn(
                  "relative flex h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition",
                  active
                    ? "bg-brand-50 text-brand-700 before:absolute before:-left-3 before:h-6 before:w-1 before:rounded-r-full before:bg-brand-600"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                  collapsed && !mobile && "justify-center",
                );
              }}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {(!collapsed || mobile) && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>
        {roleFuture.length > 0 && (
          <div>
            <div className="my-5 border-t" />
            <p
              className={cn(
                "mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400",
                collapsed && !mobile && "sr-only",
              )}
            >
              Future modules
            </p>
            <div className="space-y-1">
              {roleFuture
                .filter((item) => isAdmin || item.label !== "Analytics")
                .map(({ label, icon: Icon }) => (
                  <div
                    key={label}
                    title={`${label} — coming soon`}
                    className={cn(
                      "flex h-10 cursor-not-allowed items-center gap-3 rounded-xl px-3 text-sm text-slate-400",
                      collapsed && !mobile && "justify-center",
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    {(!collapsed || mobile) && (
                      <>
                        <span className="flex-1">{label}</span>
                        <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[9px] font-semibold uppercase">
                          Soon
                        </span>
                      </>
                    )}
                  </div>
                ))}
            </div>
          </div>
        )}
      </div>
      {!mobile && (
        <button
          onClick={onToggle}
          className="flex h-12 items-center justify-center border-t border-slate-100 text-slate-400 hover:bg-slate-50 hover:text-slate-700"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? (
            <ChevronRight className="h-5 w-5" />
          ) : (
            <>
              <ChevronLeft className="mr-2 h-5 w-5" />
              <span className="text-sm">Collapse</span>
            </>
          )}
        </button>
      )}
    </aside>
  );
}
