import { FileText, UserRound, WalletCards, CalendarDays } from "lucide-react";
import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import * as admissionsApi from "@/features/admissions/api";
import { AdmissionStatusBadge, statusExplanation } from "@/components/admissions/components";
import type { AdmissionResponse } from "@/features/admissions/types";

export function StudentDashboardPage() {
  const { user } = useAuth();
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
    <div className="page-container">
      <div className="rounded-3xl bg-gradient-to-r from-brand-700 to-indigo-800 p-7 text-white">
        <p className="text-blue-100">Student workspace</p>
        <h1 className="mt-2 text-3xl font-bold">Welcome, {user?.fullName}</h1>
        {admission && (
          <p className="mt-2 text-blue-100">
            {admission.collegeName} - {admission.departmentName}
          </p>
        )}
      </div>
      {admission && (
        <Card className="mt-6 p-6">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500">Current admission status</p>
              <div className="mt-2">
                <AdmissionStatusBadge status={admission.status} />
              </div>
              <p className="mt-2 text-sm text-slate-500">{statusExplanation(admission.status)}</p>
            </div>
            <div className="text-sm text-slate-600">
              <p>
                <b>Admission No:</b> {admission.admissionNumber}
              </p>
              <p>
                <b>Reference:</b> {admission.admissionReferenceNumber}
              </p>
            </div>
          </div>
        </Card>
      )}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <QuickCard to="/student/admission" icon={<FileText />} label="My Admission" />
        <QuickCard to="/student/profile" icon={<UserRound />} label="My Profile" />
        <DisabledCard icon={<WalletCards />} label="Fee Payment" />
        <DisabledCard icon={<CalendarDays />} label="Attendance" />
      </div>
    </div>
  );
}

function QuickCard({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <Link to={to}>
      <Card className="flex items-center gap-4 p-5 transition hover:border-brand-200 hover:bg-brand-50">
        <div className="text-brand-600">{icon}</div>
        <b>{label}</b>
      </Card>
    </Link>
  );
}
function DisabledCard({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <Card className="flex cursor-not-allowed items-center gap-4 p-5 text-slate-400">
      <div>{icon}</div>
      <b>{label}</b>
      <span className="ml-auto text-xs font-bold uppercase">Soon</span>
    </Card>
  );
}
