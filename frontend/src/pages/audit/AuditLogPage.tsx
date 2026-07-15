import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { formatDate } from "@/lib/utils";
import { handleApiError } from "@/lib/handleApiError";
import { searchAuditLogs, type AuditLog } from "@/features/audit/api";
export function AuditLogPage() {
  const [rows, setRows] = useState<AuditLog[]>([]),
    [q, setQ] = useState(""),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => {
      setLoading(true);
      searchAuditLogs({ keyword: q || undefined, size: 50 })
        .then((r) => setRows(r.content))
        .catch((e) => toast.error(handleApiError(e).message))
        .finally(() => setLoading(false));
    }, 250);
    return () => clearTimeout(t);
  }, [q]);
  return (
    <div className="page-container">
      <h1 className="page-title">Audit Logs</h1>
      <p className="page-subtitle">Security and business activity history.</p>
      <Card className="mt-6">
        <div className="border-b p-4">
          <Input
            placeholder="Search actor or description…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        {loading ? (
          <Loader />
        ) : rows.length ? (
          <div className="w-full overflow-hidden">
            <table className="w-full table-fixed text-xs sm:text-sm">
              <thead>
                <tr>
                  <th className="p-3 text-left">Date</th>
                  <th>Actor</th>
                  <th>Module</th>
                  <th>Action</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr className="border-t" key={r.id}>
                    <td className="p-3">{formatDate(r.createdAt)}</td>
                    <td>
                      {r.actorName}
                      <div className="text-xs text-slate-400">{r.actorEmail}</div>
                    </td>
                    <td>{r.module}</td>
                    <td>{r.action}</td>
                    <td>{r.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No audit activity" description="Important actions will appear here." />
        )}
      </Card>
    </div>
  );
}
