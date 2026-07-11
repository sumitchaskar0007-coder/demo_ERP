import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { StatusBadge } from "@/components/common/Badge";
import { DataTable, type Column } from "@/components/table/DataTable";
import { handleApiError } from "@/lib/handleApiError";
import {
  cancelEmailNotification,
  retryEmailNotification,
  searchEmailNotifications,
} from "@/features/email/api";
import type { EmailNotification, EmailStatus } from "@/features/email/types";
export function EmailNotificationsPage() {
  const [data, setData] = useState<EmailNotification[]>([]);
  const [status, setStatus] = useState<EmailStatus | "">("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const load = useCallback(() => {
    setLoading(true);
    searchEmailNotifications({ status, recipientEmail: email || undefined, size: 50 })
      .then((x) => setData(x.content))
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  }, [status, email]);
  useEffect(() => {
    load();
  }, [load]);
  const action = async (id: number, type: "retry" | "cancel") => {
    if (!window.confirm(`${type === "retry" ? "Retry" : "Cancel"} this notification?`)) return;
    try {
      if (type === "retry") await retryEmailNotification(id);
      else await cancelEmailNotification(id);
      toast.success("Notification updated");
      load();
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  const cols: Column<EmailNotification>[] = [
    { key: "type", header: "Type", render: (r) => r.emailType.replaceAll("_", " ") },
    { key: "recipient", header: "Recipient", render: (r) => r.recipientEmail },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    { key: "retries", header: "Retries", render: (r) => `${r.retryCount}/${r.maxRetries}` },
    { key: "created", header: "Created", render: (r) => new Date(r.createdAt).toLocaleString() },
    {
      key: "sent",
      header: "Sent",
      render: (r) => (r.sentAt ? new Date(r.sentAt).toLocaleString() : "—"),
    },
    {
      key: "actions",
      header: "Actions",
      render: (r) => (
        <div className="flex gap-2">
          {["FAILED", "RETRY_PENDING"].includes(r.status) && (
            <Button variant="secondary" onClick={() => action(r.id, "retry")}>
              Retry
            </Button>
          )}
          {["QUEUED", "RETRY_PENDING"].includes(r.status) && (
            <Button variant="danger" onClick={() => action(r.id, "cancel")}>
              Cancel
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <div className="page-container space-y-5">
      <div className="flex justify-between">
        <div>
          <h1 className="page-title">Email Notifications</h1>
          <p className="page-subtitle">
            Monitor the transactional email outbox. Delivery is confirmed only when status is SENT.
          </p>
        </div>
        <Button variant="secondary" onClick={load}>
          Refresh
        </Button>
      </div>
      <Card className="grid gap-3 p-4 md:grid-cols-2">
        <Input
          placeholder="Search masked recipient"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <select
          className="h-10 rounded-xl border px-3"
          value={status}
          onChange={(e) => setStatus(e.target.value as EmailStatus | "")}
        >
          <option value="">All statuses</option>
          {["QUEUED", "PROCESSING", "SENT", "RETRY_PENDING", "FAILED", "CANCELLED"].map((x) => (
            <option key={x}>{x}</option>
          ))}
        </select>
      </Card>
      {loading ? (
        <Card className="p-8 text-center">Loading email queue…</Card>
      ) : (
        <DataTable columns={cols} data={data} rowKey={(r) => r.id} />
      )}
    </div>
  );
}
