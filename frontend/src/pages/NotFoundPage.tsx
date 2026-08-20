import { Navigate } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { defaultRouteForRoles, ROUTES } from "@/lib/constants";

export function NotFoundPage() {
  const { isAuthenticated, initializing, user } = useAuth();

  if (initializing) {
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-500">
        Restoring secure session…
      </div>
    );
  }

  if (user?.mustChangePassword) {
    return <Navigate to={ROUTES.changePassword} replace />;
  }

  return (
    <Navigate to={isAuthenticated ? defaultRouteForRoles(user?.roles) : ROUTES.login} replace />
  );
}
