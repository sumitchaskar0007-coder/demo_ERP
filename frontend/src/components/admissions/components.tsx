import { Badge } from "@/components/common/Badge";
import { Card } from "@/components/common/Card";
import { formatDate } from "@/lib/utils";
import type { AdmissionStatus, AdmissionStatusHistoryResponse } from "@/features/admissions/types";
import type { PaymentResponse } from "@/features/fees/types";

export function AdmissionStatusBadge({ status }: { status: AdmissionStatus }) {
  const tone = status.includes("REJECTED")
    ? "danger"
    : status.includes("APPROVED")
      ? "success"
      : status.includes("PENDING")
        ? "warning"
        : status === "CANCELLED"
          ? "neutral"
          : "info";
  return <Badge tone={tone}>{status.replaceAll("_", " ")}</Badge>;
}

export function DetailSection({ title, rows }: { title: string; rows: Array<[string, unknown]> }) {
  return (
    <Card className="p-5">
      <h2 className="text-base font-bold text-slate-900">{title}</h2>
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        {rows.map(([label, value]) => (
          <div key={label}>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</p>
            <p className="mt-1 text-sm font-medium text-slate-800">{value ? String(value) : "-"}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}

type JourneyEvent = {
  id: string;
  label: string;
  detail: string;
  actor?: string | null;
  at: string;
  tone: "success" | "danger" | "warning" | "info" | "neutral";
};

export function HistoryTimeline({
  history,
  payments = [],
}: {
  history: AdmissionStatusHistoryResponse[];
  payments?: PaymentResponse[];
}) {
  const admissionEvents = history.flatMap<JourneyEvent>((item) => {
    const resubmitted =
      item.action === "SUBMITTED" && item.oldStatus === "STUDENT_SECTION_REJECTED";
    const config: Partial<Record<typeof item.action, Pick<JourneyEvent, "label" | "tone">>> = {
      SUBMITTED: {
        label: resubmitted ? "Application Resubmitted" : "Application Submitted",
        tone: resubmitted ? "info" : "warning",
      },
      STUDENT_SECTION_REJECTED: { label: "Student Section Rejected", tone: "danger" },
      STUDENT_SECTION_APPROVED: { label: "Student Section Approved", tone: "success" },
      PAYMENT_SUBMITTED: { label: "Fee Proof Submitted", tone: "warning" },
      PAYMENT_REJECTED: { label: "Fee Payment Rejected", tone: "danger" },
      PAYMENT_VERIFIED: { label: "Fee Payment Accepted", tone: "success" },
      PRINCIPAL_REVIEW_PENDING: { label: "Ready for Principal Review", tone: "info" },
      PRINCIPAL_APPROVED: { label: "Admission Approved", tone: "success" },
      PRINCIPAL_REJECTED: { label: "Admission Rejected by Principal", tone: "danger" },
    };
    const visible = config[item.action];
    if (!visible) return [];
    if (payments.length && item.action.startsWith("PAYMENT_")) return [];
    return [
      {
        id: `admission-${item.id}`,
        label: visible.label,
        detail: item.remarks || "Status updated",
        actor: item.changedByName,
        at: item.createdAt,
        tone: visible.tone,
      },
    ];
  });
  const paymentEvents = payments.flatMap<JourneyEvent>((payment) => {
    const amount = `₹${Number(payment.amount).toLocaleString("en-IN")}`;
    const detail = `${amount} via ${payment.paymentMode.replaceAll("_", " ")} · UTR ${payment.transactionReference}`;
    const events: JourneyEvent[] = [
      {
        id: `payment-${payment.id}-submitted`,
        label: "Fee Proof Submitted",
        detail,
        at: payment.submittedAt,
        tone: "warning",
      },
    ];
    if (payment.status === "REJECTED") {
      events.push({
        id: `payment-${payment.id}-rejected`,
        label: "Fee Payment Rejected",
        detail: payment.rejectionReason || detail,
        actor: payment.rejectedByName,
        at: payment.rejectedAt || payment.updatedAt,
        tone: "danger",
      });
    }
    if (payment.status === "VERIFIED") {
      events.push({
        id: `payment-${payment.id}-verified`,
        label: "Fee Payment Accepted",
        detail,
        actor: payment.verifiedByName,
        at: payment.verifiedAt || payment.updatedAt,
        tone: "success",
      });
    }
    return events;
  });
  const events = [...admissionEvents, ...paymentEvents].sort(
    (a, b) => new Date(a.at).getTime() - new Date(b.at).getTime(),
  );
  if (!events.length)
    return <Card className="p-5 text-sm text-slate-500">No status history yet.</Card>;
  const colors = {
    success: "border-emerald-200 bg-emerald-500",
    danger: "border-rose-200 bg-rose-500",
    warning: "border-amber-200 bg-amber-500",
    info: "border-blue-200 bg-blue-600",
    neutral: "border-slate-200 bg-slate-400",
  };
  return (
    <Card className="overflow-hidden">
      <div className="border-b bg-slate-50/70 px-6 py-5">
        <h2 className="text-lg font-bold">Student Admission Journey</h2>
        <p className="mt-1 text-sm text-slate-500">
          Complete admission and fee-verification progress.
        </p>
      </div>
      <div className="p-6">
        {events.map((event, index) => (
          <div key={event.id} className="relative flex gap-4 pb-7 last:pb-0">
            {index < events.length - 1 && (
              <span className="absolute left-[15px] top-8 h-[calc(100%-1rem)] w-px bg-slate-200" />
            )}
            <span
              className={`relative z-10 mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-full border-4 border-white shadow-sm ${colors[event.tone]}`}
            >
              <span className="h-2 w-2 rounded-full bg-white" />
            </span>
            <div className="min-w-0 flex-1 rounded-xl border border-slate-100 bg-white px-4 py-3 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-bold text-slate-900">{event.label}</p>
                <span className="text-xs font-medium text-slate-400">{formatDate(event.at)}</span>
              </div>
              <p className="mt-1 text-sm text-slate-600">{event.detail}</p>
              {event.actor && (
                <p className="mt-1 text-xs text-slate-400">Updated by {event.actor}</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}

// This pure formatter is intentionally colocated with the admission status components.
// eslint-disable-next-line react-refresh/only-export-components
export function statusExplanation(status?: AdmissionStatus) {
  const copy: Record<AdmissionStatus, string> = {
    STUDENT_DETAILS_PENDING: "Waiting for the student to complete the detailed form",
    SUBMITTED: "Form submitted",
    STUDENT_SECTION_REVIEW_PENDING: "Under student section review",
    STUDENT_SECTION_APPROVED: "Data verified by student section",
    STUDENT_SECTION_REJECTED: "Rejected by student section",
    PRINCIPAL_REVIEW_PENDING: "Waiting for principal review",
    PRINCIPAL_APPROVED: "Admission approved",
    PRINCIPAL_REJECTED: "Rejected by principal",
    CANCELLED: "Cancelled",
  };
  return status ? copy[status] : "-";
}
