import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { ROUTES } from "@/lib/constants";

export function RoleRoute({ roles }: { roles: string[] }) {
  const { isRole } = useAuth();
  return isRole(roles) ? <Outlet /> : <Navigate to={ROUTES.forbidden} replace />;
}
