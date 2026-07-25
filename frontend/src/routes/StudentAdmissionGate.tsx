import { useCallback, useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { getStudentAdmissionAccess } from "@/features/admissions/api";
import type { StudentAdmissionAccessResponse } from "@/features/admissions/types";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { DASHBOARD_NAVIGATION_VISIBILITY_EVENT, ROLES, ROUTES } from "@/lib/constants";

export const STUDENT_ADMISSION_CHANGED_EVENT = "student-admission:changed";

export function StudentAdmissionGate() {
  const { user } = useAuth();
  const location = useLocation();
  const isStudent = Boolean(user?.roles.includes(ROLES.STUDENT));
  const [access, setAccess] = useState<StudentAdmissionAccessResponse | null>(null);
  const [loading, setLoading] = useState(isStudent);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    if (!isStudent) return;
    setLoading(true);
    setError("");
    try {
      setAccess(await getStudentAdmissionAccess());
    } catch (requestError) {
      setError(handleApiError(requestError).message);
    } finally {
      setLoading(false);
    }
  }, [isStudent]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    window.dispatchEvent(
      new CustomEvent(DASHBOARD_NAVIGATION_VISIBILITY_EVENT, {
        detail: !isStudent || Boolean(access?.accessGranted),
      }),
    );
  }, [access?.accessGranted, isStudent]);

  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === "visible") void refresh();
    };
    window.addEventListener(STUDENT_ADMISSION_CHANGED_EVENT, refresh);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.removeEventListener(STUDENT_ADMISSION_CHANGED_EVENT, refresh);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [refresh]);

  if (!isStudent) return <Outlet />;
  if (loading && !access) return <Loader label="Checking admission status..." />;
  if (error && !access) {
    return (
      <div className="page-container py-8">
        <Card className="p-6 text-center">
          <h1 className="text-lg font-bold text-slate-900">Unable to check admission status</h1>
          <p className="mt-2 text-sm text-red-700">{error}</p>
          <button
            className="mt-4 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void refresh()}
          >
            Try again
          </button>
        </Card>
      </div>
    );
  }
  if (location.pathname === ROUTES.studentAdmission || access?.accessGranted) return <Outlet />;
  return <Navigate to={ROUTES.studentAdmission} replace state={{ from: location.pathname }} />;
}
