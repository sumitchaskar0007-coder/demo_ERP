import { useEffect, useState } from "react";
import { Download } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { exportReport, getReport, type ReportRow } from "@/features/reports/api";
export function ReportPage({ type }: { type: "admissions" | "fees" | "attendance" | "students" }) {
  const [searchParams] = useSearchParams();
  const status = searchParams.get("status") || undefined;
  const [rows, setRows] = useState<ReportRow[]>([]),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    setLoading(true);
    getReport(type, status ? { status } : undefined)
      .then(setRows)
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  }, [status, type]);
  const exp = async () => {
    try {
      await exportReport(type, status ? { status } : undefined);
      toast.success("CSV exported");
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  const keys = rows[0] ? Object.keys(rows[0]) : [];
  return (
    <div className="page-container">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">{status ? status.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : type[0].toUpperCase() + type.slice(1)} {status ? "Admissions" : "Report"}</h1>
          <p className="page-subtitle">College-scoped operational data{status ? " filtered by application status" : ""}.</p>
        </div>
        <Button onClick={exp}>
          <Download className="h-4 w-4" />
          Export CSV
        </Button>
      </div>
      <Card className="mt-6">
        {loading ? (
          <Loader />
        ) : rows.length ? (
          <div className="responsive-table">
            <table>
              <thead>
                <tr>
                  {keys.map((k) => (
                    <th className="p-2 text-left text-slate-500 sm:p-3" key={k}>
                      {k.replace(/([A-Z])/g, " $1")}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr className="border-t" key={i}>
                    {keys.map((k) => (
                      <td className="p-2 sm:p-3" key={k}>
                        {String(r[k] ?? "")}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No report records"
            description="No data matches your current role scope."
          />
        )}
      </Card>
    </div>
  );
}
