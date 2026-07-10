import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import * as admissionsApi from "@/features/admissions/api";
import { AdmissionStatusBadge, DetailSection } from "@/features/admissions/components";
import type { AdmissionResponse } from "@/features/admissions/types";

export function StudentAdmissionPage() {
  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    admissionsApi.getMyAdmission().then(setAdmission).catch((err) => toast.error(handleApiError(err).message)).finally(() => setLoading(false));
  }, []);
  if (loading) return <Loader label="Loading admission..." />;
  if (!admission) return null;
  return (
    <div className="page-container space-y-5">
      <Card className="p-6"><div className="flex flex-wrap items-center justify-between gap-4"><div><h1 className="page-title">{admission.fullName}</h1><p className="page-subtitle">{admission.admissionReferenceNumber} - {admission.admissionNumber}</p></div><AdmissionStatusBadge status={admission.status} /></div>{admission.rejectionReason && <p className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-700">{admission.rejectionReason}</p>}</Card>
      <DetailSection title="Admission" rows={[["Academic Year", admission.academicYear], ["College", admission.collegeName], ["Department", admission.departmentName], ["Submitted", formatDate(admission.submittedAt)]]} />
      <DetailSection title="Personal Details" rows={[["Email", admission.email], ["Phone", admission.phone], ["Date of Birth", formatDate(admission.dateOfBirth)], ["Gender", admission.gender]]} />
      <DetailSection title="Address" rows={[["Address Line 1", admission.addressLine1], ["Address Line 2", admission.addressLine2], ["City", admission.city], ["State", admission.state], ["Pincode", admission.pincode]]} />
      <DetailSection title="Parent and Previous Academic" rows={[["Parent", admission.parentName], ["Parent Phone", admission.parentPhone], ["Parent Email", admission.parentEmail], ["Previous School", admission.previousSchoolName], ["Previous Class", admission.previousClassName], ["Previous Percentage", admission.previousPercentage]]} />
    </div>
  );
}
