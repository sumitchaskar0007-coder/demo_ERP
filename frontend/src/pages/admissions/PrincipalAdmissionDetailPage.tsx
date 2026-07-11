import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import {
  AdmissionStatusBadge,
  DetailSection,
  HistoryTimeline,
} from "@/components/admissions/components";
import * as api from "@/features/admissions/api";
import type {
  AdmissionStatusHistoryResponse,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";

export function PrincipalAdmissionDetailPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [history, setHistory] = useState<AdmissionStatusHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline] = await Promise.all([
        api.getPrincipalAdmission(id),
        api.getPrincipalAdmissionHistory(id),
      ]);
      setAdmission(detail);
      setHistory(timeline);
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [id]);
  useEffect(() => {
    load();
  }, [load]);
  if (loading) return <Loader label="Loading principal admission detail..." />;
  if (!admission) return null;
  return (
    <div className="page-container space-y-5">
      <Card className="p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="page-title">{admission.fullName}</h1>
            <p className="page-subtitle">
              {admission.admissionReferenceNumber} - {admission.admissionNumber}
            </p>
          </div>
          <AdmissionStatusBadge status={admission.status} />
        </div>
        <p className="mt-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-800">
          Final approval will be available in the next module.
        </p>
      </Card>
      <DetailSection
        title="Admission"
        rows={[
          ["College", admission.collegeName],
          ["Department", admission.departmentName],
          ["Academic Year", admission.academicYear],
          ["Submitted", formatDate(admission.submittedAt)],
        ]}
      />
      <DetailSection
        title="Student and Parent"
        rows={[
          ["Email", admission.email],
          ["Phone", admission.phone],
          ["Parent", admission.parentName],
          ["Parent Phone", admission.parentPhone],
          ["Previous School", admission.previousSchoolName],
          ["Previous Percentage", admission.previousPercentage],
        ]}
      />
      <DetailSection
        title="Student Section Verification and Print"
        rows={[
          ["Verified At", formatDate(admission.studentSectionVerifiedAt)],
          ["Verified By", admission.studentSectionVerifiedByName],
          ["Remarks", admission.studentSectionRemarks],
          ["Print Count", admission.printCount],
          ["Last Printed", formatDate(admission.lastPrintedAt)],
          ["Last Printed By", admission.lastPrintedByName],
        ]}
      />
      <HistoryTimeline history={history} />
    </div>
  );
}
