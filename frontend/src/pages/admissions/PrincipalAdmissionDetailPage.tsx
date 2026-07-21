import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Textarea } from "@/components/common/Textarea";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { formatDate } from "@/lib/utils";
import {
  AdmissionStatusBadge,
  DetailSection,
  HistoryTimeline,
} from "@/components/admissions/components";
import { DetailedAdmissionView } from "@/components/admissions/DetailedAdmissionForm";
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
  const [decision, setDecision] = useState<"approve" | "reject" | null>(null);
  const [remarks, setRemarks] = useState("");
  const [saving, setSaving] = useState(false);
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
  const decide = async () => {
    if (!decision) return;
    if (decision === "reject" && remarks.trim().length < 5) {
      toast.error("Rejection reason must contain at least 5 characters");
      return;
    }
    setSaving(true);
    try {
      if (decision === "approve") {
        await api.principalApproveAdmission(id, remarks.trim() || undefined);
      } else {
        await api.principalRejectAdmission(id, remarks.trim());
      }
      toast.success(decision === "approve" ? "Admission approved" : "Admission rejected");
      setDecision(null);
      setRemarks("");
      await load();
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };
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
        {admission.status === "PRINCIPAL_REVIEW_PENDING" && (
          <div className="mt-5 space-y-3">
            <div className="flex gap-2">
              <Button onClick={() => setDecision("approve")}>Approve admission</Button>
              <Button variant="danger" onClick={() => setDecision("reject")}>
                Reject admission
              </Button>
            </div>
            {decision && (
              <div className="max-w-xl rounded-xl border bg-slate-50 p-4">
                <Textarea
                  label={
                    decision === "approve" ? "Approval remarks (optional)" : "Rejection reason"
                  }
                  value={remarks}
                  onChange={(event) => setRemarks(event.target.value)}
                />
                <div className="mt-3 flex gap-2">
                  <Button
                    variant={decision === "reject" ? "danger" : "primary"}
                    loading={saving}
                    onClick={decide}
                  >
                    Confirm {decision}
                  </Button>
                  <Button variant="secondary" onClick={() => setDecision(null)}>
                    Cancel
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </Card>
      <DetailedAdmissionView admission={admission} principal />
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
