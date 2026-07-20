export const APP_NAME = "Jadhavr ERP";

export const ROUTES = {
  login: "/login",
  forgotPassword: "/forgot-password",
  resetPassword: "/reset-password",
  verifyEmail: "/verify-email",
  emailNotifications: "/admin/email-notifications",
  changePassword: "/change-password",
  dashboard: "/dashboard",
  hodWorkspace: "/hod",
  teacherWorkspace: "/teacher/workspace",
  account: "/account",
  accountChangePassword: "/account/change-password",
  auditLogs: "/audit-logs",
  admissionReport: "/reports/admissions",
  feeReport: "/reports/fees",
  attendanceReport: "/reports/attendance",
  studentReport: "/reports/students",
  profile: "/profile",
  colleges: "/colleges",
  departments: "/departments",
  users: "/users",
  createPrincipal: "/users/principals/create",
  editPrincipal: "/users/principals/:id/edit",
  principals: "/principals",
  adminFeeSetup: "/admin/fee-setup",
  adminFeeCollection: "/admin/fee-collection",
  adminPendingFees: "/admin/pending-fees",
  adminAnalytics: "/admin/analytics",
  principalFeeCollection: "/principal/fee-collection",
  principalPendingFees: "/principal/pending-fees",
  principalAnalytics: "/principal/analytics",
  adminLectureLoad: "/admin/lecture-load",
  publicAdmission: "/admission/:collegeCode",
  publicAdmissionSuccess: "/admission/:collegeCode/success",
  studentDashboard: "/student/dashboard",
  studentProfile: "/student/profile",
  studentAdmission: "/student/admission",
  studentSectionDashboard: "/student-section/dashboard",
  studentSectionAdmissions: "/student-section/admissions",
  staff: "/staff",
  students: "/students",
  createStudentSectionStaff: "/staff/student-section/create",
  createFeeSectionStaff: "/staff/fee-section/create",
  createStaff: "/principal/staff/create",
  courseYears: "/principal/course-years",
  createCourseYear: "/principal/course-years/create",
  divisions: "/principal/divisions",
  createDivision: "/principal/divisions/create",
  feeStructures: "/fee-structures",
  studentFees: "/student/fees",
  studentPayments: "/student/fees/payments",
  feeSectionDashboard: "/fee-section/dashboard",
  feeAccounts: "/fee-section/fee-accounts",
  feePayments: "/fee-section/payments",
  principalReviewReady: "/principal/admissions/review-ready",
  finalAdmissions: "/principal/final-admissions",
  academicClasses: "/academic/classes",
  academicSections: "/academic/sections",
  academicSubjects: "/academic/subjects",
  subjectTeacherAssignments: "/academic/subject-teacher-assignments",
  editSubject: "/academic/subjects/:id/edit",
  academicTimetable: "/academic/timetable",
  academicAttendance: "/academic/attendance",
  studentTimetable: "/student/academic/timetable",
  studentAttendance: "/student/academic/attendance",
  studentClass: "/student/academic/class",
  studentAllocation: "/academic/student-allocation",
  classTeacherClass: "/academic/class-teacher/my-class",
  classTeacherTimetable: "/academic/class-teacher/timetable",
  teacherTimetable: "/teacher/timetable",
  teacherAttendance: "/teacher/attendance",
  notices: "/notices",
  academicSetup: "/academic-setup",
  timetable: "/timetable",
  attendance: "/attendance",
  forbidden: "/forbidden",
  serverError: "/server-error",
} as const;

export const ROLES = {
  SUPER_ADMIN: "SUPER_ADMIN",
  ADMIN: "ADMIN",
  PRINCIPAL: "PRINCIPAL",
  HOD: "HOD",
  FEE_SECTION: "FEE_SECTION",
  CLASS_TEACHER: "CLASS_TEACHER",
  SUBJECT_TEACHER: "SUBJECT_TEACHER",
  STUDENT_SECTION: "STUDENT_SECTION",
  GENERAL_STAFF: "GENERAL_STAFF",
  STUDENT: "STUDENT",
} as const;

export type AppRole = (typeof ROLES)[keyof typeof ROLES];

export function defaultRouteForRoles(roles: string[] = []) {
  if (
    roles.includes(ROLES.SUPER_ADMIN) ||
    roles.includes(ROLES.ADMIN) ||
    roles.includes(ROLES.PRINCIPAL)
  )
    return ROUTES.dashboard;
  if (roles.includes(ROLES.STUDENT_SECTION)) return ROUTES.studentSectionDashboard;
  if (roles.includes(ROLES.FEE_SECTION)) return ROUTES.feeSectionDashboard;
  if (roles.includes(ROLES.HOD)) return ROUTES.hodWorkspace;
  if (roles.includes(ROLES.CLASS_TEACHER) || roles.includes(ROLES.SUBJECT_TEACHER))
    return ROUTES.teacherWorkspace;
  if (roles.includes(ROLES.STUDENT)) return ROUTES.studentDashboard;
  return ROUTES.dashboard;
}

export function isRouteAllowedForRoles(path: string, roles: string[] = []) {
  if (roles.includes(ROLES.SUPER_ADMIN) || roles.includes(ROLES.ADMIN)) {
    return (
      path === ROUTES.dashboard ||
      path === ROUTES.account ||
      path === ROUTES.accountChangePassword ||
      path.startsWith("/colleges") ||
      path.startsWith("/principals") ||
      path === ROUTES.staff ||
      path === ROUTES.students ||
      path === ROUTES.timetable ||
      path.startsWith("/admin/")
    );
  }
  if (roles.includes(ROLES.PRINCIPAL)) {
    return (
      !path.startsWith("/student/") && !path.startsWith("/colleges") && !path.startsWith("/users")
    );
  }
  if (roles.includes(ROLES.STUDENT_SECTION)) {
    return (
      path.startsWith("/student-section") || path === ROUTES.profile || path === ROUTES.notices
    );
  }
  if (roles.includes(ROLES.STUDENT)) {
    return (
      path.startsWith("/student/") ||
      path === ROUTES.dashboard ||
      path === ROUTES.account ||
      path === ROUTES.accountChangePassword ||
      path === ROUTES.notices
    );
  }
  if (roles.includes(ROLES.FEE_SECTION))
    return path.startsWith("/fee-section") || path === ROUTES.notices || path === ROUTES.profile;
  if (
    roles.some((role) =>
      [ROLES.HOD, ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER].includes(role as never),
    )
  )
    return (
      path.startsWith("/academic") ||
      (roles.includes(ROLES.HOD) && path.startsWith("/hod")) ||
      ((roles.includes(ROLES.CLASS_TEACHER) || roles.includes(ROLES.SUBJECT_TEACHER)) &&
        (path.startsWith("/teacher/") || path === ROUTES.teacherTimetable || path === ROUTES.teacherAttendance)) ||
      ((roles.includes(ROLES.HOD) || roles.includes(ROLES.CLASS_TEACHER)) &&
        path === ROUTES.attendanceReport) ||
      ((roles.includes(ROLES.HOD) || roles.includes(ROLES.CLASS_TEACHER)) &&
        path === ROUTES.timetable) ||
      path === ROUTES.dashboard ||
      path === ROUTES.notices ||
      path === ROUTES.profile
    );
  return false;
}

export const PAGE_SIZE = 10;

export const STATUS_OPTIONS = [
  { label: "All statuses", value: "" },
  { label: "Active", value: "ACTIVE" },
  { label: "Inactive", value: "INACTIVE" },
];

export const ADMISSION_STATUS_OPTIONS = [
  { label: "All statuses", value: "" },
  { label: "Submitted", value: "SUBMITTED" },
  { label: "Under Student Section Review", value: "STUDENT_SECTION_REVIEW_PENDING" },
  { label: "Student Section Approved", value: "STUDENT_SECTION_APPROVED" },
  { label: "Student Section Rejected", value: "STUDENT_SECTION_REJECTED" },
  { label: "Principal Review Pending", value: "PRINCIPAL_REVIEW_PENDING" },
  { label: "Principal Approved", value: "PRINCIPAL_APPROVED" },
  { label: "Principal Rejected", value: "PRINCIPAL_REJECTED" },
  { label: "Cancelled", value: "CANCELLED" },
];

export const STAFF_TYPE_OPTIONS = [
  { label: "All staff types", value: "" },
  { label: "Student Section", value: "STUDENT_SECTION" },
  { label: "Fee Section", value: "FEE_SECTION" },
  { label: "HOD", value: "HOD" },
  { label: "Teacher", value: "TEACHER" },
  { label: "Class Teacher", value: "CLASS_TEACHER" },
  { label: "Subject Teacher", value: "SUBJECT_TEACHER" },
  { label: "General Staff", value: "GENERAL_STAFF" },
];
