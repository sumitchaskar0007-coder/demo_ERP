import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";
import { ROUTES } from "@/lib/constants";
import { Link } from "react-router-dom";
import { Button } from "@/components/common/Button";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/dashboard/api";
export function RoleDashboardPage() {
  const { user } = useAuth(),
    [data, setData] = useState<api.DashboardData | null>(null);
  useEffect(() => {
    if (!user) return;
    const r = user.roles;
    const p = r.includes(ROLES.SUPER_ADMIN)
      ? api.getSuperAdminDashboard()
      : r.includes(ROLES.PRINCIPAL)
        ? api.getPrincipalDashboard()
        : r.includes(ROLES.STUDENT_SECTION)
          ? api.getStudentSectionDashboard()
          : r.includes(ROLES.FEE_SECTION)
            ? api.getFeeSectionDashboard()
            : r.includes(ROLES.HOD)
              ? api.getHodDashboard()
              : r.includes(ROLES.CLASS_TEACHER) || r.includes(ROLES.SUBJECT_TEACHER)
                ? api.getTeacherDashboard()
                : api.getStudentDashboard();
    p.then(setData).catch((e) => toast.error(handleApiError(e).message));
  }, [user]);
  if (!data) return <Loader label="Loading dashboard…" />;
  return (
    <div className="page-container">
      <div className="rounded-2xl bg-gradient-to-r from-blue-600 to-cyan-500 p-6 text-white">
        <h1 className="text-2xl font-bold">Welcome, {user?.fullName}</h1>
        <p className="mt-1 text-blue-50">Here is your role-specific ERP overview.</p>
      </div>
      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Object.entries(data)
          .filter(([, v]) => typeof v !== "object")
          .map(([k, v]) => (
            <Card className="p-5" key={k}>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                {k.replace(/([A-Z])/g, " $1")}
              </p>
              <p className="mt-2 text-2xl font-bold text-slate-900">{String(v)}</p>
            </Card>
          ))}
      </div>
      {user?.roles.includes(ROLES.PRINCIPAL) && (
        <Card className="mt-6 p-5">
          <h2 className="font-bold text-slate-900">Principal setup workflow</h2>
          <p className="mt-1 text-sm text-slate-500">Department → Course Year → Division → Staff → Class Teacher</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <Link to={ROUTES.departments}><Button variant="secondary">Create Department</Button></Link>
            <Link to={ROUTES.createCourseYear}><Button variant="secondary">Create Course Year</Button></Link>
            <Link to={ROUTES.createDivision}><Button variant="secondary">Create Division</Button></Link>
            <Link to={ROUTES.createStaff}><Button variant="secondary">Create Staff</Button></Link>
            <Link to={ROUTES.divisions}><Button>Assign Class Teacher</Button></Link>
          </div>
        </Card>
      )}
    </div>
  );
}
