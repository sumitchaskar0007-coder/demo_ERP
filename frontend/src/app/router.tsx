import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useAuth } from "@/features/auth/authStore";
import { LoginPage } from "@/features/auth/LoginPage";
import { ProfilePage } from "@/features/auth/ProfilePage";
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
import { StaffListPage } from "@/features/staff/StaffListPage";
import { StudentAdmissionPage } from "@/features/student/StudentAdmissionPage";
import { StudentDashboardPage } from "@/features/student/StudentDashboardPage";
import { StudentProfilePage } from "@/features/student/StudentProfilePage";
import { CreatePrincipalPage } from "@/features/users/CreatePrincipalPage";
import { EditPrincipalPage } from "@/features/users/EditPrincipalPage";
import { UserDetailsPage } from "@/features/users/UserDetailsPage";
import { UserListPage } from "@/features/users/UserListPage";
import { NoticesPage } from "@/features/notices/NoticesPage";
import { AcademicSetupPage } from "@/features/academics/AcademicSetupPage";
import { TimetablePage } from "@/features/academics/TimetablePage";
import { AttendancePage } from "@/features/academics/AttendancePage";
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
        <Route element={<DashboardLayout />}>
          <Route path={ROUTES.profile} element={<ProfilePage />} />
          <Route path={ROUTES.notices} element={<NoticesPage />} />
          <Route path={ROUTES.timetable} element={<TimetablePage />} />
          <Route path={ROUTES.attendance} element={<AttendancePage />} />

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.PRINCIPAL, ROLES.HOD]} />}>
            <Route path={ROUTES.academicSetup} element={<AcademicSetupPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN]} />}>
            <Route path={ROUTES.colleges} element={<CollegeListPage />} />
            <Route path="/colleges/:id" element={<CollegeDetailsPage />} />
            <Route path={ROUTES.users} element={<UserListPage />} />
            <Route path="/users/:id" element={<UserDetailsPage />} />
            <Route path={ROUTES.createPrincipal} element={<CreatePrincipalPage />} />
            <Route path={ROUTES.editPrincipal} element={<EditPrincipalPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL]} />}>
            <Route path={ROUTES.dashboard} element={<DashboardPage />} />
            <Route path={ROUTES.departments} element={<DepartmentListPage />} />
            <Route path="/departments/:id" element={<DepartmentDetailsPage />} />
            <Route path={ROUTES.staff} element={<StaffListPage />} />
            <Route path={ROUTES.createStudentSectionStaff} element={<CreateStudentSectionStaffPage />} />
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
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
