import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useAuth } from "@/features/auth/authStore";
import { LoginPage } from "@/features/auth/LoginPage";
import { ProfilePage } from "@/features/auth/ProfilePage";
import { ChangePasswordPage } from "@/features/auth/ChangePasswordPage";
import { CollegeDetailsPage } from "@/features/colleges/CollegeDetailsPage";
import { CollegeListPage } from "@/features/colleges/CollegeListPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { AdmissionPrintPage } from "@/features/admissions/AdmissionPrintPage";
import { PrincipalAdmissionDetailPage } from "@/features/admissions/PrincipalAdmissionDetailPage";
import { PrincipalReviewQueuePage } from "@/features/admissions/PrincipalReviewQueuePage";
import { PublicAdmissionPage } from "@/features/admissions/PublicAdmissionPage";
import { StudentSectionAdmissionDetailPage } from "@/features/admissions/StudentSectionAdmissionDetailPage";
import { StudentSectionAdmissionListPage } from "@/features/admissions/StudentSectionAdmissionListPage";
import { StudentSectionDashboardPage } from "@/features/admissions/StudentSectionDashboardPage";
import { DepartmentDetailsPage } from "@/features/departments/DepartmentDetailsPage";
import { DepartmentListPage } from "@/features/departments/DepartmentListPage";
import { CreateStudentSectionStaffPage } from "@/features/staff/CreateStudentSectionStaffPage";
import { CreateFeeSectionStaffPage } from "@/features/staff/CreateFeeSectionStaffPage";
import { FeeStructureDetailsPage, FeeStructureFormPage, FeeStructureListPage } from "@/features/fees/FeeStructurePages";
import { MyFeeTransactionsPage, MyPaymentsPage, StudentFeesPage, SubmitPaymentPage } from "@/features/fees/StudentFeePages";
import { FeeAccountDetailsPage, FeeAccountsPage, FeeSectionDashboardPage, PaymentDetailsPage, PaymentsPage } from "@/features/fees/FeeSectionPages";
import { StaffListPage } from "@/features/staff/StaffListPage";
import { StudentAdmissionPage } from "@/features/student/StudentAdmissionPage";
import { AdminStudentListPage } from "@/features/student/AdminStudentListPage";
import { StudentDashboardPage } from "@/features/student/StudentDashboardPage";
import { StudentProfilePage } from "@/features/student/StudentProfilePage";
import { CreatePrincipalPage } from "@/features/users/CreatePrincipalPage";
import { UserDetailsPage } from "@/features/users/UserDetailsPage";
import { UserListPage } from "@/features/users/UserListPage";
import { ROLES, ROUTES, defaultRouteForRoles } from "@/lib/constants";
import { ForbiddenPage } from "@/pages/ForbiddenPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ServerErrorPage } from "@/pages/ServerErrorPage";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { RoleRoute } from "@/routes/RoleRoute";

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  return <Navigate to={isAuthenticated ? defaultRouteForRoles(user?.roles) : ROUTES.login} replace />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path={ROUTES.login} element={<LoginPage />} />
      <Route path={ROUTES.publicAdmission} element={<PublicAdmissionPage />} />
      <Route path={ROUTES.forbidden} element={<ForbiddenPage />} />
      <Route path={ROUTES.serverError} element={<ServerErrorPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path={ROUTES.changePassword} element={<ChangePasswordPage />} />
        <Route element={<DashboardLayout />}>
          <Route path={ROUTES.profile} element={<ProfilePage />} />

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN]} />}>
            <Route path={ROUTES.colleges} element={<CollegeListPage />} />
            <Route path="/colleges/:id" element={<CollegeDetailsPage />} />
            <Route path={ROUTES.users} element={<UserListPage />} />
            <Route path="/users/:id" element={<UserDetailsPage />} />
            <Route path={ROUTES.createPrincipal} element={<CreatePrincipalPage />} />
            <Route path={ROUTES.students} element={<AdminStudentListPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL]} />}>
            <Route path={ROUTES.dashboard} element={<DashboardPage />} />
            <Route path={ROUTES.departments} element={<DepartmentListPage />} />
            <Route path="/departments/:id" element={<DepartmentDetailsPage />} />
            <Route path={ROUTES.staff} element={<StaffListPage />} />
            <Route path={ROUTES.createStudentSectionStaff} element={<CreateStudentSectionStaffPage />} />
            <Route path={ROUTES.createFeeSectionStaff} element={<CreateFeeSectionStaffPage />} />
            <Route path={ROUTES.feeStructures} element={<FeeStructureListPage />} />
            <Route path="/fee-structures/create" element={<FeeStructureFormPage />} />
            <Route path="/fee-structures/:id" element={<FeeStructureDetailsPage />} />
            <Route path={ROUTES.principalReviewReady} element={<PrincipalReviewQueuePage />} />
            <Route path="/principal/admissions/:admissionId" element={<PrincipalAdmissionDetailPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL, ROLES.STUDENT_SECTION]} />}>
            <Route path={ROUTES.studentSectionDashboard} element={<StudentSectionDashboardPage />} />
            <Route path={ROUTES.studentSectionAdmissions} element={<StudentSectionAdmissionListPage />} />
            <Route path="/student-section/admissions/:admissionId" element={<StudentSectionAdmissionDetailPage />} />
            <Route path="/student-section/admissions/:admissionId/print" element={<AdmissionPrintPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.STUDENT]} />}>
            <Route path={ROUTES.studentDashboard} element={<StudentDashboardPage />} />
            <Route path={ROUTES.studentProfile} element={<StudentProfilePage />} />
            <Route path={ROUTES.studentAdmission} element={<StudentAdmissionPage />} />
            <Route path={ROUTES.studentFees} element={<StudentFeesPage />} />
            <Route path={ROUTES.studentPayments} element={<MyPaymentsPage />} />
            <Route path="/student/fees/payments/new" element={<SubmitPaymentPage />} />
            <Route path="/student/fees/transactions" element={<MyFeeTransactionsPage />} />
          </Route>
          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL, ROLES.FEE_SECTION]} />}>
            <Route path={ROUTES.feeSectionDashboard} element={<FeeSectionDashboardPage />} />
            <Route path={ROUTES.feeAccounts} element={<FeeAccountsPage />} />
            <Route path="/fee-section/fee-accounts/:feeAccountId" element={<FeeAccountDetailsPage />} />
            <Route path={ROUTES.feePayments} element={<PaymentsPage />} />
            <Route path="/fee-section/payments/:paymentId" element={<PaymentDetailsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
