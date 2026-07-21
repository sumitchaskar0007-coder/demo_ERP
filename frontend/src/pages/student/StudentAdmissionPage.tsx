import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, CheckCircle2, ClipboardList } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Loader } from "@/components/common/Loader";
import { AdmissionStatusBadge } from "@/components/admissions/components";
import { DetailedAdmissionForm, DetailedAdmissionView } from "@/components/admissions/DetailedAdmissionForm";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "@/features/admissions/api";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";

export function StudentAdmissionPage() {
  const navigate = useNavigate();
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    try { setAdmission(await api.getMyDetailedAdmission()); }
    catch (error) { toast.error(handleApiError(error).message); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);
  if (loading) return <Loader label="Loading your admission form..." />;
  if (!admission) return null;
  const correctionRequired = admission.status === "STUDENT_SECTION_REJECTED";
  const pending = correctionRequired || admission.status === "STUDENT_DETAILS_PENDING" || !admission.detailsCompletedAt;
  return <div className="page-container space-y-5">
    <Card className="overflow-hidden border-0 bg-gradient-to-r from-indigo-950 via-violet-900 to-indigo-800 p-7 text-white shadow-xl">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-center">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-bold"><ClipboardList className="h-4 w-4"/>Student admission onboarding</span>
          <h1 className="mt-4 text-3xl font-bold">{correctionRequired ? "Correct and resubmit your admission form" : pending ? "Complete your detailed admission form" : "Admission form submitted"}</h1>
          <p className="mt-2 max-w-2xl text-sm text-indigo-100">{correctionRequired ? "Review the rejection reason, correct your details or documents, and submit the form again." : pending ? "Fill every required field and upload all required documents. Your dashboard unlocks after submission." : "Your form is now available to Student Section for document verification."}</p>
        </div>
        <AdmissionStatusBadge status={admission.status}/>
      </div>
    </Card>
    {correctionRequired && <Card className="border-amber-300 bg-amber-50 p-5">
      <div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 h-6 w-6 shrink-0 text-amber-600"/><div><p className="font-bold text-amber-950">Changes requested by Student Section</p><p className="mt-1 whitespace-pre-wrap text-sm text-amber-800">{admission.rejectionReason || "Please correct the admission form and submitted documents."}</p></div></div>
    </Card>}
    {pending ? <DetailedAdmissionForm admission={admission} onSaved={load}/> : <>
      <Card className="border-emerald-200 bg-emerald-50 p-5">
        <div className="flex items-start gap-3"><CheckCircle2 className="mt-0.5 h-6 w-6 text-emerald-600"/><div><p className="font-bold text-emerald-900">Detailed form submitted successfully</p><p className="mt-1 text-sm text-emerald-700">Student Section will verify the uploaded documents and continue your admission to the Fee Section.</p></div></div>
        <Button className="mt-4" onClick={() => navigate(ROUTES.studentDashboard)}>Continue to dashboard</Button>
      </Card>
      <DetailedAdmissionView admission={admission}/>
    </>}
  </div>;
}
