import { zodResolver } from "@hookform/resolvers/zod";
import { Pencil, X } from "lucide-react";
import { forwardRef, useCallback, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import { handleApiError } from "@/lib/handleApiError";
import { approveAdmissionSchema, rejectAdmissionSchema } from "@/lib/validators";
import { formatDate } from "@/lib/utils";
import {
  DetailedAdmissionForm,
  DetailedAdmissionView,
} from "@/components/admissions/DetailedAdmissionForm";
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
import type { AdmissionFeeSummaryResponse } from "@/features/fees/types";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";

export function StudentSectionAdmissionDetailPage() {
  const { isRole } = useAuth();
  const canManage = isRole([ROLES.STUDENT_SECTION]);
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [history, setHistory] = useState<AdmissionStatusHistoryResponse[]>([]);
  const [fees, setFees] = useState<AdmissionFeeSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [modal, setModal] = useState<"approve" | "reject" | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline, feeSummary] = await Promise.all([
        api.getStudentSectionAdmission(id),
        api.getAdmissionHistory(id),
        api.getStudentSectionAdmissionFees(id),
      ]);
      setAdmission(detail);
      setHistory(timeline);
      setFees(feeSummary);
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [id]);
  useEffect(() => {
    load();
  }, [load]);
  const quick = async () => {
    try {
      await api.startAdmissionReview(id);
      toast.success("Review started");
      await load();
    } catch (err) {
      toast.error(handleApiError(err).message);
    }
  };
  if (loading) return <Loader label="Loading admission detail..." />;
  if (!admission) return null;
  const canVerify = canManage && admission.status === "STUDENT_SECTION_REVIEW_PENDING";
  const canChangeInformation =
    canManage &&
    Boolean(admission.detailsCompletedAt) &&
    ["SUBMITTED", "STUDENT_SECTION_REVIEW_PENDING"].includes(admission.status);
  const canApprove = canVerify && Boolean(admission.detailsCompletedAt) && admission.photoAvailable;
  const finishEditing = async () => {
    setEditing(false);
    await load();
  };
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
        <div className="mt-5 flex flex-wrap gap-2">
          {canManage && admission.status === "SUBMITTED" && admission.detailsCompletedAt && (
            <Button variant="secondary" onClick={quick}>
              Start Review
            </Button>
          )}
          {canChangeInformation && !editing && (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              <Pencil className="h-4 w-4" />
              Change Information
            </Button>
          )}
          {canChangeInformation && editing && (
            <Button variant="ghost" onClick={() => setEditing(false)}>
              <X className="h-4 w-4" />
              Cancel Changes
            </Button>
          )}
          {canApprove && <Button onClick={() => setModal("approve")}>Approve</Button>}
          {canVerify && (
            <Button variant="danger" onClick={() => setModal("reject")}>
              Reject
            </Button>
          )}
        </div>
      </Card>
      {canChangeInformation && editing ? (
        <DetailedAdmissionForm admission={admission} onSaved={finishEditing} />
      ) : (
        <DetailedAdmissionView admission={admission} />
      )}
      <DetailSection
        title="Admission"
        rows={[
          ["College", admission.collegeName],
          ["Department", admission.departmentName],
          ["Academic Year", admission.academicYear],
          ["Requested Category", admission.studentCategory],
          ["Submitted", formatDate(admission.submittedAt)],
        ]}
      />
      <DetailSection
        title="Student"
        rows={[
          ["Email", admission.email],
          ["Phone", admission.phone],
          ["Date of Birth", formatDate(admission.dateOfBirth)],
          ["Gender", admission.gender],
          [
            "Address",
            [admission.addressLine1, admission.city, admission.state, admission.pincode]
              .filter(Boolean)
              .join(", "),
          ],
        ]}
      />
      <DetailSection
        title="Parent and Previous Academic"
        rows={[
          ["Parent", admission.parentName],
          ["Parent Phone", admission.parentPhone],
          ["Parent Email", admission.parentEmail],
          ["Previous School", admission.previousSchoolName],
          ["Previous Class", admission.previousClassName],
          ["Previous Percentage", admission.previousPercentage],
        ]}
      />
      <DetailSection
        title="Verification and Print"
        rows={[
          ["Verified At", formatDate(admission.studentSectionVerifiedAt)],
          ["Verified By", admission.studentSectionVerifiedByName],
          ["Remarks", admission.studentSectionRemarks],
          ["Rejected At", formatDate(admission.studentSectionRejectedAt)],
          ["Rejection Reason", admission.rejectionReason],
          ["Print Count", admission.printCount],
          ["Last Printed", formatDate(admission.lastPrintedAt)],
          ["Last Printed By", admission.lastPrintedByName],
        ]}
      />
      <DetailSection
        title="Fee and Scholarship"
        rows={
          fees?.account
            ? [
                ["Total Fee", money(fees.account.totalFee)],
                ["Paid Amount", money(fees.account.paidAmount)],
                ["Scholarship Reduction", money(fees.account.scholarshipAmount)],
                [
                  "Payable After Scholarship",
                  money(fees.account.totalFee - fees.account.scholarshipAmount),
                ],
                ["Remaining Amount", money(fees.account.remainingAmount)],
                ["Fee Status", fees.account.status.replaceAll("_", " ")],
              ]
            : [["Fee Account", "Not generated yet"]]
        }
      />
      <HistoryTimeline history={history} />
      <ActionModal
        modal={modal}
        onClose={() => setModal(null)}
        id={id}
        requestedCategory={admission.studentCategory}
        admission={admission}
        reload={load}
      />
    </div>
  );
}

function money(value: number) {
  return `₹${Number(value).toLocaleString("en-IN")}`;
}

function ActionModal({
  modal,
  onClose,
  id,
  requestedCategory,
  admission,
  reload,
}: {
  modal: "approve" | "reject" | null;
  onClose: () => void;
  id: number;
  requestedCategory: StudentSectionAdmissionResponse["studentCategory"];
  admission: StudentSectionAdmissionResponse;
  reload: () => Promise<void>;
}) {
  const approveForm = useForm<z.infer<typeof approveAdmissionSchema>>({
    resolver: zodResolver(approveAdmissionSchema),
    defaultValues: {
      studentCategory: requestedCategory,
      photoVerified: false,
      tenthMarksheetVerified: false,
      twelfthMarksheetVerified: false,
      provisionalCertificateVerified: false,
      leavingCertificateVerified: false,
      nationalityCertificateVerified: false,
      domicileCertificateVerified: false,
      aadhaarCardVerified: false,
      graduationPgCertificateVerified: false,
      migrationCertificateVerified: false,
      gapAffidavitVerified: false,
      casteCertificateVerified: false,
      incomeProofVerified: false,
      nameChangeCertificateVerified: false,
      remarks: "",
    },
  });
  const rejectForm = useForm<z.infer<typeof rejectAdmissionSchema>>({
    resolver: zodResolver(rejectAdmissionSchema),
    defaultValues: { rejectionReason: "" },
  });
  const submit = async (values: Record<string, string | boolean>) => {
    try {
      if (modal === "approve")
        await api.approveAdmission(id, {
          studentCategory:
            values.studentCategory as StudentSectionAdmissionResponse["studentCategory"],
          photoVerified: Boolean(values.photoVerified),
          tenthMarksheetVerified: Boolean(values.tenthMarksheetVerified),
          twelfthMarksheetVerified: Boolean(values.twelfthMarksheetVerified),
          leavingCertificateVerified: Boolean(values.leavingCertificateVerified),
          aadhaarCardVerified: Boolean(values.aadhaarCardVerified),
          graduationPgCertificateVerified: Boolean(values.graduationPgCertificateVerified),
          migrationCertificateVerified: Boolean(values.migrationCertificateVerified),
          gapAffidavitVerified: Boolean(values.gapAffidavitVerified),
          casteCertificateVerified: Boolean(values.casteCertificateVerified),
          incomeProofVerified: Boolean(values.incomeProofVerified),
          nameChangeCertificateVerified: Boolean(values.nameChangeCertificateVerified),
          remarks: String(values.remarks || ""),
        });
      if (modal === "reject")
        await api.rejectAdmission(id, { rejectionReason: String(values.rejectionReason) });
      toast.success("Admission updated");
      onClose();
      await reload();
    } catch (err) {
      toast.error(handleApiError(err).message);
    }
  };
  return (
    <Modal
      open={Boolean(modal)}
      onClose={onClose}
      title={modal === "reject" ? "Reject admission" : "Approve admission"}
    >
      {modal === "approve" && (
        <form
          onSubmit={approveForm.handleSubmit(submit, (errors) => {
            const messages = Object.values(errors)
              .map((error) => error?.message)
              .filter((message): message is string => typeof message === "string");
            toast.error(messages[0] ?? "Please verify every available required document");
          })}
          className="space-y-4"
        >
          <Select
            label="Verified student category"
            options={[
              { label: "Open", value: "OPEN" },
              { label: "OBC", value: "OBC" },
              { label: "SC", value: "SC" },
              { label: "ST", value: "ST" },
              { label: "SBC", value: "SBC" },
              { label: "VJNT", value: "VJNT" },
              { label: "EWS", value: "EWS" },
              { label: "Other", value: "OTHER" },
            ]}
            {...approveForm.register("studentCategory")}
            error={approveForm.formState.errors.studentCategory?.message}
          />
          <div className="space-y-3 rounded-xl border bg-slate-50 p-4">
            <div>
              <p className="text-sm font-bold">Required document verification</p>
              <p className="mt-1 text-xs text-slate-500">
                Verify the passport photo and all 7 required documents.
              </p>
            </div>
            <VerificationCheckbox
              label="Passport photo"
              available={admission.photoAvailable}
              {...approveForm.register("photoVerified")}
            />
            <VerificationCheckbox
              label="10th marksheet"
              available={admission.tenthMarksheetAvailable}
              {...approveForm.register("tenthMarksheetVerified")}
            />
            <VerificationCheckbox
              label="12th marksheet"
              available={admission.twelfthMarksheetAvailable}
              {...approveForm.register("twelfthMarksheetVerified")}
            />
            <VerificationCheckbox
              label="Provisional certificate"
              available={admission.uploadedDocuments.includes("PROVISIONAL_CERTIFICATE")}
              {...approveForm.register("provisionalCertificateVerified")}
            />
            <VerificationCheckbox
              label="Transfer / leaving certificate"
              available={admission.leavingCertificateAvailable}
              {...approveForm.register("leavingCertificateVerified")}
            />
            <VerificationCheckbox
              label="Nationality certificate"
              available={admission.uploadedDocuments.includes("NATIONALITY_CERTIFICATE")}
              {...approveForm.register("nationalityCertificateVerified")}
            />
            <VerificationCheckbox
              label="Domicile certificate"
              available={admission.uploadedDocuments.includes("DOMICILE_CERTIFICATE")}
              {...approveForm.register("domicileCertificateVerified")}
            />
            <VerificationCheckbox
              label="Aadhaar card"
              available={admission.aadhaarCardAvailable}
              {...approveForm.register("aadhaarCardVerified")}
            />
          </div>
          <div className="space-y-3 rounded-xl border bg-slate-50 p-4">
            <p className="text-sm font-bold">Optional document verification</p>
            <VerificationCheckbox
              label="Graduation / PG certificate"
              available={admission.graduationPgCertificateAvailable}
              {...approveForm.register("graduationPgCertificateVerified")}
            />
            <VerificationCheckbox
              label="Migration certificate"
              available={admission.migrationCertificateAvailable}
              {...approveForm.register("migrationCertificateVerified")}
            />
            <VerificationCheckbox
              label="Gap affidavit"
              available={admission.gapAffidavitAvailable}
              {...approveForm.register("gapAffidavitVerified")}
            />
            <VerificationCheckbox
              label="Caste certificate"
              available={admission.casteCertificateAvailable}
              {...approveForm.register("casteCertificateVerified")}
            />
            <VerificationCheckbox
              label="Income proof"
              available={admission.incomeProofAvailable}
              {...approveForm.register("incomeProofVerified")}
            />
            <VerificationCheckbox
              label="Name-change certificate"
              available={admission.nameChangeCertificateAvailable}
              {...approveForm.register("nameChangeCertificateVerified")}
            />
          </div>
          <Textarea
            label="Remarks"
            {...approveForm.register("remarks")}
            error={approveForm.formState.errors.remarks?.message}
          />
          <Button type="submit" loading={approveForm.formState.isSubmitting}>
            Approve
          </Button>
        </form>
      )}
      {modal === "reject" && (
        <form onSubmit={rejectForm.handleSubmit(submit)} className="space-y-4">
          <Textarea
            label="Rejection reason"
            {...rejectForm.register("rejectionReason")}
            error={rejectForm.formState.errors.rejectionReason?.message}
          />
          <Button type="submit" variant="danger" loading={rejectForm.formState.isSubmitting}>
            Reject
          </Button>
        </form>
      )}
    </Modal>
  );
}

const VerificationCheckbox = forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement> & { label: string; available: boolean }
>(function VerificationCheckbox({ label, available, ...inputProps }, ref) {
  return (
    <label className="flex items-center justify-between gap-3 text-sm">
      <span>{label}</span>
      <span className="flex items-center gap-2">
        <span className={available ? "text-emerald-700" : "text-rose-600"}>
          {available ? "Available" : "Missing"}
        </span>
        <input ref={ref} type="checkbox" disabled={!available} {...inputProps} />
      </span>
    </label>
  );
});
