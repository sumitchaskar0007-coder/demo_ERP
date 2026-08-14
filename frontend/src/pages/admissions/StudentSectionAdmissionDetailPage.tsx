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
import { formatDate, formatIndianCurrency } from "@/lib/utils";
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
  AdmissionDocumentRequirement,
  AdmissionDocumentCustody,
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
  const [requirements, setRequirements] = useState<AdmissionDocumentRequirement[]>([]);
  const [custody, setCustody] = useState<AdmissionDocumentCustody[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [modal, setModal] = useState<"approve" | "reject" | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline, feeSummary, configuredDocuments, custodyRows] = await Promise.all([
        api.getStudentSectionAdmission(id),
        api.getAdmissionHistory(id),
        api.getStudentSectionAdmissionFees(id),
        api.getAdmissionDocumentRequirements(id),
        api.getDocumentCustody(id),
      ]);
      setAdmission(detail);
      setHistory(timeline);
      setFees(feeSummary);
      setRequirements(configuredDocuments.filter((item) => item.active));
      setCustody(custodyRows);
    } catch (err) {
      toast.error(handleApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [id]);
  useEffect(() => {
    load();
  }, [load]);
  if (loading) return <Loader label="Loading admission detail..." />;
  if (!admission) return null;
  const canVerify = canManage && admission.status === "STUDENT_SECTION_REVIEW_PENDING";
  const admissionFormFeeVerified = Boolean(
    fees?.account?.admissionFeeAccount &&
      Number(fees.account.paidAmount) >= Number(fees.account.minimumAmountForAdmission),
  );
  const admissionFeeLabel = fees?.account
    ? formatIndianCurrency(fees.account.totalFee)
    : "configured";
  const canChangeInformation =
    canManage &&
    Boolean(admission.detailsCompletedAt) &&
    admission.status === "STUDENT_SECTION_REVIEW_PENDING";
  const canApprove =
    canVerify &&
    admissionFormFeeVerified &&
    Boolean(admission.detailsCompletedAt) &&
    admission.photoAvailable;
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
        {canVerify && !admissionFormFeeVerified && (
          <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            {`Approval is locked until Fee Section verifies the ${admissionFeeLabel} admission form fee.`}
          </p>
        )}
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
          [
            "Requested Category",
            admission.studentCategory === "OTHER" && admission.customCategoryName
              ? `OTHER - ${admission.customCategoryName}`
              : admission.studentCategory,
          ],
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
                ["Credit / Refund Due", money(fees.account.creditAmount)],
                [
                  "Scholarship Status",
                  fees.account.scholarshipRemoved
                    ? "Removed by Principal"
                    : Number(fees.account.scholarshipAmount) > 0
                      ? "Applied"
                      : "Not configured",
                ],
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
        requirements={requirements}
        reload={load}
      />
      {custody.length > 0 && (
        <Card className="p-6">
          <h2 className="text-lg font-bold">Physical document custody</h2>
          <div className="mt-4 space-y-3">
            {custody.map((item) => (
              <div
                key={item.documentType}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3"
              >
                <div>
                  <p className="font-semibold">
                    {requirements.find((r) => r.documentKey === item.documentType)?.documentName ??
                      item.documentType}
                  </p>
                  <p className="text-xs text-slate-500">
                    {[item.originalReceived && "Original", item.xeroxReceived && "Xerox"]
                      .filter(Boolean)
                      .join(" + ")}
                  </p>
                </div>
                <label className="flex items-center gap-2 text-sm font-semibold">
                  <input
                    type="checkbox"
                    checked={item.returnedToStudent}
                    onChange={async (e) => {
                      const remarks = e.target.checked
                        ? (window.prompt("Return remarks (optional)", item.returnRemarks ?? "") ??
                          "")
                        : "";
                      await api.markDocumentReturned(
                        id,
                        item.documentType,
                        e.target.checked,
                        remarks,
                      );
                      await load();
                    }}
                  />{" "}
                  Returned to student
                </label>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}

function money(value: number) {
  return formatIndianCurrency(value);
}

function ActionModal({
  modal,
  onClose,
  id,
  requestedCategory,
  admission,
  requirements,
  reload,
}: {
  modal: "approve" | "reject" | null;
  onClose: () => void;
  id: number;
  requestedCategory: StudentSectionAdmissionResponse["studentCategory"];
  admission: StudentSectionAdmissionResponse;
  requirements: AdmissionDocumentRequirement[];
  reload: () => Promise<void>;
}) {
  const approveForm = useForm<z.infer<typeof approveAdmissionSchema>>({
    resolver: zodResolver(approveAdmissionSchema),
    defaultValues: {
      studentCategory: requestedCategory,
      customCategoryName: admission.customCategoryName || "",
      photoVerified: false,
      remarks: "",
    },
  });
  const [categoryOptions, setCategoryOptions] = useState<
    Array<{
      category: StudentSectionAdmissionResponse["studentCategory"];
      customCategoryName?: string | null;
      label: string;
    }>
  >([]);
  const selectedCategory = approveForm.watch("studentCategory");
  useEffect(() => {
    if (modal !== "approve") return;
    api
      .getPublicAdmissionCategories(admission.collegeCode, admission.departmentId, {
        gender: admission.gender,
        academicYear: admission.academicYear,
        courseYear: admission.courseYearDisplayName ?? undefined,
      })
      .then(setCategoryOptions)
      .catch(() => setCategoryOptions([]));
  }, [
    modal,
    admission.academicYear,
    admission.collegeCode,
    admission.courseYearDisplayName,
    admission.departmentId,
    admission.gender,
  ]);
  const rejectForm = useForm<z.infer<typeof rejectAdmissionSchema>>({
    resolver: zodResolver(rejectAdmissionSchema),
    defaultValues: { rejectionReason: "" },
  });
  const [documentCustody, setDocumentCustody] = useState<
    Record<string, { originalReceived: boolean; xeroxReceived: boolean }>
  >({});
  const submit = async (values: Record<string, string | boolean>) => {
    try {
      if (modal === "approve") {
        const selectedCustom = String(values.customCategoryName || "");
        const categoryConfigured = categoryOptions.some(
          (option) =>
            option.category === values.studentCategory &&
            (values.studentCategory !== "OTHER" ||
              option.customCategoryName?.toUpperCase() === selectedCustom.toUpperCase()),
        );
        if (!categoryConfigured) {
          throw new Error("Select an active fee category for this course year and gender");
        }
      }
      if (modal === "approve")
        if (requirements.some((item) => item.required && !documentCustody[item.documentKey])) {
          throw new Error("Select Original, Xerox, or both for every required document");
        }
      if (modal === "approve")
        await api.approveAdmission(id, {
          studentCategory:
            values.studentCategory as StudentSectionAdmissionResponse["studentCategory"],
          customCategoryName: String(values.customCategoryName || "") || undefined,
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
          documentCustody: requirements
            .filter((item) => documentCustody[item.documentKey])
            .map((item) => ({
              documentType: item.documentKey,
              ...documentCustody[item.documentKey],
            })),
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
            id="verified-student-category"
            label="Verified student category"
            options={[
              {
                label: categoryOptions.length
                  ? "Select category"
                  : "No active fee category configured",
                value: "",
              },
              ...categoryOptions
                .filter(
                  (option, index, all) =>
                    all.findIndex((candidate) => candidate.category === option.category) === index,
                )
                .map((option) => ({ label: option.category, value: option.category })),
            ]}
            value={selectedCategory}
            onChange={(event) => {
              approveForm.setValue(
                "studentCategory",
                event.target.value as StudentSectionAdmissionResponse["studentCategory"],
                { shouldValidate: true },
              );
              if (event.target.value !== "OTHER") {
                approveForm.setValue("customCategoryName", "", { shouldValidate: true });
              }
            }}
            error={approveForm.formState.errors.studentCategory?.message}
          />
          {selectedCategory === "OTHER" && (
            <Select
              id="verified-custom-category"
              label="Other category"
              options={[
                { label: "Select category", value: "" },
                ...categoryOptions
                  .filter((option) => option.category === "OTHER" && option.customCategoryName)
                  .filter(
                    (option, index, all) =>
                      all.findIndex(
                        (candidate) =>
                          candidate.customCategoryName?.toUpperCase() ===
                          option.customCategoryName?.toUpperCase(),
                      ) === index,
                  )
                  .map((option) => ({
                    label: option.customCategoryName!,
                    value: option.customCategoryName!,
                  })),
              ]}
              value={approveForm.watch("customCategoryName") ?? ""}
              onChange={(event) =>
                approveForm.setValue("customCategoryName", event.target.value, {
                  shouldValidate: true,
                })
              }
              error={approveForm.formState.errors.customCategoryName?.message}
            />
          )}
          <div className="space-y-3 rounded-xl border bg-slate-50 p-4">
            <div>
              <p className="text-sm font-bold">Required document verification</p>
              <p className="mt-1 text-xs text-slate-500">
                Only documents configured for this department are shown.
              </p>
            </div>
            <VerificationCheckbox
              label="Passport photo"
              available={admission.photoAvailable}
              {...approveForm.register("photoVerified")}
            />
            {requirements
              .filter((item) => item.required)
              .map((item) => (
                <CustodyChoice
                  key={item.documentKey}
                  requirement={item}
                  available={admission.uploadedDocuments.includes(item.documentKey)}
                  value={documentCustody[item.documentKey]}
                  onChange={(value) =>
                    setDocumentCustody((current) => ({ ...current, [item.documentKey]: value }))
                  }
                />
              ))}
          </div>
          <div className="space-y-3 rounded-xl border bg-slate-50 p-4">
            <p className="text-sm font-bold">Optional document verification</p>
            {requirements
              .filter((item) => !item.required)
              .map((item) => (
                <CustodyChoice
                  key={item.documentKey}
                  requirement={item}
                  available={admission.uploadedDocuments.includes(item.documentKey)}
                  value={documentCustody[item.documentKey]}
                  onChange={(value) =>
                    setDocumentCustody((current) => ({ ...current, [item.documentKey]: value }))
                  }
                />
              ))}
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

function CustodyChoice({
  requirement,
  available,
  value,
  onChange,
}: {
  requirement: AdmissionDocumentRequirement;
  available: boolean;
  value?: { originalReceived: boolean; xeroxReceived: boolean };
  onChange: (value: { originalReceived: boolean; xeroxReceived: boolean }) => void;
}) {
  const current = value ?? { originalReceived: false, xeroxReceived: false };
  return (
    <div className="rounded-lg border bg-white p-3 text-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="font-semibold">{requirement.documentName}</span>
        <span className={available ? "text-emerald-700" : "text-rose-600"}>
          {available ? "Uploaded" : "Missing"}
        </span>
      </div>
      {available && (
        <div className="mt-2 flex gap-5">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={current.originalReceived}
              onChange={(event) => onChange({ ...current, originalReceived: event.target.checked })}
            />
            Original
          </label>
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={current.xeroxReceived}
              onChange={(event) => onChange({ ...current, xeroxReceived: event.target.checked })}
            />
            Xerox
          </label>
        </div>
      )}
    </div>
  );
}
