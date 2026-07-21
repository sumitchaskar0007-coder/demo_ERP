import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { ROUTES } from "@/lib/constants";

export function ProtectedRoute() {
  const { isAuthenticated, initializing } = useAuth();
  const location = useLocation();
  if (initializing)
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-500">
        Restoring secure session…
      </div>
    );
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}
