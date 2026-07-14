export const APP_NAME = "Jadhavr ERP";

export const ROUTES = {
  login: "/login",
  dashboard: "/dashboard",
  profile: "/profile",
  colleges: "/colleges",
  departments: "/departments",
  users: "/users",
  createPrincipal: "/users/principals/create",
  editPrincipal: "/users/principals/:id/edit",
  publicAdmission: "/admission/:collegeCode",
  publicAdmissionSuccess: "/admission/:collegeCode/success",
  studentDashboard: "/student/dashboard",
  studentProfile: "/student/profile",
  studentAdmission: "/student/admission",
  studentSectionDashboard: "/student-section/dashboard",
  studentSectionAdmissions: "/student-section/admissions",
  staff: "/staff",
  createStudentSectionStaff: "/staff/student-section/create",
  principalReviewReady: "/principal/admissions/review-ready",
  notices: "/notices",
  forbidden: "/forbidden",
  serverError: "/server-error",
} as const;

export const ROLES = {
  SUPER_ADMIN: "SUPER_ADMIN",
  PRINCIPAL: "PRINCIPAL",
  HOD: "HOD",
  FEE_SECTION: "FEE_SECTION",
  CLASS_TEACHER: "CLASS_TEACHER",
  SUBJECT_TEACHER: "SUBJECT_TEACHER",
  STUDENT_SECTION: "STUDENT_SECTION",
  STUDENT: "STUDENT",
} as const;

export type AppRole = (typeof ROLES)[keyof typeof ROLES];

export function defaultRouteForRoles(roles: string[] = []) {
  if (roles.includes(ROLES.SUPER_ADMIN) || roles.includes(ROLES.PRINCIPAL)) return ROUTES.dashboard;
  if (roles.includes(ROLES.STUDENT_SECTION)) return ROUTES.studentSectionDashboard;
  if (roles.includes(ROLES.HOD) || roles.includes(ROLES.CLASS_TEACHER) || roles.includes(ROLES.SUBJECT_TEACHER) || roles.includes(ROLES.FEE_SECTION)) return ROUTES.notices;
  if (roles.includes(ROLES.STUDENT)) return ROUTES.studentDashboard;
  return ROUTES.dashboard;
}

export function isRouteAllowedForRoles(path: string, roles: string[] = []) {
  if (roles.includes(ROLES.SUPER_ADMIN)) return true;
  if (roles.includes(ROLES.PRINCIPAL)) {
    return !path.startsWith("/student/") && !path.startsWith("/colleges") && !path.startsWith("/users");
  }
  if (roles.includes(ROLES.STUDENT_SECTION)) {
    return path.startsWith("/student-section") || path === ROUTES.profile;
  }
  if (roles.includes(ROLES.STUDENT)) {
    return path.startsWith("/student/") || path === ROUTES.notices;
  }
  if (roles.includes(ROLES.HOD) || roles.includes(ROLES.CLASS_TEACHER) || roles.includes(ROLES.SUBJECT_TEACHER) || roles.includes(ROLES.FEE_SECTION)) return path === ROUTES.notices || path === ROUTES.profile;
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
  { label: "Class Teacher", value: "CLASS_TEACHER" },
  { label: "Subject Teacher", value: "SUBJECT_TEACHER" },
  { label: "General Staff", value: "GENERAL_STAFF" },
];
