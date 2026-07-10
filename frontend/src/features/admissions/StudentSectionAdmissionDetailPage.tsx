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
import { Textarea } from "@/components/common/Textarea";
import { handleApiError } from "@/lib/handleApiError";
import { approveAdmissionSchema, markAdmissionPrintedSchema, rejectAdmissionSchema } from "@/lib/validators";
import { formatDate } from "@/lib/utils";
import { AdmissionStatusBadge, DetailSection, HistoryTimeline } from "./components";
import * as api from "./api";
import type { AdmissionStatusHistoryResponse, StudentSectionAdmissionResponse } from "./types";

export function StudentSectionAdmissionDetailPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [history, setHistory] = useState<AdmissionStatusHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<"approve" | "reject" | "printed" | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline] = await Promise.all([api.getStudentSectionAdmission(id), api.getAdmissionHistory(id)]);
      setAdmission(detail); setHistory(timeline);
    } catch (err) { toast.error(handleApiError(err).message); }
    finally { setLoading(false); }
  }, [id]);
  useEffect(() => { load(); }, [load]);
  const quick = async () => {
    try { await api.startAdmissionReview(id); toast.success("Review started"); await load(); }
    catch (err) { toast.error(handleApiError(err).message); }
  };
  if (loading) return <Loader label="Loading admission detail..." />;
  if (!admission) return null;
  const canVerify = ["SUBMITTED", "STUDENT_SECTION_REVIEW_PENDING"].includes(admission.status);
  return (
    <div className="page-container space-y-5">
      <Card className="p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div><h1 className="page-title">{admission.fullName}</h1><p className="page-subtitle">{admission.admissionReferenceNumber} - {admission.admissionNumber}</p></div>
          <AdmissionStatusBadge status={admission.status} />
        </div>
        <div className="mt-5 flex flex-wrap gap-2">
          {admission.status === "SUBMITTED" && <Button variant="secondary" onClick={quick}>Start Review</Button>}
          {canVerify && <Button onClick={() => setModal("approve")}>Approve</Button>}
          {canVerify && <Button variant="danger" onClick={() => setModal("reject")}>Reject</Button>}
          {admission.status === "STUDENT_SECTION_APPROVED" && <Link to={`/student-section/admissions/${id}/print`}><Button variant="secondary"><FileText className="h-4 w-4" />View Print Format</Button></Link>}
          {admission.status === "STUDENT_SECTION_APPROVED" && <Button variant="secondary" onClick={() => setModal("printed")}>Mark Printed</Button>}
        </div>
      </Card>
      <DetailSection title="Admission" rows={[["College", admission.collegeName], ["Department", admission.departmentName], ["Academic Year", admission.academicYear], ["Submitted", formatDate(admission.submittedAt)]]} />
      <DetailSection title="Student" rows={[["Email", admission.email], ["Phone", admission.phone], ["Date of Birth", formatDate(admission.dateOfBirth)], ["Gender", admission.gender], ["Address", [admission.addressLine1, admission.city, admission.state, admission.pincode].filter(Boolean).join(", ")]]} />
      <DetailSection title="Parent and Previous Academic" rows={[["Parent", admission.parentName], ["Parent Phone", admission.parentPhone], ["Parent Email", admission.parentEmail], ["Previous School", admission.previousSchoolName], ["Previous Class", admission.previousClassName], ["Previous Percentage", admission.previousPercentage]]} />
      <DetailSection title="Verification and Print" rows={[["Verified At", formatDate(admission.studentSectionVerifiedAt)], ["Verified By", admission.studentSectionVerifiedByName], ["Remarks", admission.studentSectionRemarks], ["Rejected At", formatDate(admission.studentSectionRejectedAt)], ["Rejection Reason", admission.rejectionReason], ["Print Count", admission.printCount], ["Last Printed", formatDate(admission.lastPrintedAt)], ["Last Printed By", admission.lastPrintedByName]]} />
      <HistoryTimeline history={history} />
      <ActionModal modal={modal} onClose={() => setModal(null)} id={id} reload={load} />
    </div>
  );
}

function ActionModal({ modal, onClose, id, reload }: { modal: "approve" | "reject" | "printed" | null; onClose: () => void; id: number; reload: () => Promise<void> }) {
  const approveForm = useForm<z.infer<typeof approveAdmissionSchema>>({ resolver: zodResolver(approveAdmissionSchema), defaultValues: { remarks: "" } });
  const rejectForm = useForm<z.infer<typeof rejectAdmissionSchema>>({ resolver: zodResolver(rejectAdmissionSchema), defaultValues: { rejectionReason: "" } });
  const printedForm = useForm<z.infer<typeof markAdmissionPrintedSchema>>({ resolver: zodResolver(markAdmissionPrintedSchema), defaultValues: { remarks: "" } });
  const submit = async (values: Record<string, string>) => {
    try {
      if (modal === "approve") await api.approveAdmission(id, values);
      if (modal === "reject") await api.rejectAdmission(id, { rejectionReason: values.rejectionReason });
      if (modal === "printed") await api.markAdmissionPrinted(id, values);
      toast.success("Admission updated");
      onClose();
      await reload();
    } catch (err) { toast.error(handleApiError(err).message); }
  };
  return (
    <Modal open={Boolean(modal)} onClose={onClose} title={modal === "reject" ? "Reject admission" : modal === "printed" ? "Mark as printed" : "Approve admission"}>
      {modal === "approve" && <form onSubmit={approveForm.handleSubmit(submit)} className="space-y-4"><Textarea label="Remarks" {...approveForm.register("remarks")} error={approveForm.formState.errors.remarks?.message} /><Button type="submit" loading={approveForm.formState.isSubmitting}>Approve</Button></form>}
      {modal === "reject" && <form onSubmit={rejectForm.handleSubmit(submit)} className="space-y-4"><Textarea label="Rejection reason" {...rejectForm.register("rejectionReason")} error={rejectForm.formState.errors.rejectionReason?.message} /><Button type="submit" variant="danger" loading={rejectForm.formState.isSubmitting}>Reject</Button></form>}
      {modal === "printed" && <form onSubmit={printedForm.handleSubmit(submit)} className="space-y-4"><Textarea label="Remarks" {...printedForm.register("remarks")} error={printedForm.formState.errors.remarks?.message} /><Button type="submit" loading={printedForm.formState.isSubmitting}>Mark Printed</Button></form>}
    </Modal>
  );
}
