import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import * as admissionsApi from "@/features/admissions/api";
import { ROUTES } from "@/lib/constants";

export function StudentAdmissionGuard() {
  const location = useLocation();
  const [state, setState] = useState<"loading" | "complete" | "pending">("loading");
  useEffect(() => {
    let active = true;
    admissionsApi.getMyDetailedAdmission()
      .then((admission) => {
        const correctionRequired = admission.status === "STUDENT_SECTION_REJECTED";
        if (active) setState(admission.detailsCompletedAt && admission.status !== "STUDENT_DETAILS_PENDING" && !correctionRequired ? "complete" : "pending");
      })
      .catch(() => { if (active) setState("pending"); });
    return () => { active = false; };
  }, [location.pathname]);
  if (state === "loading") return <div className="grid min-h-[50vh] place-items-center text-sm text-slate-500">Checking admission form...</div>;
  if (state === "pending") return <Navigate to={ROUTES.studentAdmission} replace state={{ onboardingRequired: true }} />;
  return <Outlet/>;
}
