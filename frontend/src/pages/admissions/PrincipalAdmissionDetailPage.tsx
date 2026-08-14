import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Textarea } from "@/components/common/Textarea";
import { Loader } from "@/components/common/Loader";
import { StatusBadge } from "@/components/common/Badge";
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
  AdmissionDocumentRequirement,
  AdmissionStatusHistoryResponse,
  StudentSectionAdmissionResponse,
} from "@/features/admissions/types";
import type { AdmissionFeeSummaryResponse } from "@/features/fees/types";

export function PrincipalAdmissionDetailPage() {
  const { admissionId = "" } = useParams();
  const id = Number(admissionId);
  const [admission, setAdmission] = useState<StudentSectionAdmissionResponse | null>(null);
  const [history, setHistory] = useState<AdmissionStatusHistoryResponse[]>([]);
  const [fees, setFees] = useState<AdmissionFeeSummaryResponse | null>(null);
  const [requirements, setRequirements] = useState<AdmissionDocumentRequirement[]>([]);
  const [loading, setLoading] = useState(true);
  const [decision, setDecision] = useState<"approve" | "reject" | null>(null);
  const [remarks, setRemarks] = useState("");
  const [saving, setSaving] = useState(false);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [detail, timeline, feeInformation, configuredDocuments] = await Promise.all([
        api.getPrincipalAdmission(id),
        api.getPrincipalAdmissionHistory(id),
        api.getPrincipalAdmissionFees(id),
        api.getAdmissionDocumentRequirements(id),
      ]);
      setAdmission(detail);
      setHistory(timeline);
      setFees(feeInformation);
      setRequirements(configuredDocuments);
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
      <FeeInformationCard fees={fees} />
      <DetailedAdmissionView
        admission={admission}
        documentRequirements={requirements}
        principal
      />
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
      <HistoryTimeline history={history} payments={fees?.payments} />
    </div>
  );
}

function FeeInformationCard({ fees }: { fees: AdmissionFeeSummaryResponse | null }) {
  const account = fees?.account;
  if (!account) {
    return (
      <Card className="overflow-hidden">
        <div className="border-b bg-slate-50 px-6 py-5">
          <h2 className="text-lg font-bold">Student Fee Information</h2>
        </div>
        <p className="m-6 rounded-xl bg-amber-50 p-4 text-sm text-amber-800">
          A fee account has not been created for this admission yet.
        </p>
      </Card>
    );
  }
  const payableFee = Math.max(0, account.totalFee - account.scholarshipAmount);
  const percentage = payableFee
    ? Math.min(100, Math.round((account.paidAmount / payableFee) * 100))
    : 0;
  const money = (amount: number) => `₹${Number(amount).toLocaleString("en-IN")}`;
  return (
    <Card className="overflow-hidden">
      <div className="bg-gradient-to-r from-blue-700 to-indigo-700 px-6 py-6 text-white">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[.18em] text-blue-200">
              Fee Account
            </p>
            <h2 className="mt-1 text-xl font-bold">Student Fee Information</h2>
            <p className="mt-1 text-sm text-blue-100">
              {account.academicYear} · {account.customCategoryName || account.studentCategory}
            </p>
          </div>
          <StatusBadge status={account.status} />
        </div>
        <div className="mt-6 grid gap-5 md:grid-cols-[1fr_auto] md:items-end">
          <div>
            <div className="flex items-end justify-between gap-4">
              <div>
                <p className="text-sm text-blue-100">Remaining fee</p>
                <p className="mt-1 text-3xl font-black">{money(account.remainingAmount)}</p>
              </div>
              <p className="text-sm font-bold">{percentage}% paid</p>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/20">
              <div
                className="h-full rounded-full bg-emerald-300"
                style={{ width: `${percentage}%` }}
              />
            </div>
          </div>
          <div className="rounded-xl bg-white/10 px-5 py-3 text-right backdrop-blur">
            <p className="text-xs text-blue-100">Total fee</p>
            <p className="text-xl font-black">{money(account.totalFee)}</p>
          </div>
        </div>
      </div>
      <div className="grid gap-3 border-b bg-slate-50/70 p-5 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ["Paid Fee", account.paidAmount],
          ["Minimum Required", account.minimumAmountForAdmission],
          ["Discount", account.discountAmount],
          ["Credit / Refund Due", account.creditAmount],
        ].map(([label, amount]) => (
          <div key={String(label)} className="rounded-xl border bg-white px-4 py-3">
            <p className="text-xs font-bold uppercase text-slate-400">{label}</p>
            <p className="mt-1 text-lg font-black text-slate-900">{money(Number(amount))}</p>
          </div>
        ))}
      </div>
      {account.scholarshipRemoved && (
        <div className="border-b border-amber-200 bg-amber-50 px-6 py-4 text-sm text-amber-900">
          <p className="font-bold">Scholarship removed by Principal</p>
          {account.scholarshipRemovalReason && (
            <p className="mt-1">Reason: {account.scholarshipRemovalReason}</p>
          )}
        </div>
      )}
      <div className="p-6">
        <div className="flex items-center justify-between gap-3">
          <h3 className="font-bold">Payment Submissions</h3>
          <span className="text-xs font-semibold text-slate-400">
            {fees?.payments.length || 0} records
          </span>
        </div>
        {fees?.payments.length ? (
          <div className="mt-4 grid gap-3 lg:grid-cols-2">
            {fees.payments.map((payment) => (
              <div key={payment.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xl font-black">{money(payment.amount)}</p>
                    <p className="mt-1 text-xs text-slate-500">{formatDate(payment.paymentDate)}</p>
                  </div>
                  <StatusBadge status={payment.status} />
                </div>
                <div className="mt-4 grid gap-2 border-t pt-3 text-sm sm:grid-cols-2">
                  <div>
                    <span className="text-slate-400">UTR</span>
                    <p className="font-semibold">{payment.transactionReference}</p>
                  </div>
                  <div>
                    <span className="text-slate-400">Mode</span>
                    <p className="font-semibold">{payment.paymentMode.replaceAll("_", " ")}</p>
                  </div>
                </div>
                {payment.rejectionReason && (
                  <p className="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
                    {payment.rejectionReason}
                  </p>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="mt-4 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
            No payment proof has been submitted yet.
          </p>
        )}
      </div>
    </Card>
  );
}
