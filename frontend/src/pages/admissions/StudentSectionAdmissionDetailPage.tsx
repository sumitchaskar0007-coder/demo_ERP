import { zodResolver } from "@hookform/resolvers/zod";
import { FileText } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { Modal } from "@/components/common/Modal";
import { Select } from "@/components/common/Select";
import { Textarea } from "@/components/common/Textarea";
import { handleApiError } from "@/lib/handleApiError";
import {
  approveAdmissionSchema,
  markAdmissionPrintedSchema,
  rejectAdmissionSchema,
} from "@/lib/validators";
import { formatDate } from "@/lib/utils";
import { DetailedAdmissionView } from "@/components/admissions/DetailedAdmissionForm";
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
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";

export function StudentSectionAdmissionDetailPage() {
  const { isRole } = useAuth();
  const canManage = isRole([ROLES.STUDENT_SECTION]);
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [history, setHistory] = useState<AdmissionStatusHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<"approve" | "reject" | "printed" | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline] = await Promise.all([
        api.getStudentSectionAdmission(id),
        api.getAdmissionHistory(id),
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
  const canVerify = canManage && ["SUBMITTED", "STUDENT_SECTION_REVIEW_PENDING"].includes(admission.status);
  const canApprove = canVerify && Boolean(admission.detailsCompletedAt) && admission.photoAvailable && admission.tenthMarksheetAvailable && admission.twelfthMarksheetAvailable && admission.leavingCertificateAvailable && admission.aadhaarCardAvailable;
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
          {canManage && admission.status === "SUBMITTED" && (
            <Button variant="secondary" onClick={quick}>
              Start Review
            </Button>
          )}
          {canApprove && <Button onClick={() => setModal("approve")}>Approve</Button>}
          {canVerify && (
            <Button variant="danger" onClick={() => setModal("reject")}>
              Reject
            </Button>
          )}
          {canManage && admission.status === "STUDENT_SECTION_APPROVED" && (
            <Link to={`/student-section/admissions/${id}/print`}>
              <Button variant="secondary">
                <FileText className="h-4 w-4" />
                View Print Format
              </Button>
            </Link>
          )}
          {canManage && admission.status === "STUDENT_SECTION_APPROVED" && (
            <Button variant="secondary" onClick={() => setModal("printed")}>
              Mark Printed
            </Button>
          )}
        </div>
      </Card>
      <DocumentVerification admission={admission} />
      <DetailedAdmissionView admission={admission} />
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
      <HistoryTimeline history={history} />
      <ActionModal
        modal={modal}
        onClose={() => setModal(null)}
        onReject={() => setModal("reject")}
        id={id}
        requestedCategory={admission.studentCategory}
        admission={admission}
        reload={load}
      />
    </div>
  );
}

function ActionModal({
  modal,
  onClose,
  onReject,
  id,
  requestedCategory,
  admission,
  reload,
}: {
  modal: "approve" | "reject" | "printed" | null;
  onClose: () => void;
  onReject: () => void;
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
      leavingCertificateVerified: false,
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
  const printedForm = useForm<z.infer<typeof markAdmissionPrintedSchema>>({
    resolver: zodResolver(markAdmissionPrintedSchema),
    defaultValues: { remarks: "" },
  });
  const approveDirectly = async (form: HTMLFormElement | null) => {
    if (!form) return;
    const data = new FormData(form);
    const checked = (name: string) => data.has(name);
    const values = {
      studentCategory: String(data.get("studentCategory") || requestedCategory),
      photoVerified: checked("photoVerified"),
      tenthMarksheetVerified: checked("tenthMarksheetVerified"),
      twelfthMarksheetVerified: checked("twelfthMarksheetVerified"),
      leavingCertificateVerified: checked("leavingCertificateVerified"),
      aadhaarCardVerified: checked("aadhaarCardVerified"),
      graduationPgCertificateVerified: checked("graduationPgCertificateVerified"),
      migrationCertificateVerified: checked("migrationCertificateVerified"),
      gapAffidavitVerified: checked("gapAffidavitVerified"),
      casteCertificateVerified: checked("casteCertificateVerified"),
      incomeProofVerified: checked("incomeProofVerified"),
      nameChangeCertificateVerified: checked("nameChangeCertificateVerified"),
      remarks: String(data.get("remarks") || ""),
    };
    if (!values.photoVerified || !values.tenthMarksheetVerified || !values.twelfthMarksheetVerified
        || !values.leavingCertificateVerified || !values.aadhaarCardVerified) {
      toast.error("Check and verify every required document before approval");
      return;
    }
    await submit(values);
  };
  const submit = async (values: Record<string, string | boolean>) => {
    try {
      if (modal === "approve")
        await api.approveAdmission(id, {
          studentCategory: values.studentCategory as StudentSectionAdmissionResponse["studentCategory"],
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
      if (modal === "printed") await api.markAdmissionPrinted(id, { remarks: String(values.remarks || "") });
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
      title={
        modal === "reject"
          ? "Reject admission"
          : modal === "printed"
            ? "Mark as printed"
            : "Approve admission"
      }
    >
      {modal === "approve" && (
        <form onSubmit={approveForm.handleSubmit(submit)} className="space-y-4">
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
            <p className="text-sm font-bold">Required document verification</p>
            <ChecklistItem label="Passport photo is clear and belongs to the student" error={approveForm.formState.errors.photoVerified?.message} {...approveForm.register("photoVerified")} />
            <ChecklistItem label="10th marksheet has been submitted and verified" error={approveForm.formState.errors.tenthMarksheetVerified?.message} {...approveForm.register("tenthMarksheetVerified")} />
            <ChecklistItem label="12th marksheet has been submitted and verified" error={approveForm.formState.errors.twelfthMarksheetVerified?.message} {...approveForm.register("twelfthMarksheetVerified")} />
            <ChecklistItem label="Leaving certificate has been submitted and verified" error={approveForm.formState.errors.leavingCertificateVerified?.message} {...approveForm.register("leavingCertificateVerified")} />
            <ChecklistItem label="Aadhaar card has been submitted and verified" error={approveForm.formState.errors.aadhaarCardVerified?.message} {...approveForm.register("aadhaarCardVerified")} />
          </div>
          <OptionalDocumentChecklist admission={admission} register={approveForm.register} />
          <Textarea
            label="Remarks"
            {...approveForm.register("remarks")}
            error={approveForm.formState.errors.remarks?.message}
          />
          <div className="flex flex-wrap gap-2">
            <Button type="button" onClick={(event) => void approveDirectly(event.currentTarget.form)} loading={approveForm.formState.isSubmitting}>
              Approve
            </Button>
            <Button type="button" variant="danger" onClick={onReject}>
              Reject / Request corrections
            </Button>
          </div>
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
      {modal === "printed" && (
        <form onSubmit={printedForm.handleSubmit(submit)} className="space-y-4">
          <Textarea
            label="Remarks"
            {...printedForm.register("remarks")}
            error={printedForm.formState.errors.remarks?.message}
          />
          <Button type="submit" loading={printedForm.formState.isSubmitting}>
            Mark Printed
          </Button>
        </form>
      )}
    </Modal>
  );
}

function OptionalDocumentChecklist({
  admission,
  register,
}: {
  admission: StudentSectionAdmissionResponse;
  register: ReturnType<typeof useForm<z.infer<typeof approveAdmissionSchema>>>["register"];
}) {
  const documents = [
    ["Graduation / PG certificate", "graduationPgCertificateAvailable", "graduationPgCertificateVerified"],
    ["Migration certificate", "migrationCertificateAvailable", "migrationCertificateVerified"],
    ["GAP affidavit", "gapAffidavitAvailable", "gapAffidavitVerified"],
    ["Caste certificate", "casteCertificateAvailable", "casteCertificateVerified"],
    ["Income proof", "incomeProofAvailable", "incomeProofVerified"],
    ["Marriage / name-change proof", "nameChangeCertificateAvailable", "nameChangeCertificateVerified"],
  ] as const;
  const submitted = documents.filter(([, available]) => admission[available]);
  if (submitted.length === 0) return null;
  return (
    <div className="space-y-3 rounded-xl border border-indigo-100 bg-indigo-50/50 p-4">
      <div>
        <p className="text-sm font-bold">Submitted optional document verification</p>
        <p className="mt-1 text-xs text-slate-500">Only optional documents uploaded by the student are shown.</p>
      </div>
      {submitted.map(([label, , field]) => (
        <ChecklistItem key={field} label={`${label} has been submitted and verified`} {...register(field)} />
      ))}
    </div>
  );
}

function DocumentVerification({ admission }: { admission: StudentSectionAdmissionResponse }) {
  const open = async (type: api.AdmissionDocumentType) => {
    try { const url = await api.getAdmissionDocument(admission.id, type); window.open(url, "_blank", "noopener,noreferrer"); setTimeout(() => URL.revokeObjectURL(url), 60000); }
    catch (error) { toast.error(handleApiError(error).message); }
  };
  const documents = [
    ["Passport photo", admission.photoAvailable, null],
    ["10th marksheet", admission.tenthMarksheetAvailable, "TENTH_MARKSHEET"],
    ["12th marksheet", admission.twelfthMarksheetAvailable, "TWELFTH_MARKSHEET"],
    ["Leaving certificate", admission.leavingCertificateAvailable, "LEAVING_CERTIFICATE"],
    ["Aadhaar card", admission.aadhaarCardAvailable, "AADHAAR_CARD"],
    ["Graduation / PG certificate (optional)", admission.graduationPgCertificateAvailable, "GRADUATION_PG_CERTIFICATE"],
    ["Migration certificate (optional)", admission.migrationCertificateAvailable, "MIGRATION_CERTIFICATE"],
    ["GAP affidavit (optional)", admission.gapAffidavitAvailable, "GAP_AFFIDAVIT"],
    ["Caste certificate (optional)", admission.casteCertificateAvailable, "CASTE_CERTIFICATE"],
    ["Income proof (optional)", admission.incomeProofAvailable, "INCOME_PROOF"],
    ["Marriage / name-change proof (optional)", admission.nameChangeCertificateAvailable, "NAME_CHANGE_CERTIFICATE"],
  ] as const;
  return <Card className="p-5"><h2 className="text-lg font-bold">Submitted document checklist</h2><p className="mt-1 text-sm text-slate-500">Open each submitted file, compare it with the academic details, then confirm required documents in the approval checklist.</p><div className="mt-4 grid gap-3 md:grid-cols-3">{documents.map(([label, available, type]) => { const optional = label.includes("(optional)"); return <div key={label} className={`rounded-xl border p-4 ${available ? "border-emerald-200 bg-emerald-50" : optional ? "border-slate-200 bg-slate-50" : "border-rose-200 bg-rose-50"}`}><p className="font-semibold">{label}</p><p className={`mt-1 text-xs ${available ? "text-emerald-700" : optional ? "text-slate-500" : "text-rose-700"}`}>{available ? "Submitted" : optional ? "Not submitted — optional" : "Not submitted — required"}</p>{available && type && <Button className="mt-3" variant="secondary" onClick={() => void open(type)}>View document</Button>}</div>;})}</div></Card>;
}

function ChecklistItem({ label, error, ...props }: { label: string; error?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return <label className="block"><span className="flex items-start gap-2 text-sm"><input type="checkbox" className="mt-1 h-4 w-4" {...props}/><span>{label}</span></span>{error && <span className="ml-6 text-xs text-rose-600">{error}</span>}</label>;
}
