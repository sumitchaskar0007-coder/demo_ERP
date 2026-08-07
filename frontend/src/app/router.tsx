import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useAuth } from "@/features/auth/authStore";
import { ROLES, ROUTES, defaultRouteForRoles } from "@/lib/constants";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { RoleRoute } from "@/routes/RoleRoute";
import { StudentAdmissionGate } from "@/routes/StudentAdmissionGate";
import { StudentAcademicAccessProvider } from "@/features/academics/StudentAcademicAccessContext";

// React component props are intentionally inferred from each selected module export.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyComponent = ComponentType<any>;

function lazyNamed<TModule, TKey extends keyof TModule>(
  loader: () => Promise<TModule>,
  exportName: TKey,
): LazyExoticComponent<Extract<TModule[TKey], AnyComponent>> {
  return lazy(async () => ({
    default: (await loader())[exportName] as Extract<TModule[TKey], AnyComponent>,
  }));
}

const LoginPage = lazyNamed(() => import("@/pages/auth/LoginPage"), "LoginPage");
const ForgotPasswordPage = lazyNamed(
  () => import("@/pages/auth/PasswordRecoveryPages"),
  "ForgotPasswordPage",
);
const ResetPasswordPage = lazyNamed(
  () => import("@/pages/auth/PasswordRecoveryPages"),
  "ResetPasswordPage",
);
const VerifyEmailPage = lazyNamed(
  () => import("@/pages/auth/PasswordRecoveryPages"),
  "VerifyEmailPage",
);
const ProfilePage = lazyNamed(() => import("@/pages/auth/ProfilePage"), "ProfilePage");
const ChangePasswordPage = lazyNamed(
  () => import("@/pages/auth/ChangePasswordPage"),
  "ChangePasswordPage",
);
const CollegeDetailsPage = lazyNamed(
  () => import("@/pages/colleges/CollegeDetailsPage"),
  "CollegeDetailsPage",
);
const CollegeListPage = lazyNamed(
  () => import("@/pages/colleges/CollegeListPage"),
  "CollegeListPage",
);
const AdmissionPrintPage = lazyNamed(
  () => import("@/pages/admissions/AdmissionPrintPage"),
  "AdmissionPrintPage",
);
const PrincipalAdmissionDetailPage = lazyNamed(
  () => import("@/pages/admissions/PrincipalAdmissionDetailPage"),
  "PrincipalAdmissionDetailPage",
);
const PrincipalReviewQueuePage = lazyNamed(
  () => import("@/pages/admissions/PrincipalReviewQueuePage"),
  "PrincipalReviewQueuePage",
);
const PublicAdmissionPage = lazyNamed(
  () => import("@/pages/admissions/PublicAdmissionPage"),
  "PublicAdmissionPage",
);
const StudentSectionAdmissionDetailPage = lazyNamed(
  () => import("@/pages/admissions/StudentSectionAdmissionDetailPage"),
  "StudentSectionAdmissionDetailPage",
);
const StudentSectionAdmissionListPage = lazyNamed(
  () => import("@/pages/admissions/StudentSectionAdmissionListPage"),
  "StudentSectionAdmissionListPage",
);
const StudentSectionDashboardPage = lazyNamed(
  () => import("@/pages/admissions/StudentSectionDashboardPage"),
  "StudentSectionDashboardPage",
);
const StudentDocumentsPage = lazyNamed(
  () => import("@/pages/admissions/StudentDocumentsPage"),
  "StudentDocumentsPage",
);
const DepartmentDetailsPage = lazyNamed(
  () => import("@/pages/departments/DepartmentDetailsPage"),
  "DepartmentDetailsPage",
);
const DepartmentListPage = lazyNamed(
  () => import("@/pages/departments/DepartmentListPage"),
  "DepartmentListPage",
);
const CreateStudentSectionStaffPage = lazyNamed(
  () => import("@/pages/staff/CreateStudentSectionStaffPage"),
  "CreateStudentSectionStaffPage",
);
const CreateFeeSectionStaffPage = lazyNamed(
  () => import("@/pages/staff/CreateFeeSectionStaffPage"),
  "CreateFeeSectionStaffPage",
);
const FeeStructureDetailsPage = lazyNamed(
  () => import("@/pages/fees/FeeStructurePages"),
  "FeeStructureDetailsPage",
);
const FeeStructureFormPage = lazyNamed(
  () => import("@/pages/fees/FeeStructurePages"),
  "FeeStructureFormPage",
);
const FeeStructureListPage = lazyNamed(
  () => import("@/pages/fees/FeeStructurePages"),
  "FeeStructureListPage",
);
const MyFeeTransactionsPage = lazyNamed(
  () => import("@/pages/fees/StudentFeePages"),
  "MyFeeTransactionsPage",
);
const MyPaymentsPage = lazyNamed(() => import("@/pages/fees/StudentFeePages"), "MyPaymentsPage");
const StudentFeesPage = lazyNamed(() => import("@/pages/fees/StudentFeePages"), "StudentFeesPage");
const SubmitPaymentPage = lazyNamed(
  () => import("@/pages/fees/StudentFeePages"),
  "SubmitPaymentPage",
);
const FeeAccountDetailsPage = lazyNamed(
  () => import("@/pages/fees/FeeSectionPages"),
  "FeeAccountDetailsPage",
);
const FeeAccountsPage = lazyNamed(() => import("@/pages/fees/FeeSectionPages"), "FeeAccountsPage");
const FeeSectionDashboardPage = lazyNamed(
  () => import("@/pages/fees/FeeSectionPages"),
  "FeeSectionDashboardPage",
);
const PaymentDetailsPage = lazyNamed(
  () => import("@/pages/fees/FeeSectionPages"),
  "PaymentDetailsPage",
);
const PaymentsPage = lazyNamed(() => import("@/pages/fees/FeeSectionPages"), "PaymentsPage");
const FeeOfficerWorkspacePage = lazyNamed(
  () => import("@/pages/fees/FeeOfficerWorkspacePage"),
  "FeeOfficerWorkspacePage",
);
const StaffListPage = lazyNamed(() => import("@/pages/staff/StaffListPage"), "StaffListPage");
const StaffDetailsPage = lazyNamed(
  () => import("@/pages/staff/StaffDetailsPage"),
  "StaffDetailsPage",
);
const CreateStaffPage = lazyNamed(() => import("@/pages/staff/CreateStaffPage"), "CreateStaffPage");
const EditStaffPage = lazyNamed(() => import("@/pages/staff/EditStaffPage"), "EditStaffPage");
const CourseYearFormPage = lazyNamed(
  () => import("@/pages/academic/CourseYearDivisionPages"),
  "CourseYearFormPage",
);
const CourseYearListPage = lazyNamed(
  () => import("@/pages/academic/CourseYearDivisionPages"),
  "CourseYearListPage",
);
const DivisionFormPage = lazyNamed(
  () => import("@/pages/academic/CourseYearDivisionPages"),
  "DivisionFormPage",
);
const DivisionDetailsPage = lazyNamed(
  () => import("@/pages/academic/CourseYearDivisionPages"),
  "DivisionDetailsPage",
);
const DivisionListPage = lazyNamed(
  () => import("@/pages/academic/CourseYearDivisionPages"),
  "DivisionListPage",
);
const StudentAdmissionPage = lazyNamed(
  () => import("@/pages/student/StudentAdmissionPage"),
  "StudentAdmissionPage",
);
const AdminStudentListPage = lazyNamed(
  () => import("@/pages/student/AdminStudentListPage"),
  "AdminStudentListPage",
);
const StudentDashboardPage = lazyNamed(
  () => import("@/pages/student/StudentDashboardPage"),
  "StudentDashboardPage",
);
const StudentProfilePage = lazyNamed(
  () => import("@/pages/student/StudentProfilePage"),
  "StudentProfilePage",
);
const CreatePrincipalPage = lazyNamed(
  () => import("@/pages/users/CreatePrincipalPage"),
  "CreatePrincipalPage",
);
const EditPrincipalPage = lazyNamed(
  () => import("@/features/users/EditPrincipalPage"),
  "EditPrincipalPage",
);
const UserDetailsPage = lazyNamed(() => import("@/pages/users/UserDetailsPage"), "UserDetailsPage");
const UserListPage = lazyNamed(() => import("@/pages/users/UserListPage"), "UserListPage");
const NoticesPage = lazyNamed(() => import("@/features/notices/NoticesPage"), "NoticesPage");
const AcademicSetupPage = lazyNamed(
  () => import("@/features/academics/AcademicSetupPage"),
  "AcademicSetupPage",
);
const TimetablePage = lazyNamed(
  () => import("@/features/academics/TimetablePage"),
  "TimetablePage",
);
const AttendancePage = lazyNamed(
  () => import("@/features/academics/AttendancePage"),
  "AttendancePage",
);
const ForbiddenPage = lazyNamed(() => import("@/pages/ForbiddenPage"), "ForbiddenPage");
const NotFoundPage = lazyNamed(() => import("@/pages/NotFoundPage"), "NotFoundPage");
const ServerErrorPage = lazyNamed(() => import("@/pages/ServerErrorPage"), "ServerErrorPage");
const AccountPage = lazyNamed(() => import("@/pages/account/AccountPage"), "AccountPage");
const RoleDashboardPage = lazyNamed(
  () => import("@/pages/dashboard/RoleDashboardPage"),
  "RoleDashboardPage",
);
const ReportPage = lazyNamed(() => import("@/pages/reports/ReportPage"), "ReportPage");
const AuditLogPage = lazyNamed(() => import("@/pages/audit/AuditLogPage"), "AuditLogPage");
const AdminAnalyticsPage = lazyNamed(
  () => import("@/pages/admin/AdminPages"),
  "AdminAnalyticsPage",
);
const AdminDashboardPage = lazyNamed(
  () => import("@/pages/admin/AdminPages"),
  "AdminDashboardPage",
);
const AdminFeeSetupPage = lazyNamed(() => import("@/pages/admin/AdminPages"), "AdminFeeSetupPage");
const AdminMoneyPage = lazyNamed(() => import("@/pages/admin/AdminPages"), "AdminMoneyPage");
const AdminLectureLoadPage = lazyNamed(
  () => import("@/pages/admin/AdminPages"),
  "AdminLectureLoadPage",
);
const AcademicCreatePage = lazyNamed(
  () => import("@/pages/academic/AcademicPages"),
  "AcademicCreatePage",
);
const AcademicListPage = lazyNamed(
  () => import("@/pages/academic/AcademicPages"),
  "AcademicListPage",
);
const StudentAcademicPage = lazyNamed(
  () => import("@/pages/academic/AcademicPages"),
  "StudentAcademicPage",
);
const SubjectEditPage = lazyNamed(
  () => import("@/pages/academic/AcademicPages"),
  "SubjectEditPage",
);
const MyClassRosterPage = lazyNamed(
  () => import("@/pages/academic/ClassAllocationPages"),
  "MyClassRosterPage",
);
const StudentAllocationPage = lazyNamed(
  () => import("@/pages/academic/ClassAllocationPages"),
  "StudentAllocationPage",
);
const StudentClassPage = lazyNamed(
  () => import("@/pages/academic/ClassAllocationPages"),
  "StudentClassPage",
);
const SubjectTeacherAssignmentPage = lazyNamed(
  () => import("@/pages/academic/SubjectTeacherAssignmentPage"),
  "SubjectTeacherAssignmentPage",
);
const TeacherTimetablePage = lazyNamed(
  () => import("@/features/teacherTimetable/TeacherTimetablePage"),
  "TeacherTimetablePage",
);
const TeacherAttendancePage = lazyNamed(
  () => import("@/features/attendance/TeacherAttendancePage"),
  "TeacherAttendancePage",
);
const StudentAttendancePage = lazyNamed(
  () => import("@/features/attendance/StudentAttendancePage"),
  "StudentAttendancePage",
);
const AttendanceReportPage = lazyNamed(
  () => import("@/features/attendance/AttendanceReportPage"),
  "AttendanceReportPage",
);
const HodWorkspacePage = lazyNamed(
  () => import("@/pages/hod/HodWorkspacePage"),
  "HodWorkspacePage",
);
const TeacherWorkspacePage = lazyNamed(
  () => import("@/pages/teacher/TeacherWorkspacePage"),
  "TeacherWorkspacePage",
);
const PrincipalWorkspacePage = lazyNamed(
  () => import("@/pages/principal/PrincipalWorkspacePage"),
  "PrincipalWorkspacePage",
);
const AdmissionDocumentSettingsPage = lazyNamed(
  () => import("@/pages/principal/AdmissionDocumentSettingsPage"),
  "AdmissionDocumentSettingsPage",
);
const AdminWorkspacePage = lazyNamed(
  () => import("@/pages/admin/AdminWorkspacePage"),
  "AdminWorkspacePage",
);

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  return (
    <Navigate to={isAuthenticated ? defaultRouteForRoles(user?.roles) : ROUTES.login} replace />
  );
}

function SmartDashboard() {
  const { isRole } = useAuth();
  if (isRole([ROLES.SUPER_ADMIN])) return <AdminDashboardPage />;
  if (isRole([ROLES.PRINCIPAL])) return <AdminDashboardPage principal />;
  if (isRole([ROLES.HOD])) return <Navigate to={ROUTES.hodWorkspace} replace />;
  return <RoleDashboardPage />;
}

export function AppRouter() {
  return (
    <Suspense
      fallback={
        <div className="grid min-h-[40vh] place-items-center text-sm text-slate-500">Loading…</div>
      }
    >
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path={ROUTES.login} element={<LoginPage />} />
        <Route path={ROUTES.forgotPassword} element={<ForgotPasswordPage />} />
        <Route path={ROUTES.resetPassword} element={<ResetPasswordPage />} />
        <Route path={ROUTES.verifyEmail} element={<VerifyEmailPage />} />
        <Route path={ROUTES.publicAdmission} element={<PublicAdmissionPage />} />
        <Route path={ROUTES.forbidden} element={<ForbiddenPage />} />
        <Route path={ROUTES.serverError} element={<ServerErrorPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path={ROUTES.changePassword} element={<ChangePasswordPage />} />
          <Route element={<StudentAdmissionGate />}>
            <Route
              element={
                <StudentAcademicAccessProvider>
                  <DashboardLayout />
                </StudentAcademicAccessProvider>
              }
            >
              <Route path={ROUTES.profile} element={<ProfilePage />} />
              <Route path={ROUTES.account} element={<AccountPage />} />
              <Route path={ROUTES.accountChangePassword} element={<ChangePasswordPage />} />
              <Route path={ROUTES.dashboard} element={<SmartDashboard />} />
              <Route path={ROUTES.notices} element={<NoticesPage />} />
              <Route path={ROUTES.attendance} element={<AttendancePage />} />

              <Route
                element={
                  <RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.PRINCIPAL, ROLES.HOD]} />
                }
              >
                <Route path={ROUTES.academicSetup} element={<AcademicSetupPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL]} />}>
                <Route path={ROUTES.staff} element={<StaffListPage />} />
                <Route path="/staff/:id" element={<StaffDetailsPage />} />
                <Route path={ROUTES.students} element={<AdminStudentListPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN]} />}>
                <Route path={ROUTES.adminPeople} element={<AdminWorkspacePage kind="people" />} />
                <Route path={ROUTES.adminFees} element={<AdminWorkspacePage kind="fees" />} />
                <Route
                  path={ROUTES.adminInsights}
                  element={<AdminWorkspacePage kind="insights" />}
                />
                <Route
                  path={ROUTES.adminAdministration}
                  element={<AdminWorkspacePage kind="administration" />}
                />
                <Route path={ROUTES.colleges} element={<CollegeListPage />} />
                <Route path="/colleges/create" element={<CollegeListPage />} />
                <Route path="/colleges/:id" element={<CollegeDetailsPage />} />
                <Route path={ROUTES.principals} element={<UserListPage />} />
                <Route path="/users/:id" element={<UserDetailsPage />} />
                <Route path={ROUTES.createPrincipal} element={<CreatePrincipalPage />} />
                <Route path={ROUTES.editPrincipal} element={<EditPrincipalPage />} />
                <Route path="/principals/create" element={<CreatePrincipalPage />} />
                <Route path={ROUTES.adminFeeSetup} element={<AdminFeeSetupPage />} />
                <Route path={ROUTES.adminFeeCollection} element={<AdminMoneyPage />} />
                <Route path={ROUTES.adminPendingFees} element={<AdminMoneyPage pending />} />
                <Route path={ROUTES.adminAnalytics} element={<AdminAnalyticsPage />} />
                <Route path={ROUTES.adminLectureLoad} element={<AdminLectureLoadPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL]} />}>
                <Route
                  path={ROUTES.principalAcademics}
                  element={<PrincipalWorkspacePage kind="academics" />}
                />
                <Route
                  path={ROUTES.principalFees}
                  element={<PrincipalWorkspacePage kind="fees" />}
                />
                <Route
                  path={ROUTES.principalReports}
                  element={<PrincipalWorkspacePage kind="reports" />}
                />
                <Route
                  path={ROUTES.principalAdministration}
                  element={<PrincipalWorkspacePage kind="administration" />}
                />
                <Route
                  path={ROUTES.principalAdmissionDocuments}
                  element={<AdmissionDocumentSettingsPage />}
                />
                <Route path={ROUTES.departments} element={<DepartmentListPage />} />
                <Route path="/departments/:id" element={<DepartmentDetailsPage />} />
                <Route path={ROUTES.createStaff} element={<CreateStaffPage />} />
                <Route path={ROUTES.editStaff} element={<EditStaffPage />} />
                <Route path={ROUTES.courseYears} element={<CourseYearListPage />} />
                <Route path={ROUTES.createCourseYear} element={<CourseYearFormPage />} />
                <Route path="/principal/course-years/:id/edit" element={<CourseYearFormPage />} />
                <Route path={ROUTES.divisions} element={<DivisionListPage />} />
                <Route path={ROUTES.createDivision} element={<DivisionFormPage />} />
                <Route path="/principal/divisions/:id" element={<DivisionDetailsPage />} />
                <Route path="/principal/divisions/:id/edit" element={<DivisionFormPage />} />
                <Route
                  path={ROUTES.createStudentSectionStaff}
                  element={<CreateStudentSectionStaffPage />}
                />
                <Route
                  path={ROUTES.createFeeSectionStaff}
                  element={<CreateFeeSectionStaffPage />}
                />
                <Route path={ROUTES.feeStructures} element={<FeeStructureListPage />} />
                <Route
                  path={ROUTES.principalFeeCollection}
                  element={<AdminMoneyPage principal />}
                />
                <Route
                  path={ROUTES.principalPendingFees}
                  element={<AdminMoneyPage pending principal />}
                />
                <Route
                  path={ROUTES.principalAnalytics}
                  element={<AdminAnalyticsPage principal />}
                />
                <Route path="/fee-structures/create" element={<FeeStructureFormPage />} />
                <Route path="/fee-structures/:id/edit" element={<FeeStructureFormPage />} />
                <Route path="/fee-structures/:id" element={<FeeStructureDetailsPage />} />
                <Route path={ROUTES.principalReviewReady} element={<PrincipalReviewQueuePage />} />
                <Route
                  path="/principal/admissions/:admissionId"
                  element={<PrincipalAdmissionDetailPage />}
                />
                <Route
                  path={ROUTES.finalAdmissions}
                  element={<Navigate to={ROUTES.principalReviewReady} replace />}
                />
                <Route path={ROUTES.auditLogs} element={<AuditLogPage />} />
              </Route>

              <Route
                element={
                  <RoleRoute
                    roles={[ROLES.PRINCIPAL, ROLES.HOD, ROLES.FEE_SECTION, ROLES.STUDENT_SECTION]}
                  />
                }
              >
                <Route path={ROUTES.admissionReport} element={<ReportPage type="admissions" />} />
                <Route path={ROUTES.feeReport} element={<ReportPage type="fees" />} />
                <Route path={ROUTES.studentReport} element={<ReportPage type="students" />} />
              </Route>

              <Route
                element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.HOD, ROLES.CLASS_TEACHER]} />}
              >
                <Route path={ROUTES.attendanceReport} element={<AttendanceReportPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.HOD]} />}>
                <Route
                  path={ROUTES.academicClasses}
                  element={<Navigate to={ROUTES.courseYears} replace />}
                />
                <Route
                  path="/academic/classes/create"
                  element={<Navigate to={ROUTES.createCourseYear} replace />}
                />
                <Route
                  path={ROUTES.academicSections}
                  element={<AcademicListPage kind="sections" />}
                />
                <Route
                  path="/academic/sections/create"
                  element={<AcademicCreatePage kind="sections" />}
                />
                <Route
                  path={ROUTES.academicSubjects}
                  element={<AcademicListPage kind="subjects" />}
                />
                <Route
                  path="/academic/subjects/create"
                  element={<AcademicCreatePage kind="subjects" />}
                />
                <Route path="/academic/subjects/:id/edit" element={<SubjectEditPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.HOD]} />}>
                <Route path={ROUTES.hodWorkspace} element={<HodWorkspacePage />} />
                <Route path={ROUTES.studentAllocation} element={<StudentAllocationPage />} />
                <Route
                  path={ROUTES.subjectTeacherAssignments}
                  element={<SubjectTeacherAssignmentPage />}
                />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.HOD]} />}>
                <Route path={ROUTES.timetable} element={<TimetablePage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.STUDENT_SECTION]} />}>
                <Route
                  path={ROUTES.studentSectionDashboard}
                  element={<StudentSectionDashboardPage />}
                />
                <Route
                  path={ROUTES.studentSectionAdmissions}
                  element={<StudentSectionAdmissionListPage />}
                />
                <Route
                  path="/student-section/admissions/:admissionId"
                  element={<StudentSectionAdmissionDetailPage />}
                />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.STUDENT_SECTION]} />}>
                <Route path={ROUTES.studentSectionDocuments} element={<StudentDocumentsPage />} />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.STUDENT_SECTION]} />}>
                <Route
                  path="/student-section/admissions/:admissionId/print"
                  element={<AdmissionPrintPage />}
                />
              </Route>

              <Route element={<RoleRoute roles={[ROLES.CLASS_TEACHER]} />}>
                <Route path={ROUTES.classTeacherClass} element={<MyClassRosterPage />} />
              </Route>
              <Route element={<RoleRoute roles={[ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER]} />}>
                <Route path={ROUTES.teacherWorkspace} element={<TeacherWorkspacePage />} />
                <Route path={ROUTES.teacherTimetable} element={<TeacherTimetablePage />} />
                <Route path={ROUTES.teacherAttendance} element={<TeacherAttendancePage />} />
              </Route>
              <Route element={<RoleRoute roles={[ROLES.STUDENT]} />}>
                <Route path={ROUTES.studentDashboard} element={<StudentDashboardPage />} />
                <Route path={ROUTES.studentProfile} element={<StudentProfilePage />} />
                <Route path={ROUTES.studentAdmission} element={<StudentAdmissionPage />} />
                <Route path="/student/admission/print" element={<AdmissionPrintPage />} />
                <Route path={ROUTES.studentFees} element={<StudentFeesPage />} />
                <Route path={ROUTES.studentPayments} element={<MyPaymentsPage />} />
                <Route path="/student/fees/payments/new" element={<SubmitPaymentPage />} />
                <Route path="/student/fees/transactions" element={<MyFeeTransactionsPage />} />
                <Route path={ROUTES.studentTimetable} element={<StudentAcademicPage />} />
                <Route path={ROUTES.studentAttendance} element={<StudentAttendancePage />} />
                <Route path={ROUTES.studentClass} element={<StudentClassPage />} />
              </Route>
              <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.FEE_SECTION]} />}>
                <Route path={ROUTES.feeOfficerWorkspace} element={<FeeOfficerWorkspacePage />} />
                <Route path={ROUTES.feeSectionDashboard} element={<FeeSectionDashboardPage />} />
                <Route path={ROUTES.feeAccounts} element={<FeeAccountsPage />} />
                <Route
                  path="/fee-section/fee-accounts/:feeAccountId"
                  element={<FeeAccountDetailsPage />}
                />
                <Route path={ROUTES.feePayments} element={<PaymentsPage />} />
                <Route path="/fee-section/payments/:paymentId" element={<PaymentDetailsPage />} />
              </Route>
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
