import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useAuth } from "@/features/auth/authStore";
import { LoginPage } from "@/pages/auth/LoginPage";
import {
  ForgotPasswordPage,
  ResetPasswordPage,
  VerifyEmailPage,
} from "@/pages/auth/PasswordRecoveryPages";
import { ProfilePage } from "@/pages/auth/ProfilePage";
import { ChangePasswordPage } from "@/pages/auth/ChangePasswordPage";
import { CollegeDetailsPage } from "@/pages/colleges/CollegeDetailsPage";
import { CollegeListPage } from "@/pages/colleges/CollegeListPage";
import { AdmissionPrintPage } from "@/pages/admissions/AdmissionPrintPage";
import { PrincipalAdmissionDetailPage } from "@/pages/admissions/PrincipalAdmissionDetailPage";
import { PrincipalReviewQueuePage } from "@/pages/admissions/PrincipalReviewQueuePage";
import { PublicAdmissionPage } from "@/pages/admissions/PublicAdmissionPage";
import { StudentSectionAdmissionDetailPage } from "@/pages/admissions/StudentSectionAdmissionDetailPage";
import { StudentSectionAdmissionListPage } from "@/pages/admissions/StudentSectionAdmissionListPage";
import { StudentSectionDashboardPage } from "@/pages/admissions/StudentSectionDashboardPage";
import { DepartmentDetailsPage } from "@/pages/departments/DepartmentDetailsPage";
import { DepartmentListPage } from "@/pages/departments/DepartmentListPage";
import { CreateStudentSectionStaffPage } from "@/pages/staff/CreateStudentSectionStaffPage";
import { CreateFeeSectionStaffPage } from "@/pages/staff/CreateFeeSectionStaffPage";
import {
  FeeStructureDetailsPage,
  FeeStructureFormPage,
  FeeStructureListPage,
} from "@/pages/fees/FeeStructurePages";
import {
  MyFeeTransactionsPage,
  MyPaymentsPage,
  StudentFeesPage,
  SubmitPaymentPage,
} from "@/pages/fees/StudentFeePages";
import {
  FeeAccountDetailsPage,
  FeeAccountsPage,
  FeeSectionDashboardPage,
  PaymentDetailsPage,
  PaymentsPage,
} from "@/pages/fees/FeeSectionPages";
import { StaffListPage } from "@/pages/staff/StaffListPage";
import { CreateStaffPage } from "@/pages/staff/CreateStaffPage";
import {
  CourseYearFormPage,
  CourseYearListPage,
  DivisionFormPage,
  DivisionDetailsPage,
  DivisionListPage,
} from "@/pages/academic/CourseYearDivisionPages";
import { StudentAdmissionPage } from "@/pages/student/StudentAdmissionPage";
import { AdminStudentListPage } from "@/pages/student/AdminStudentListPage";
import { StudentDashboardPage } from "@/pages/student/StudentDashboardPage";
import { StudentProfilePage } from "@/pages/student/StudentProfilePage";
import { CreatePrincipalPage } from "@/pages/users/CreatePrincipalPage";
import { EditPrincipalPage } from "@/features/users/EditPrincipalPage";
import { UserDetailsPage } from "@/pages/users/UserDetailsPage";
import { UserListPage } from "@/pages/users/UserListPage";
import { NoticesPage } from "@/features/notices/NoticesPage";
import { AcademicSetupPage } from "@/features/academics/AcademicSetupPage";
import { TimetablePage } from "@/features/academics/TimetablePage";
import { AttendancePage } from "@/features/academics/AttendancePage";
import { ClassTeacherTimetablePage } from "@/pages/academic/ClassTeacherTimetablePage";
import { ROLES, ROUTES, defaultRouteForRoles } from "@/lib/constants";
import { ForbiddenPage } from "@/pages/ForbiddenPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ServerErrorPage } from "@/pages/ServerErrorPage";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { RoleRoute } from "@/routes/RoleRoute";
import { AccountPage } from "@/pages/account/AccountPage";
import { RoleDashboardPage } from "@/pages/dashboard/RoleDashboardPage";
import { ReportPage } from "@/pages/reports/ReportPage";
import { AuditLogPage } from "@/pages/audit/AuditLogPage";
import {
  AdminAnalyticsPage,
  AdminDashboardPage,
  AdminFeeSetupPage,
  AdminMoneyPage,
  AdminLectureLoadPage,
} from "@/pages/admin/AdminPages";
import {
  AcademicCreatePage,
  AcademicListPage,
  FinalAdmissionsPage,
  StudentAcademicPage,
  SubjectEditPage,
} from "@/pages/academic/AcademicPages";
import {
  MyClassRosterPage,
  StudentAllocationPage,
  StudentClassPage,
} from "@/pages/academic/ClassAllocationPages";
import { SubjectTeacherAssignmentPage } from "@/pages/academic/SubjectTeacherAssignmentPage";
import { TeacherTimetablePage } from "@/features/teacherTimetable/TeacherTimetablePage";
import { TeacherAttendancePage } from "@/features/attendance/TeacherAttendancePage";
import { StudentAttendancePage } from "@/features/attendance/StudentAttendancePage";
import { AttendanceReportPage } from "@/features/attendance/AttendanceReportPage";

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  return (
    <Navigate to={isAuthenticated ? defaultRouteForRoles(user?.roles) : ROUTES.login} replace />
  );
}

function SmartDashboard() {
  const { isRole } = useAuth();
  return isRole([ROLES.SUPER_ADMIN]) ? <AdminDashboardPage /> : <RoleDashboardPage />;
}

export function AppRouter() {
  return (
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
        <Route element={<DashboardLayout />}>
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

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN]} />}>
            <Route path={ROUTES.colleges} element={<CollegeListPage />} />
            <Route path="/colleges/create" element={<CollegeListPage />} />
            <Route path="/colleges/:id" element={<CollegeDetailsPage />} />
            <Route path={ROUTES.principals} element={<UserListPage />} />
            <Route path="/users/:id" element={<UserDetailsPage />} />
            <Route path={ROUTES.createPrincipal} element={<CreatePrincipalPage />} />
            <Route path={ROUTES.editPrincipal} element={<EditPrincipalPage />} />
            <Route path="/principals/create" element={<CreatePrincipalPage />} />
            <Route path={ROUTES.staff} element={<StaffListPage />} />
            <Route path={ROUTES.students} element={<AdminStudentListPage />} />
            <Route path={ROUTES.adminFeeSetup} element={<AdminFeeSetupPage />} />
            <Route path={ROUTES.adminFeeCollection} element={<AdminMoneyPage />} />
            <Route path={ROUTES.adminPendingFees} element={<AdminMoneyPage pending />} />
            <Route path={ROUTES.adminAnalytics} element={<AdminAnalyticsPage />} />
            <Route path={ROUTES.adminLectureLoad} element={<AdminLectureLoadPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.PRINCIPAL]} />}>
            <Route path={ROUTES.departments} element={<DepartmentListPage />} />
            <Route path="/departments/:id" element={<DepartmentDetailsPage />} />
            <Route path={ROUTES.staff} element={<StaffListPage />} />
            <Route path={ROUTES.createStaff} element={<CreateStaffPage />} />
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
            <Route path={ROUTES.createFeeSectionStaff} element={<CreateFeeSectionStaffPage />} />
            <Route path={ROUTES.feeStructures} element={<FeeStructureListPage />} />
            <Route path="/fee-structures/create" element={<FeeStructureFormPage />} />
            <Route path="/fee-structures/:id/edit" element={<FeeStructureFormPage />} />
            <Route path="/fee-structures/:id" element={<FeeStructureDetailsPage />} />
            <Route path={ROUTES.principalReviewReady} element={<PrincipalReviewQueuePage />} />
            <Route
              path="/principal/admissions/:admissionId"
              element={<PrincipalAdmissionDetailPage />}
            />
            <Route path={ROUTES.finalAdmissions} element={<FinalAdmissionsPage />} />
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

          <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.HOD, ROLES.CLASS_TEACHER]} />}>
            <Route path={ROUTES.attendanceReport} element={<AttendanceReportPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.HOD]} />}>
            <Route path={ROUTES.academicClasses} element={<AcademicListPage kind="classes" />} />
            <Route
              path="/academic/classes/create"
              element={<AcademicCreatePage kind="classes" />}
            />
            <Route path={ROUTES.academicSections} element={<AcademicListPage kind="sections" />} />
            <Route
              path="/academic/sections/create"
              element={<AcademicCreatePage kind="sections" />}
            />
            <Route path={ROUTES.academicSubjects} element={<AcademicListPage kind="subjects" />} />
            <Route path={ROUTES.studentAllocation} element={<StudentAllocationPage />} />
            <Route
              path="/academic/subjects/create"
              element={<AcademicCreatePage kind="subjects" />}
            />
            <Route path="/academic/subjects/:id/edit" element={<SubjectEditPage />} />
            <Route
              path={ROUTES.subjectTeacherAssignments}
              element={<SubjectTeacherAssignmentPage />}
            />
          </Route>

          <Route
            element={
              <RoleRoute
                roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL, ROLES.HOD, ROLES.CLASS_TEACHER]}
              />
            }
          >
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
            <Route
              path="/student-section/admissions/:admissionId/print"
              element={<AdmissionPrintPage />}
            />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.CLASS_TEACHER]} />}>
            <Route path={ROUTES.classTeacherClass} element={<MyClassRosterPage />} />
            <Route path={ROUTES.classTeacherTimetable} element={<ClassTeacherTimetablePage />} />
          </Route>
          <Route element={<RoleRoute roles={[ROLES.CLASS_TEACHER, ROLES.SUBJECT_TEACHER]} />}>
            <Route path={ROUTES.teacherTimetable} element={<TeacherTimetablePage />} />
            <Route path={ROUTES.teacherAttendance} element={<TeacherAttendancePage />} />
          </Route>
          <Route element={<RoleRoute roles={[ROLES.STUDENT]} />}>
            <Route path={ROUTES.studentDashboard} element={<StudentDashboardPage />} />
            <Route path={ROUTES.studentProfile} element={<StudentProfilePage />} />
            <Route path={ROUTES.studentAdmission} element={<StudentAdmissionPage />} />
            <Route path={ROUTES.studentFees} element={<StudentFeesPage />} />
            <Route path={ROUTES.studentPayments} element={<MyPaymentsPage />} />
            <Route path="/student/fees/payments/new" element={<SubmitPaymentPage />} />
            <Route path="/student/fees/transactions" element={<MyFeeTransactionsPage />} />
            <Route path={ROUTES.studentTimetable} element={<StudentAcademicPage />} />
            <Route path={ROUTES.studentAttendance} element={<StudentAttendancePage />} />
            <Route path={ROUTES.studentClass} element={<StudentClassPage />} />
          </Route>
          <Route element={<RoleRoute roles={[ROLES.PRINCIPAL, ROLES.FEE_SECTION]} />}>
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

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
