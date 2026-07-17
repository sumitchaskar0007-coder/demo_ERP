import { useEffect, useState } from "react";
import { BarChart3, Download, Printer } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { attendanceApi, type AttendanceReport } from "./api";
import { exportAttendanceCsv, exportAttendanceExcel, exportAttendancePdf } from "./export";

const iso = (date: Date) => date.toISOString().slice(0, 10);
export function AttendanceReportPage() {
  const { user } = useAuth();
  const now = new Date();
  const before = new Date();
  before.setDate(now.getDate() - 29);
  const [from, setFrom] = useState(iso(before));
  const [to, setTo] = useState(iso(now));
  const [data, setData] = useState<AttendanceReport>();
  const [loading, setLoading] = useState(true);
  const path =
    user?.roles.includes(ROLES.PRINCIPAL) || user?.roles.includes(ROLES.SUPER_ADMIN)
      ? "/api/principal/attendance/report"
      : user?.roles.includes(ROLES.HOD)
        ? "/api/hod/attendance/report"
        : "/api/class-teacher/attendance/division";
  const load = () => {
    setLoading(true);
    attendanceApi
      .report(path, { from, to })
      .then(setData)
      .catch((e) => toast.error(handleApiError(e).message))
      .finally(() => setLoading(false));
  };
  useEffect(load, [path]);
  if (loading && !data) return <Loader label="Preparing attendance report..." />;
  return (
    <div className="page-container space-y-6 pb-12">
      <header className="page-header sm:items-end">
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Analytics</p>
          <h1 className="page-title mt-1">Attendance Reports</h1>
          <p className="mt-1 text-sm text-slate-500">
            Secure role-scoped lecture attendance analytics.
          </p>
        </div>
        <div className="grid w-full grid-cols-2 gap-2 sm:flex sm:w-auto sm:flex-wrap sm:justify-end">
          <Button
            variant="secondary"
            disabled={!data}
            onClick={() => data && void exportAttendancePdf(data)}
          >
            <Printer className="mr-2 h-4 w-4" />
            PDF
          </Button>
          <Button
            variant="secondary"
            disabled={!data}
            onClick={() => data && void exportAttendanceExcel(data)}
          >
            <Download className="mr-2 h-4 w-4" />
            Excel
          </Button>
          <Button
            variant="secondary"
            disabled={!data}
            onClick={() => data && exportAttendanceCsv(data)}
          >
            <Download className="mr-2 h-4 w-4" />
            CSV
          </Button>
          <Button variant="secondary" onClick={() => window.print()}>
            <Printer className="mr-2 h-4 w-4" />
            Print
          </Button>
        </div>
      </header>
      <Card className="p-4 sm:p-5">
        <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <Input label="From" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          <Input label="To" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          <Button className="w-full sm:w-auto" loading={loading} onClick={load}>Apply filters</Button>
        </div>
      </Card>
      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-5">
            <Metric label="Overall" value={`${data.percentage}%`} />
            <Metric label="Sessions" value={data.sessions} />
            <Metric label="Present" value={data.present} />
            <Metric label="Absent" value={data.absent} />
            <Metric label="Late / Leave" value={`${data.late} / ${data.leave}`} />
          </div>
          <Card className="overflow-hidden">
            <div className="border-b p-5">
              <h2 className="flex items-center gap-2 font-bold">
                <BarChart3 className="h-5 w-5 text-brand-600" />
                Lecture register
              </h2>
            </div>
            {data.rows.length === 0 ? (
              <div className="p-12 text-center text-sm text-slate-500">
                No submitted attendance sessions match this range.
              </div>
            ) : (
              <>
                <div className="grid gap-3 bg-slate-50 p-3 lg:hidden">
                  {data.rows.map((row) => <AttendanceMobileCard key={row.id} row={row} />)}
                </div>
                <div className="erp-table-scroll hidden lg:block">
                <table className="erp-table min-w-[980px]">
                  <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                    <tr>
                      {[
                        "Date & time",
                        "Subject",
                        "Department",
                        "Class",
                        "Teacher",
                        "Present",
                        "Absent",
                        "Late",
                        "Leave",
                        "%",
                        "Status",
                      ].map((h) => (
                        <th key={h} className="px-4 py-3">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {data.rows.map((r) => (
                      <tr key={r.id} className="border-t">
                        <td className="px-4 py-3">
                          {r.date}
                          <br />
                          <span className="text-xs text-slate-400">{r.time}</span>
                        </td>
                        <td className="px-4 py-3 font-semibold">{r.subject}</td>
                        <td className="px-4 py-3">{r.department}</td>
                        <td className="px-4 py-3">
                          {r.year} - {r.division}
                        </td>
                        <td className="px-4 py-3">{r.teacher}</td>
                        <td className="px-4 py-3 text-emerald-600">{r.present}</td>
                        <td className="px-4 py-3 text-rose-600">{r.absent}</td>
                        <td className="px-4 py-3 text-amber-600">{r.late}</td>
                        <td className="px-4 py-3 text-blue-600">{r.leave}</td>
                        <td className="px-4 py-3 font-bold">{r.percentage}</td>
                        <td className="px-4 py-3 text-xs font-bold">{r.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                </div>
              </>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <Card className="p-4 sm:p-5">
      <p className="text-xs font-semibold uppercase text-slate-400">{label}</p>
      <p className="mt-2 text-xl font-black sm:text-2xl">{value}</p>
    </Card>
  );
}

function AttendanceMobileCard({
  row,
}: {
  row: AttendanceReport["rows"][number];
}) {
  return (
    <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-start justify-between gap-3 border-b bg-slate-50/80 p-4">
        <div className="min-w-0">
          <p className="break-words font-semibold text-slate-900">{row.subject}</p>
          <p className="mt-1 text-xs text-slate-500">{row.date} · {row.time}</p>
        </div>
        <span className="shrink-0 rounded-full bg-brand-50 px-2.5 py-1 text-[10px] font-bold text-brand-700">
          {row.status}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-x-4 gap-y-3 p-4 text-xs">
        <MobileFact label="Department" value={row.department} />
        <MobileFact label="Class" value={`${row.year} - ${row.division}`} />
        <MobileFact label="Teacher" value={row.teacher} className="col-span-2" />
      </div>
      <div className="grid grid-cols-5 border-t bg-slate-50/70 text-center">
        {[
          ["Present", row.present, "text-emerald-600"],
          ["Absent", row.absent, "text-rose-600"],
          ["Late", row.late, "text-amber-600"],
          ["Leave", row.leave, "text-blue-600"],
          ["Rate", `${row.percentage}%`, "text-slate-900"],
        ].map(([label, value, tone]) => (
          <div className="border-r px-1 py-3 last:border-r-0" key={String(label)}>
            <p className={`font-bold ${tone}`}>{value}</p>
            <p className="mt-0.5 text-[9px] uppercase tracking-wide text-slate-400">{label}</p>
          </div>
        ))}
      </div>
    </article>
  );
}

function MobileFact({ label, value, className = "" }: { label: string; value: string; className?: string }) {
  return (
    <div className={className}>
      <p className="font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 break-words font-medium text-slate-700">{value || "—"}</p>
    </div>
  );
}
