import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStudentAdmissionAccess } from "@/features/admissions/api";
import { useAuth } from "@/features/auth/authStore";
import { ROLES, ROUTES } from "@/lib/constants";

export function ProtectedRoute() {
  const { isAuthenticated, initializing, user } = useAuth();
  const location = useLocation();
  const isStudent = Boolean(user?.roles?.includes(ROLES.STUDENT));
  const mustCheckStudentApproval = Boolean(user?.mustChangePassword && isStudent);
  const [studentApproved, setStudentApproved] = useState<boolean | null>(null);

  useEffect(() => {
    let active = true;

    const checkApproval = async () => {
      if (!mustCheckStudentApproval) {
        setStudentApproved(null);
        return;
      }
      try {
        const access = await getStudentAdmissionAccess();
        if (active) setStudentApproved(access.accessGranted);
      } catch {
        if (active) setStudentApproved(false);
      }
    };

    void checkApproval();
    const onVisible = () => {
      if (document.visibilityState === "visible") void checkApproval();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      active = false;
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [mustCheckStudentApproval]);

  if (initializing)
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-500">
        Restoring secure session…
      </div>
    );
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />;
  }
  if (mustCheckStudentApproval && studentApproved === null) {
    return (
      <div className="grid min-h-screen place-items-center text-sm text-slate-500">
        Checking admission approval…
      </div>
    );
  }
  if (mustCheckStudentApproval && !studentApproved) {
    return location.pathname === ROUTES.changePassword ? (
      <Navigate to={ROUTES.studentAdmission} replace />
    ) : (
      <Outlet />
    );
  }
  if (user?.mustChangePassword && location.pathname !== ROUTES.changePassword) {
    return <Navigate to={ROUTES.changePassword} replace />;
  }
  return <Outlet />;
}
