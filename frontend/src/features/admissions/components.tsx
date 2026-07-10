import { Badge } from "@/components/common/Badge";
import { Card } from "@/components/common/Card";
import { formatDate } from "@/lib/utils";
import type { AdmissionStatus, AdmissionStatusHistoryResponse } from "./types";

export function AdmissionStatusBadge({ status }: { status: AdmissionStatus }) {
  const tone =
    status.includes("REJECTED") ? "danger" :
    status.includes("APPROVED") ? "success" :
    status.includes("PENDING") ? "warning" :
    status === "CANCELLED" ? "neutral" : "info";
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

export function HistoryTimeline({ history }: { history: AdmissionStatusHistoryResponse[] }) {
  if (!history.length) return <Card className="p-5 text-sm text-slate-500">No status history yet.</Card>;
  return (
    <Card className="p-5">
      <h2 className="text-base font-bold">Status history</h2>
      <div className="mt-5 space-y-4">
        {history.map((item) => (
          <div key={item.id} className="border-l-2 border-brand-200 pl-4">
            <div className="flex flex-wrap items-center gap-2">
              <AdmissionStatusBadge status={item.newStatus} />
              <span className="text-xs text-slate-400">{formatDate(item.createdAt)}</span>
            </div>
            <p className="mt-1 text-sm font-semibold text-slate-800">{item.action.replaceAll("_", " ")}</p>
            <p className="text-sm text-slate-500">{item.remarks || "No remarks"} {item.changedByName ? `- ${item.changedByName}` : ""}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}

export function statusExplanation(status?: AdmissionStatus) {
  const copy: Record<AdmissionStatus, string> = {
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
