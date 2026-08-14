import { useCallback, useEffect, useState } from "react";
import { AdmissionStatusBadge } from "@/components/admissions/components";
import {
  DetailedAdmissionForm,
  DetailedAdmissionView,
} from "@/components/admissions/DetailedAdmissionForm";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import * as admissionsApi from "@/features/admissions/api";
import * as feesApi from "@/features/fees/api";
import type {
  AdmissionDocumentRequirement,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import type { StudentFeeAccountResponse } from "@/features/fees/types";
import { handleApiError } from "@/lib/handleApiError";
import { formatIndianCurrency } from "@/lib/utils";
import { STUDENT_ADMISSION_CHANGED_EVENT } from "@/routes/StudentAdmissionGate";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/lib/constants";

const rejectedStatuses = new Set(["STUDENT_SECTION_REJECTED", "PRINCIPAL_REJECTED"]);
const studentSectionApprovedStatuses = new Set([
  "STUDENT_SECTION_APPROVED",
  "PRINCIPAL_REVIEW_PENDING",
  "PRINCIPAL_APPROVED",
]);

export function StudentAdmissionPage() {
  const navigate = useNavigate();
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [feeAccount, setFeeAccount] = useState<StudentFeeAccountResponse | null>(null);
  const [requirements, setRequirements] = useState<AdmissionDocumentRequirement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadAdmission = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const admissionResponse = await admissionsApi.getMyAdmission();
      setAdmission(admissionResponse);
      setRequirements(await admissionsApi.getMyAdmissionDocumentRequirements());
      if (
        admissionResponse.status === "SUBMITTED" &&
        Boolean(admissionResponse.detailsCompletedAt)
      ) {
        try {
          setFeeAccount(await feesApi.getMyFeeAccount());
        } catch {
          setFeeAccount(null);
        }
      } else {
        setFeeAccount(null);
      }
    } catch (requestError) {
      setError(handleApiError(requestError).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAdmission();
  }, [loadAdmission]);

  const afterSubmission = async () => {
    await loadAdmission();
    window.dispatchEvent(new Event(STUDENT_ADMISSION_CHANGED_EVENT));
    navigate(ROUTES.studentFees, { replace: true });
  };

  if (loading && !admission) return <Loader label="Loading admission form..." />;
  if (error && !admission) {
    return (
      <div className="page-container py-8">
        <Card className="p-6 text-center">
          <h1 className="text-lg font-bold">Unable to load your admission</h1>
          <p className="mt-2 text-sm text-red-700">{error}</p>
          <Button className="mt-4" onClick={() => void loadAdmission()}>
            Try again
          </Button>
        </Card>
      </div>
    );
  }
  if (!admission) return null;

  const rejected = rejectedStatuses.has(admission.status);
  const editable = !admission.detailsCompletedAt || rejected;
  const pending = admission.status === "STUDENT_SECTION_REVIEW_PENDING";
  const awaitingFeeVerification =
    admission.status === "SUBMITTED" && Boolean(admission.detailsCompletedAt);
  const admissionFeeLabel = feeAccount ? formatIndianCurrency(feeAccount.totalFee) : "configured";

  return (
    <div className="page-container space-y-5">
      <Card className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="page-title">Student admission</h1>
            <p className="page-subtitle">
              {admission.admissionReferenceNumber} · {admission.collegeName}
            </p>
          </div>
          <AdmissionStatusBadge status={admission.status} />
        </div>

        {!admission.detailsCompletedAt && (
          <p className="mt-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">
            Complete every required section and submit this form. Other student features remain
            locked until the Student Section approves your application.
          </p>
        )}
        {pending && (
          <p className="mt-4 rounded-xl bg-blue-50 p-4 text-sm text-blue-800">
            Your application is pending Student Section review. It is read-only while under review.
          </p>
        )}
        {awaitingFeeVerification && (
          <p className="mt-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">
            {`Your form is complete. Pay the ${admissionFeeLabel} admission form fee and wait for Fee Section verification. It will then move to Student Section review automatically.`}
          </p>
        )}
        {admission.detailsCompletedAt && !rejected && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            {!studentSectionApprovedStatuses.has(admission.status) ? (
              <Button onClick={() => navigate(ROUTES.studentFees)}>
                Open admission form payment
              </Button>
            ) : (
              <span />
            )}
            <Button variant="secondary" onClick={() => navigate("/student/admission/print")}>
              Download admission form
            </Button>
          </div>
        )}
        {rejected && (
          <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
            <p className="font-semibold">Your application was rejected.</p>
            <p className="mt-1">
              Reason: {admission.rejectionReason || "No rejection reason was provided."}
            </p>
            <p className="mt-2">Correct the saved information below and select Resubmit.</p>
          </div>
        )}
        {error && <p className="mt-4 text-sm text-red-700">{error}</p>}
      </Card>

      {editable ? (
        <DetailedAdmissionForm admission={admission} onSaved={afterSubmission} studentOwned />
      ) : (
        <DetailedAdmissionView
          admission={admission}
          documentRequirements={requirements}
          studentOwned
        />
      )}
    </div>
  );
}
