import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { ROUTES } from "@/lib/constants";

export function ProtectedRoute() {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />;
  }
  if (user?.mustChangePassword && location.pathname !== ROUTES.changePassword) {
    return <Navigate to={ROUTES.changePassword} replace />;
  }
  return <Outlet />;
}
