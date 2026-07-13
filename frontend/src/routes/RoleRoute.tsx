import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { defaultRouteForRoles } from "@/lib/constants";

export function RoleRoute({ roles }: { roles: string[] }) {
  const { isRole, user } = useAuth();
  return isRole(roles) ? <Outlet /> : (
    <Navigate to={defaultRouteForRoles(user?.roles)} replace />
  );
}
