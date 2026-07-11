import { CheckCircle2, FileText, Printer, Search, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/admissions/api";
import type { AdmissionStatus } from "@/features/admissions/types";

const cards: Array<{
  label: string;
  status?: AdmissionStatus;
  icon: typeof FileText;
  color: string;
}> = [
  {
    label: "Submitted Admissions",
    status: "SUBMITTED",
    icon: FileText,
    color: "bg-blue-50 text-blue-600",
  },
  {
    label: "Under Review",
    status: "STUDENT_SECTION_REVIEW_PENDING",
    icon: Search,
    color: "bg-amber-50 text-amber-600",
  },
  {
    label: "Approved by Student Section",
    status: "STUDENT_SECTION_APPROVED",
    icon: CheckCircle2,
    color: "bg-emerald-50 text-emerald-600",
  },
  {
    label: "Rejected by Student Section",
    status: "STUDENT_SECTION_REJECTED",
    icon: XCircle,
    color: "bg-red-50 text-red-600",
  },
  {
    label: "Printed Forms",
    status: "STUDENT_SECTION_APPROVED",
    icon: Printer,
    color: "bg-indigo-50 text-indigo-600",
  },
];

export function StudentSectionDashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    Promise.all(
      cards.map((card) =>
        api
          .searchStudentSectionAdmissions({ status: card.status, size: 1 })
          .then((page) => [card.label, page.totalElements] as const),
      ),
    )
      .then((pairs) => setStats(Object.fromEntries(pairs)))
      .catch((err) => toast.error(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, []);
  return (
    <div className="page-container">
      <div className="rounded-3xl bg-gradient-to-r from-brand-700 to-indigo-800 p-7 text-white">
        <p className="text-blue-100">Student Section</p>
        <h1 className="mt-2 text-3xl font-bold">Welcome, {user?.fullName}</h1>
        <p className="mt-2 text-blue-100">{user?.collegeName}</p>
      </div>
      {loading ? (
        <Loader label="Loading dashboard..." />
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          {cards.map(({ label, icon: Icon, color }) => (
            <Card key={label} className="p-5">
              <div className={`mb-4 grid h-11 w-11 place-items-center rounded-xl ${color}`}>
                <Icon className="h-5 w-5" />
              </div>
              <p className="text-sm text-slate-500">{label}</p>
              <p className="mt-1 text-2xl font-bold">{stats[label] ?? 0}</p>
            </Card>
          ))}
        </div>
      )}
      <Button
        className="mt-6"
        onClick={() => window.location.assign("/student-section/admissions")}
      >
        View Admissions
      </Button>
    </div>
  );
}
