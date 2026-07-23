import {
  ArrowRight,
  BarChart3,
  Bell,
  CalendarDays,
  FileText,
  GraduationCap,
  UserRound,
  WalletCards,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as admissionsApi from "@/features/admissions/api";
import { AdmissionStatusBadge, statusExplanation } from "@/components/admissions/components";
import type { AdmissionResponse } from "@/features/admissions/types";
import { useStudentAcademicAccess } from "@/features/academics/StudentAcademicAccessContext";

export function StudentDashboardPage() {
  const { user } = useAuth();
  const { divisionAllocated } = useStudentAcademicAccess();
  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    admissionsApi
      .getMyAdmission()
      .then(setAdmission)
      .catch((err) => toast.error(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, []);
  if (loading) return <Loader label="Loading student dashboard..." />;
  return (
    <div className="page-container pb-10">
      <div className="mb-6">
        <p className="text-xs font-semibold text-slate-400">
          Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Student
        </p>
        <h1 className="mt-2 text-2xl font-bold">Student Dashboard</h1>
        <p className="mt-1 text-sm text-slate-500">
          Your personal admission and profile workspace.
        </p>
      </div>
      <section className="erp-welcome-banner p-7 sm:p-9">
        <div className="absolute -right-12 -top-20 h-64 w-64 rounded-full border-[32px] border-blue-500/30" />
        <div className="relative">
          <p className="text-sm text-blue-100">Student workspace</p>
          <h2 className="mt-2 text-3xl font-bold">Welcome back, {user?.fullName?.split(" ")[0]}</h2>
          {admission && (
            <p className="mt-3 text-sm text-blue-100">
              {admission.collegeName} · {admission.departmentName}
            </p>
          )}
        </div>
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.35fr_0.8fr]">
        {admission && (
          <Card className="overflow-hidden">
            <div className="flex items-center justify-between border-b px-6 py-5">
              <div>
                <h2 className="font-bold">Admission overview</h2>
                <p className="mt-1 text-xs text-slate-500">Your current application information</p>
              </div>
              <GraduationCap className="h-5 w-5 text-brand-600" />
            </div>
            <div className="p-6">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Current status
              </p>
              <div className="mt-3">
                <AdmissionStatusBadge status={admission.status} />
              </div>
              <p className="mt-3 text-sm leading-6 text-slate-500">
                {statusExplanation(admission.status)}
              </p>
              <div className="mt-6 grid gap-3 border-t pt-5 sm:grid-cols-2">
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-xs text-slate-400">Admission number</p>
                  <p className="mt-1 font-semibold">{admission.admissionNumber}</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-xs text-slate-400">Reference number</p>
                  <p className="mt-1 font-semibold">{admission.admissionReferenceNumber}</p>
                </div>
              </div>
            </div>
          </Card>
        )}
        <Card className="overflow-hidden">
          <div className="border-b px-6 py-5">
            <h2 className="font-bold">Quick access</h2>
            <p className="mt-1 text-xs text-slate-500">Open your available ERP services</p>
          </div>
          <div className="space-y-2 p-4">
            <QuickCard to={ROUTES.studentAdmission} icon={<FileText />} label="My Admission" />
            <QuickCard to={ROUTES.studentProfile} icon={<UserRound />} label="My Profile" />
            <QuickCard to={ROUTES.studentFees} icon={<WalletCards />} label="My Fees" />
            {divisionAllocated && (
              <>
                <QuickCard
                  to={ROUTES.studentTimetable}
                  icon={<CalendarDays />}
                  label="My Timetable"
                />
                <QuickCard
                  to={ROUTES.studentAttendance}
                  icon={<BarChart3 />}
                  label="My Attendance"
                />
                <QuickCard to={ROUTES.studentClass} icon={<GraduationCap />} label="My Class" />
                <QuickCard to={ROUTES.notices} icon={<Bell />} label="Notices" />
              </>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

function QuickCard({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <Link
      to={to}
      className="group flex items-center gap-3 rounded-xl p-3 transition hover:bg-slate-50"
    >
      <div className="grid h-11 w-11 place-items-center rounded-xl bg-blue-50 text-brand-600">
        {icon}
      </div>
      <b className="flex-1 text-sm">{label}</b>
      <ArrowRight className="h-4 w-4 text-slate-300 group-hover:text-brand-600" />
    </Link>
  );
}
