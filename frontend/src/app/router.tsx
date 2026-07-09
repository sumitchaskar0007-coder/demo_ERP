import { Navigate, Route, Routes } from "react-router-dom";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { useAuth } from "@/features/auth/authStore";
import { LoginPage } from "@/features/auth/LoginPage";
import { ProfilePage } from "@/features/auth/ProfilePage";
import { CollegeDetailsPage } from "@/features/colleges/CollegeDetailsPage";
import { CollegeListPage } from "@/features/colleges/CollegeListPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { DepartmentDetailsPage } from "@/features/departments/DepartmentDetailsPage";
import { DepartmentListPage } from "@/features/departments/DepartmentListPage";
import { CreatePrincipalPage } from "@/features/users/CreatePrincipalPage";
import { UserDetailsPage } from "@/features/users/UserDetailsPage";
import { UserListPage } from "@/features/users/UserListPage";
import { ROLES, ROUTES } from "@/lib/constants";
import { ForbiddenPage } from "@/pages/ForbiddenPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ServerErrorPage } from "@/pages/ServerErrorPage";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { RoleRoute } from "@/routes/RoleRoute";

function HomeRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? ROUTES.dashboard : ROUTES.login} replace />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path={ROUTES.login} element={<LoginPage />} />
      <Route path={ROUTES.forbidden} element={<ForbiddenPage />} />
      <Route path={ROUTES.serverError} element={<ServerErrorPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path={ROUTES.dashboard} element={<DashboardPage />} />
          <Route path={ROUTES.profile} element={<ProfilePage />} />

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN]} />}>
            <Route path={ROUTES.colleges} element={<CollegeListPage />} />
            <Route path="/colleges/:id" element={<CollegeDetailsPage />} />
            <Route path={ROUTES.users} element={<UserListPage />} />
            <Route path="/users/:id" element={<UserDetailsPage />} />
            <Route path={ROUTES.createPrincipal} element={<CreatePrincipalPage />} />
          </Route>

          <Route element={<RoleRoute roles={[ROLES.SUPER_ADMIN, ROLES.PRINCIPAL]} />}>
            <Route path={ROUTES.departments} element={<DepartmentListPage />} />
            <Route path="/departments/:id" element={<DepartmentDetailsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
