import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { defaultRouteForRoles } from "@/lib/constants";

export function RoleRoute({ roles }: { roles: string[] }) {
  const { initializing, isRole, user } = useAuth();

  // Never make an authorization decision while the secure session is still
  // being restored. This prevents a protected page from briefly redirecting
  // on the first navigation after login or a hard refresh.
  if (initializing || !user) {
    return <div className="grid min-h-64 place-items-center text-sm text-slate-500">Checking access…</div>;
  }

  return isRole(roles) ? <Outlet /> : (
    <Navigate to={defaultRouteForRoles(user?.roles)} replace />
  );
}
