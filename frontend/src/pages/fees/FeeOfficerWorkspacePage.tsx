import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  BadgeIndianRupee,
  CheckCircle2,
  CircleAlert,
  Download,
  Eye,
  Mail,
  Maximize2,
  RefreshCw,
  Search,
  ShieldCheck,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import { DocumentViewer } from "@/components/common/DocumentViewer";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/feeOfficer/api";
import { downloadFeeReceipt } from "@/features/fees/feeReceiptPdf";
const input =
  "h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100";
const primary =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-emerald-700 hover:shadow-md focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-emerald-100 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:translate-y-0";
const secondary =
  "inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-40";
const money = (v: number) => `₹${Number(v || 0).toLocaleString("en-IN")}`;
const feeStructureLabel = (value: string) => value.replace(/\s+fee$/i, "").trim();
export function FeeOfficerWorkspacePage() {
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") || "dashboard";
  const [data, setData] = useState<api.Workspace | null>(null),
    [loading, setLoading] = useState(true),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [mode, setMode] = useState(""),
    [departmentId, setDepartmentId] = useState(""),
    [academicYear, setAcademicYear] = useState(""),
    [courseYearId, setCourseYearId] = useState(""),
    [page, setPage] = useState(0),
    [payment, setPayment] = useState<api.Payment | null>(null),
    [accountId, setAccountId] = useState<number | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setData(
        await api.workspace({
          search: search || undefined,
          departmentId: departmentId || undefined,
          academicYear: academicYear || undefined,
          courseYearId: courseYearId || undefined,
          accountStatus: tab === "accounts" ? status || undefined : undefined,
          paymentStatus: !["accounts", "dashboard", "dues", "reports"].includes(tab)
            ? status || undefined
            : undefined,
          paymentMode: mode || undefined,
          page,
          size: 50,
        }),
      );
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, [search, status, mode, departmentId, academicYear, courseYearId, page, tab]);
  useEffect(() => {
    const t = setTimeout(() => void load(), search ? 250 : 0);
    return () => clearTimeout(t);
  }, [load, search]);
  useEffect(() => {
    setStatus("");
    setMode("");
    setDepartmentId("");
    setAcademicYear("");
    setCourseYearId("");
    setPage(0);
  }, [tab]);
  if (loading && !data) return <Skeleton />;
  if (!data)
    return (
      <Empty
        title="Fee workspace unavailable"
        text="Refresh the page or contact your administrator."
      />
    );
  return (
    <div className="mx-auto max-w-[1550px] space-y-6 px-3 py-4 pb-10 sm:px-6 sm:py-5 lg:px-0 lg:py-0">
      <header className="overflow-hidden rounded-3xl bg-gradient-to-r from-blue-700 via-blue-600 to-indigo-600 p-7 text-white shadow-xl shadow-blue-200/60">
        <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold">
              Fee Section Officer
            </span>
            <h1 className="mt-4 text-3xl font-bold">Fee Collection & Verification</h1>
            <p className="mt-2 text-sm text-blue-100">
              Accounts, student payments, dues, receipts and collection analytics
            </p>
          </div>
          <button
            className="rounded-xl border border-white/30 bg-white px-4 py-2 text-sm font-semibold text-blue-700 shadow-sm transition hover:bg-blue-50"
            onClick={() => window.location.reload()}
          >
            <RefreshCw className="mr-2 inline h-4 w-4" />
            Refresh
          </button>
        </div>
      </header>
      {tab === "dashboard" && <Dashboard data={data} go={(x) => setParams({ tab: x })} />}{" "}
      {tab !== "dashboard" && tab !== "dues" && tab !== "reports" && (
        <Filters
          data={data}
          search={search}
          setSearch={setSearch}
          status={status}
          setStatus={setStatus}
          mode={mode}
          setMode={setMode}
          departmentId={departmentId}
          setDepartmentId={setDepartmentId}
          academicYear={academicYear}
          setAcademicYear={setAcademicYear}
          courseYearId={courseYearId}
          setCourseYearId={setCourseYearId}
          type={tab}
        />
      )}{" "}
      {tab === "accounts" && (
        <Accounts data={data} page={page} setPage={setPage} open={setAccountId} />
      )}{" "}
      {tab === "pending" && (
        <Payments title="Pending payment verification" rows={data.pending} open={setPayment} />
      )}{" "}
      {tab === "verified" && (
        <Payments title="Verified payments" rows={data.verified} open={setPayment} receipt />
      )}{" "}
      {tab === "rejected" && <Rejected rows={data.rejected} open={setPayment} />}{" "}
      {tab === "history" && <HistoryView rows={data.history} open={setPayment} />}{" "}
      {tab === "dues" && <Dues rows={data.dues} open={setAccountId} />}{" "}
      {tab === "reports" && <Reports data={data} />}{" "}
      {payment && <PaymentReview payment={payment} close={() => setPayment(null)} reload={load} />}{" "}
      {accountId && (
        <AccountModal
          id={accountId}
          close={() => setAccountId(null)}
          payment={(selectedPayment) => {
            setAccountId(null);
            setPayment(selectedPayment);
          }}
        />
      )}
    </div>
  );
}
function Dashboard({ data, go }: { data: api.Workspace; go: (x: string) => void }) {
  const s = data.summary;
  const cards = [
    ["Total Fee Accounts", s.totalFeeAccounts, "accounts"],
    ["Pending Verifications", s.pendingVerifications, "pending"],
    ["Verified Payments", s.verifiedPayments, "verified"],
    ["Rejected Payments", s.rejectedPayments, "rejected"],
    ["Today's Collection", money(s.todayCollections), "history"],
    ["This Month", money(s.monthCollection), "reports"],
    ["Pending Amount", money(s.pendingAmount), "dues"],
    ["Today's Requests", s.todayPendingRequests, "pending"],
  ] as const;
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
        {cards.map(([label, value, to]) => (
          <button
            onClick={() => go(to)}
            key={label}
            className="rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-300"
          >
            <BadgeIndianRupee className="h-5 w-5 text-emerald-600" />
            <p className="mt-4 text-xl font-bold text-slate-900">{value}</p>
            <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
          </button>
        ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <Chart title="Daily Collection" rows={data.dailyCollection} />
        <Chart title="Monthly Collection" rows={data.monthlyCollection} bar />
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <Chart title="Collection by Department" rows={data.departmentCollection} bar />
        <Panel title="Pending vs Collected" subtitle="Current account balances">
          <div className="h-72">
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={data.pendingVsCollected}
                  dataKey="value"
                  nameKey="label"
                  innerRadius={60}
                  outerRadius={95}
                >
                  {data.pendingVsCollected.map((_, i) => (
                    <Cell key={i} fill={i ? "#f59e0b" : "#10b981"} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => money(Number(v))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>
    </>
  );
}
function Filters({
  data,
  search,
  setSearch,
  status,
  setStatus,
  mode,
  setMode,
  departmentId,
  setDepartmentId,
  academicYear,
  setAcademicYear,
  courseYearId,
  setCourseYearId,
  type,
}: {
  data: api.Workspace;
  search: string;
  setSearch: (v: string) => void;
  status: string;
  setStatus: (v: string) => void;
  mode: string;
  setMode: (v: string) => void;
  departmentId: string;
  setDepartmentId: (v: string) => void;
  academicYear: string;
  setAcademicYear: (v: string) => void;
  courseYearId: string;
  setCourseYearId: (v: string) => void;
  type: string;
}) {
  const statuses =
    type === "accounts"
      ? ["PENDING", "PARTIALLY_PAID", "FULLY_PAID"]
      : type === "pending"
        ? ["PENDING", "RESUBMISSION_REQUESTED"]
        : type === "verified"
          ? ["VERIFIED"]
          : type === "rejected"
            ? ["REJECTED"]
            : ["PENDING", "VERIFIED", "REJECTED", "RESUBMISSION_REQUESTED"];
  return (
    <div className="grid gap-3 rounded-2xl border bg-white p-4 shadow-sm md:grid-cols-2 xl:grid-cols-6">
      <label className="relative xl:col-span-2">
        <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
        <input
          className={`${input} w-full pl-9`}
          placeholder="Student, PRN, receipt or transaction ID"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>
      <select
        className={input}
        value={departmentId}
        onChange={(e) => setDepartmentId(e.target.value)}
      >
        <option value="">All departments</option>
        {data.departments.map((o) => (
          <option key={o.id} value={o.id}>
            {o.label}
          </option>
        ))}
      </select>
      <select
        className={input}
        value={academicYear}
        onChange={(e) => setAcademicYear(e.target.value)}
      >
        <option value="">All academic years</option>
        {data.academicYears.map((y) => (
          <option key={y}>{y}</option>
        ))}
      </select>
      <select
        className={input}
        value={courseYearId}
        onChange={(e) => setCourseYearId(e.target.value)}
      >
        <option value="">All courses</option>
        {data.courseYears.map((o) => (
          <option key={o.id} value={o.id}>
            {o.label}
          </option>
        ))}
      </select>
      <select className={input} value={status} onChange={(e) => setStatus(e.target.value)}>
        <option value="">All statuses</option>
        {statuses.map((s) => (
          <option key={s}>{s}</option>
        ))}
      </select>
      {type !== "accounts" && (
        <select className={input} value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="">All payment modes</option>
          {["UPI", "BANK_TRANSFER", "CASH", "CHEQUE", "OTHER"].map((m) => (
            <option key={m}>{m}</option>
          ))}
        </select>
      )}
    </div>
  );
}
function Accounts({
  data,
  page,
  setPage,
  open,
}: {
  data: api.Workspace;
  page: number;
  setPage: (v: number) => void;
  open: (v: number) => void;
}) {
  return (
    <Panel
      title="Fee Accounts"
      subtitle="Master accounts automatically created for approved students"
    >
      <Table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Department</th>
            <th>Year / Division</th>
            <th>Fee Structure</th>
            <th>Total</th>
            <th>Paid</th>
            <th>Pending</th>
            <th>Status</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {data.accounts.content.map((a) => (
            <tr className="group bg-white" key={a.id}>
              <td className="min-w-64">
                <div className="flex items-center gap-3">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-blue-100 text-sm font-bold text-blue-700">
                    {a.student
                      .split(" ")
                      .slice(0, 2)
                      .map((part) => part[0])
                      .join("")
                      .toUpperCase()}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-slate-900">{a.student}</p>
                    <p className="mt-1 font-mono text-[11px] text-slate-500">{a.prn}</p>
                  </div>
                </div>
              </td>
              <td>
                <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                  {a.department}
                </span>
              </td>
              <td className="min-w-36">
                <p className="font-semibold text-slate-800">{a.course}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {a.division || "Not allocated"} · {a.academicYear}
                </p>
              </td>
              <td className="max-w-48 text-sm font-medium text-slate-600">
                {feeStructureLabel(a.feeStructure)}
              </td>
              <td className="whitespace-nowrap font-semibold text-slate-900">
                {money(a.totalFee)}
              </td>
              <td className="whitespace-nowrap">
                <p className="font-bold text-emerald-700">{money(a.paid)}</p>
                <p className="mt-1 text-[10px] font-semibold uppercase tracking-wide text-emerald-600">
                  Collected
                </p>
              </td>
              <td className="whitespace-nowrap">
                <p className="font-bold text-rose-700">{money(a.pending)}</p>
                <p className="mt-1 text-[10px] font-semibold uppercase tracking-wide text-rose-500">
                  Balance
                </p>
              </td>
              <td>
                <Badge value={a.status} />
              </td>
              <td className="whitespace-nowrap text-right">
                <button
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
                  onClick={() => open(a.id)}
                >
                  <Eye className="h-4 w-4" />
                  View details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      <Pager page={page} pages={data.accounts.totalPages} setPage={setPage} />
    </Panel>
  );
}
function Payments({
  title,
  rows,
  open,
  receipt = false,
}: {
  title: string;
  rows: api.Payment[];
  open: (v: api.Payment) => void;
  receipt?: boolean;
}) {
  return (
    <Panel title={title} subtitle={`${rows.length} matching payment records`}>
      <Table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Department</th>
            <th>Amount</th>
            <th>Mode / Transaction</th>
            <th>Payment Date</th>
            <th>Submitted</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr className="bg-white" key={p.id}>
              <td>
                <StudentIdentity name={p.student} prn={p.prn} />
              </td>
              <td>
                <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                  {p.department}
                </span>
                <p className="mt-1.5 text-xs text-slate-500">
                  {p.course} {p.division}
                </p>
              </td>
              <td className="whitespace-nowrap">
                <p className="font-bold text-slate-900">{money(p.amount)}</p>
                {p.duplicateDetected && (
                  <p className="mt-1 text-xs font-semibold text-rose-600">Duplicate detected</p>
                )}
              </td>
              <td>
                <p className="font-semibold text-slate-800">{p.paymentMode.replaceAll("_", " ")}</p>
                <p className="mt-1 font-mono text-[11px] text-slate-500">{p.transactionId}</p>
              </td>
              <td className="whitespace-nowrap text-sm font-medium">{p.paymentDate}</td>
              <td className="min-w-36 text-xs leading-5 text-slate-500">
                {new Date(p.submittedAt).toLocaleString()}
              </td>
              <td>
                <Badge value={p.status} />
              </td>
              <td className="text-right">
                <button
                  className={
                    receipt
                      ? "inline-flex min-w-40 items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2.5 text-sm font-semibold text-blue-700 shadow-sm transition hover:bg-blue-100"
                      : `${primary} min-w-40`
                  }
                  onClick={() => (receipt ? void downloadReceipt(p) : open(p))}
                >
                  {receipt ? <Download className="h-4 w-4" /> : <ShieldCheck className="h-4 w-4" />}
                  {receipt ? "Download receipt" : "Review & verify"}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      {!rows.length && (
        <Empty title="No payments found" text="Payments matching this view will appear here." />
      )}
    </Panel>
  );
}
function Rejected({ rows, open }: { rows: api.Payment[]; open: (v: api.Payment) => void }) {
  return (
    <Panel
      title="Rejected Payments"
      subtitle="Review rejection reasons and submitted payment proof"
    >
      <Table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Amount</th>
            <th>Reason</th>
            <th>Rejected By</th>
            <th>Date</th>
            <th>Screenshot</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr className="bg-white" key={p.id}>
              <td>
                <StudentIdentity name={p.student} prn={p.prn} />
              </td>
              <td className="whitespace-nowrap font-bold text-slate-900">{money(p.amount)}</td>
              <td className="min-w-56">
                <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700">
                  {p.rejectionReason || "No reason provided"}
                </p>
              </td>
              <td className="font-medium text-slate-700">{p.rejectedBy || "—"}</td>
              <td className="min-w-36 text-xs leading-5 text-slate-500">
                {p.rejectedAt && new Date(p.rejectedAt).toLocaleString()}
              </td>
              <td>
                <button
                  className="font-semibold text-blue-600 hover:text-blue-700"
                  onClick={() => open(p)}
                >
                  View proof
                </button>
              </td>
              <td className="text-right">
                <button className={secondary} onClick={() => open(p)}>
                  Details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Panel>
  );
}
function HistoryView({ rows, open }: { rows: api.Payment[]; open: (v: api.Payment) => void }) {
  return (
    <Panel title="Payment History" subtitle="Complete transaction and verification lifecycle">
      <Table>
        <thead>
          <tr>
            <th>Receipt</th>
            <th>Student</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Officer</th>
            <th>Timeline</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr className="bg-white" key={p.id}>
              <td>
                <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs font-semibold text-slate-700">
                  {p.receiptNumber || "Pending"}
                </span>
              </td>
              <td>
                <StudentIdentity name={p.student} prn={p.prn} />
              </td>
              <td className="whitespace-nowrap font-bold text-slate-900">{money(p.amount)}</td>
              <td>
                <Badge value={p.status} />
              </td>
              <td className="font-medium text-slate-700">
                {p.verifiedBy || p.rejectedBy || "Awaiting officer"}
              </td>
              <td className="min-w-44">
                <p className="text-xs text-slate-500">
                  Uploaded · {new Date(p.submittedAt).toLocaleDateString()}
                </p>
                <p className="mt-1 text-xs font-medium text-slate-700">
                  {p.verifiedAt
                    ? `Verified · ${new Date(p.verifiedAt).toLocaleDateString()}`
                    : p.rejectedAt
                      ? `Reviewed · ${new Date(p.rejectedAt).toLocaleDateString()}`
                      : "Verification pending"}
                </p>
              </td>
              <td className="text-right">
                <button
                  className="inline-flex items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-100"
                  onClick={() => open(p)}
                >
                  <Eye className="h-4 w-4" /> View details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Panel>
  );
}
function Dues({ rows, open }: { rows: api.Due[]; open: (v: number) => void }) {
  const [sending, setSending] = useState(false);
  const sendAllReminders = async () => {
    if (!rows.length) {
      toast.info("There are no students with pending fees");
      return;
    }
    if (
      !window.confirm(
        "Send a pending fee reminder to every student in this college who has an outstanding balance?",
      )
    )
      return;
    setSending(true);
    try {
      const students = await api.sendPendingFeeReminders();
      toast.success(`Fee reminders processed for ${students} student${students === 1 ? "" : "s"}`);
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSending(false);
    }
  };
  return (
    <Panel title="Pending Dues" subtitle="Overdue and upcoming student balances">
      <div className="mb-5 flex flex-col justify-between gap-4 rounded-2xl border border-blue-100 bg-blue-50 p-4 sm:flex-row sm:items-center">
        <div className="flex items-start gap-3">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-blue-600 text-white">
            <Mail className="h-5 w-5" />
          </span>
          <div>
            <p className="font-bold text-slate-900">Remind all students with pending fees</p>
            <p className="mt-1 text-sm text-slate-500">
              Each student receives a personalized email with their outstanding balance.
            </p>
          </div>
        </div>
        <button
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={sending || !rows.length}
          onClick={() => void sendAllReminders()}
        >
          <Mail className="h-4 w-4" />
          {sending ? "Sending reminders…" : "Send reminder to all"}
        </button>
      </div>
      <Table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Department</th>
            <th>Total</th>
            <th>Paid</th>
            <th>Pending</th>
            <th>Due Date</th>
            <th>Last Payment</th>
            <th>Priority</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((d) => (
            <tr
              className={d.urgency === "OVERDUE" ? "bg-rose-50/50" : "bg-white"}
              key={d.accountId}
            >
              <td>
                <StudentIdentity name={d.student} prn={d.prn} />
              </td>
              <td>
                <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                  {d.department}
                </span>
              </td>
              <td className="whitespace-nowrap font-semibold text-slate-900">
                {money(d.totalFee)}
              </td>
              <td className="whitespace-nowrap font-bold text-emerald-700">{money(d.paid)}</td>
              <td className="whitespace-nowrap font-bold text-rose-700">{money(d.pending)}</td>
              <td className="whitespace-nowrap font-medium text-slate-700">{d.dueDate}</td>
              <td className="whitespace-nowrap text-sm text-slate-500">
                {d.lastPayment || "No payment yet"}
              </td>
              <td>
                <Badge value={d.urgency} />
              </td>
              <td className="text-right">
                <button
                  className="inline-flex items-center gap-2 whitespace-nowrap rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
                  onClick={() => open(d.accountId)}
                >
                  <Eye className="h-4 w-4" /> View details
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Panel>
  );
}
type ReportKind = "collection" | "dues" | "fully-paid" | "partial" | "rejected";
type ReportRow = {
  id: string;
  student: string;
  prn: string;
  department: string;
  course: string;
  division: string;
  reference: string;
  amount: number;
  paid: number;
  pending: number;
  status: string;
  date: string;
};
function Reports({ data }: { data: api.Workspace }) {
  const [kind, setKind] = useState<ReportKind>("collection");
  const [department, setDepartment] = useState("");
  const [course, setCourse] = useState("");
  const [division, setDivision] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [draftDepartment, setDraftDepartment] = useState("");
  const [draftCourse, setDraftCourse] = useState("");
  const [draftDivision, setDraftDivision] = useState("");
  const [draftFrom, setDraftFrom] = useState("");
  const [draftTo, setDraftTo] = useState("");
  const [chartRange, setChartRange] = useState<"daily" | "monthly">("daily");
  const [exportOpen, setExportOpen] = useState(false);
  const [reportPage, setReportPage] = useState(0);
  useEffect(() => setReportPage(0), [kind, department, course, division, from, to]);
  const paymentRow = (p: api.Payment): ReportRow => ({
    id: `payment-${p.id}`,
    student: p.student,
    prn: p.prn,
    department: p.department,
    course: p.course,
    division: p.division || "",
    reference: p.receiptNumber || p.transactionId,
    amount: Number(p.amount),
    paid: p.status === "VERIFIED" ? Number(p.amount) : 0,
    pending: 0,
    status: p.status,
    date: p.paymentDate,
  });
  const accountRow = (a: api.Account): ReportRow => ({
    id: `account-${a.id}`,
    student: a.student,
    prn: a.prn,
    department: a.department,
    course: a.course,
    division: a.division || "",
    reference: feeStructureLabel(a.feeStructure),
    amount: Number(a.totalFee),
    paid: Number(a.paid),
    pending: Number(a.pending),
    status: a.status,
    date: a.lastPayment || "",
  });
  const dueRow = (d: api.Due): ReportRow => ({
    id: `due-${d.accountId}`,
    student: d.student,
    prn: d.prn,
    department: d.department,
    course: "",
    division: "",
    reference: "Pending fee",
    amount: Number(d.totalFee),
    paid: Number(d.paid),
    pending: Number(d.pending),
    status: d.urgency,
    date: d.dueDate,
  });
  const source: ReportRow[] =
    kind === "collection"
      ? data.history.filter((p) => p.status === "VERIFIED").map(paymentRow)
      : kind === "dues"
        ? data.dues.map(dueRow)
        : kind === "fully-paid"
          ? data.accounts.content.filter((a) => a.status === "FULLY_PAID").map(accountRow)
          : kind === "partial"
            ? data.accounts.content.filter((a) => a.status === "PARTIALLY_PAID").map(accountRow)
            : data.rejected.map(paymentRow);
  const rows = source.filter(
    (row) =>
      (!department || row.department === department) &&
      (!course || row.course === course) &&
      (!division || row.division === division) &&
      (!from || !row.date || row.date >= from) &&
      (!to || !row.date || row.date <= to),
  );
  const matchesAppliedScope = (row: {
    department: string;
    course: string;
    division?: string | null;
  }) =>
    (!department || row.department === department) &&
    (!course || row.course === course) &&
    (!division || (row.division || "") === division);
  const scopedVerifiedPayments = data.history.filter(
    (payment) => payment.status === "VERIFIED" && matchesAppliedScope(payment),
  );
  const scopedAccounts = data.accounts.content.filter(matchesAppliedScope);
  const localDateKey = (date: Date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  const dailyTrend = Array.from({ length: 14 }, (_, index) => {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - (13 - index));
    const key = localDateKey(date);
    return {
      label: date.toLocaleDateString("en-IN", { day: "2-digit", month: "short" }),
      value: scopedVerifiedPayments
        .filter((payment) => payment.paymentDate === key)
        .reduce((sum, payment) => sum + Number(payment.amount), 0),
    };
  });
  const monthlyTrend = Array.from({ length: 12 }, (_, index) => {
    const date = new Date();
    date.setDate(1);
    date.setHours(0, 0, 0, 0);
    date.setMonth(date.getMonth() - (11 - index));
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
    return {
      label: date.toLocaleDateString("en-IN", { month: "short", year: "2-digit" }),
      value: scopedVerifiedPayments
        .filter((payment) => payment.paymentDate.startsWith(key))
        .reduce((sum, payment) => sum + Number(payment.amount), 0),
    };
  });
  const scopedPaidVsPending = [
    {
      label: "Collected",
      value: scopedAccounts.reduce((sum, account) => sum + Number(account.paid), 0),
    },
    {
      label: "Pending",
      value: scopedAccounts.reduce((sum, account) => sum + Number(account.pending), 0),
    },
  ];
  const pageSize = 8;
  const reportPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const visibleRows = rows.slice(reportPage * pageSize, reportPage * pageSize + pageSize);
  const departments = [...new Set(source.map((row) => row.department).filter(Boolean))].sort();
  const courses = [...new Set(source.map((row) => row.course).filter(Boolean))].sort();
  const divisions = [...new Set(source.map((row) => row.division).filter(Boolean))].sort();
  const collected = data.history
    .filter((p) => p.status === "VERIFIED")
    .reduce((sum, p) => sum + Number(p.amount), 0);
  const pending = data.dues.reduce((sum, d) => sum + Number(d.pending), 0);
  const collectionRate = collected + pending ? (collected / (collected + pending)) * 100 : 0;
  const tabs: { key: ReportKind; label: string }[] = [
    { key: "collection", label: "Collection" },
    { key: "dues", label: "Pending dues" },
    { key: "fully-paid", label: "Fully paid" },
    { key: "partial", label: "Partial payments" },
    { key: "rejected", label: "Rejected" },
  ];
  const applyFilters = () => {
    if (draftFrom && draftTo && draftFrom > draftTo) {
      toast.error("From date must be before the to date");
      return;
    }
    setDepartment(draftDepartment);
    setCourse(draftCourse);
    setDivision(draftDivision);
    setFrom(draftFrom);
    setTo(draftTo);
    setReportPage(0);
  };
  const resetFilters = () => {
    setDepartment("");
    setCourse("");
    setDivision("");
    setFrom("");
    setTo("");
    setDraftDepartment("");
    setDraftCourse("");
    setDraftDivision("");
    setDraftFrom("");
    setDraftTo("");
    setReportPage(0);
  };
  const exportRows = rows.map(({ id, ...row }) => {
    void id;
    return row;
  });
  const downloadCsv = () => {
    const keys = Object.keys(exportRows[0] || {});
    const content = [
      keys.join(","),
      ...exportRows.map((row) =>
        keys
          .map(
            (key) =>
              `"${String((row as unknown as Record<string, unknown>)[key] ?? "").replaceAll('"', '""')}"`,
          )
          .join(","),
      ),
    ].join("\n");
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `${kind}-fee-report.csv`;
    link.click();
    URL.revokeObjectURL(url);
    setExportOpen(false);
  };
  const downloadExcel = async () => {
    const { default: XLSX } = await import("xlsx-js-style");
    const sheet = XLSX.utils.json_to_sheet(exportRows);
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, sheet, "Fee Report");
    XLSX.writeFile(book, `${kind}-fee-report.xlsx`);
    setExportOpen(false);
  };
  return (
    <div className="space-y-5">
      <section className="flex flex-col justify-between gap-4 rounded-2xl border border-blue-100 bg-gradient-to-r from-blue-50 to-indigo-50 p-5 sm:flex-row sm:items-center">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">
            Fee analytics
          </p>
          <h2 className="mt-1 text-2xl font-bold text-slate-900">Reports & collection insights</h2>
          <p className="mt-1 text-sm text-slate-500">
            Explore live fee data, compare performance, and export the current view.
          </p>
        </div>
        <div className="relative">
          <button
            className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
            onClick={() => setExportOpen((open) => !open)}
          >
            <Download className="h-4 w-4" /> Download report
          </button>
          {exportOpen && (
            <div className="absolute right-0 top-12 z-20 w-44 rounded-xl border bg-white p-2 shadow-xl">
              <button
                className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-blue-50"
                onClick={() => void downloadExcel()}
              >
                Excel workbook
              </button>
              <button
                className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-blue-50"
                onClick={downloadCsv}
              >
                CSV file
              </button>
              <button
                className="w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-blue-50"
                onClick={() => window.print()}
              >
                Print / PDF
              </button>
            </div>
          )}
        </div>
      </section>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {[
          ["Total collected", money(collected), "text-emerald-700", "bg-emerald-50"],
          ["Pending amount", money(pending), "text-amber-700", "bg-amber-50"],
          ["Collection rate", `${collectionRate.toFixed(1)}%`, "text-blue-700", "bg-blue-50"],
          ["Verified payments", data.summary.verifiedPayments, "text-indigo-700", "bg-indigo-50"],
          ["Rejected payments", data.summary.rejectedPayments, "text-rose-700", "bg-rose-50"],
        ].map(([label, value, color, background]) => (
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" key={label}>
            <span className={`grid h-9 w-9 place-items-center rounded-xl ${background}`}>
              <BadgeIndianRupee className={`h-4 w-4 ${color}`} />
            </span>
            <p className="mt-3 text-xl font-bold text-slate-900">{value}</p>
            <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
          </div>
        ))}
      </div>

      <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="rounded-2xl border border-blue-100 bg-blue-50/60 p-4">
          <div className="mb-3 flex items-center gap-3">
            <span className="grid h-8 w-8 place-items-center rounded-full bg-blue-600 text-xs font-bold text-white">
              1
            </span>
            <div>
              <h3 className="font-bold text-slate-900">Select academic scope</h3>
              <p className="text-xs text-slate-500">Choose department, course year and division.</p>
            </div>
          </div>
          <div className="grid gap-3 md:grid-cols-3">
            <label className="space-y-1.5">
              <span className="text-xs font-semibold text-slate-600">Department</span>
              <select
                className={`${input} w-full`}
                value={draftDepartment}
                onChange={(event) => setDraftDepartment(event.target.value)}
              >
                <option value="">All departments</option>
                {departments.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <label className="space-y-1.5">
              <span className="text-xs font-semibold text-slate-600">Course year</span>
              <select
                className={`${input} w-full`}
                value={draftCourse}
                onChange={(event) => setDraftCourse(event.target.value)}
              >
                <option value="">All course years</option>
                {courses.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <label className="space-y-1.5">
              <span className="text-xs font-semibold text-slate-600">Division</span>
              <select
                className={`${input} w-full`}
                value={draftDivision}
                onChange={(event) => setDraftDivision(event.target.value)}
              >
                <option value="">All divisions</option>
                {divisions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="mb-3 flex items-center gap-3">
            <span className="grid h-8 w-8 place-items-center rounded-full bg-blue-600 text-xs font-bold text-white">
              2
            </span>
            <div>
              <h3 className="font-bold text-slate-900">Select date range</h3>
              <p className="text-xs text-slate-500">
                Limit the report to a specific reporting period.
              </p>
            </div>
          </div>
          <div className="grid max-w-2xl gap-3 sm:grid-cols-2">
            <label className="space-y-1.5">
              <span className="text-xs font-semibold text-slate-600">From date</span>
              <input
                className={`${input} w-full`}
                type="date"
                value={draftFrom}
                onChange={(event) => setDraftFrom(event.target.value)}
              />
            </label>
            <label className="space-y-1.5">
              <span className="text-xs font-semibold text-slate-600">To date</span>
              <input
                className={`${input} w-full`}
                type="date"
                value={draftTo}
                onChange={(event) => setDraftTo(event.target.value)}
              />
            </label>
          </div>
        </div>

        <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:justify-end">
          <button className={`${secondary} sm:min-w-32`} onClick={resetFilters}>
            Reset filters
          </button>
          <button
            className="rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 sm:min-w-36"
            onClick={applyFilters}
          >
            Apply filters
          </button>
        </div>
      </section>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {tabs.map((tab) => (
          <button
            className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition ${kind === tab.key ? "bg-blue-600 text-white shadow-sm" : "border border-slate-200 bg-white text-slate-600 hover:bg-blue-50"}`}
            key={tab.key}
            onClick={() => {
              setKind(tab.key);
              setReportPage(0);
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.45fr_.85fr]">
        <Panel
          title="Collection trend"
          subtitle="Applied academic scope across the full daily or monthly timeline"
        >
          <div className="mb-3 flex justify-end gap-1">
            {(["daily", "monthly"] as const).map((range) => (
              <button
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold ${chartRange === range ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600"}`}
                key={range}
                onClick={() => setChartRange(range)}
              >
                {range[0].toUpperCase() + range.slice(1)}
              </button>
            ))}
          </div>
          <div className="h-72">
            <ResponsiveContainer>
              <AreaChart data={chartRange === "daily" ? dailyTrend : monthlyTrend}>
                <defs>
                  <linearGradient id="reportCollection" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0.03} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" />
                <YAxis />
                <Tooltip formatter={(value) => money(Number(value))} />
                <Area
                  dataKey="value"
                  stroke="#2563eb"
                  strokeWidth={3}
                  fill="url(#reportCollection)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="Paid vs pending" subtitle="Current balances for the applied academic scope">
          <div className="h-[322px]">
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={scopedPaidVsPending}
                  dataKey="value"
                  nameKey="label"
                  innerRadius={70}
                  outerRadius={105}
                  paddingAngle={3}
                >
                  <Cell fill="#2563eb" />
                  <Cell fill="#f59e0b" />
                </Pie>
                <Tooltip formatter={(value) => money(Number(value))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>

      <Panel
        title={`${tabs.find((tab) => tab.key === kind)?.label} report`}
        subtitle={`${rows.length} records in the current view`}
      >
        <Table>
          <thead>
            <tr>
              <th>Student</th>
              <th>Department</th>
              <th>Reference</th>
              <th>Total</th>
              <th>Paid</th>
              <th>Pending</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {visibleRows.map((row) => (
              <tr className="bg-white" key={row.id}>
                <td>
                  <StudentIdentity name={row.student} prn={row.prn} />
                </td>
                <td>
                  <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold">
                    {row.department}
                  </span>
                </td>
                <td className="font-mono text-xs text-slate-600">{row.reference}</td>
                <td className="whitespace-nowrap font-semibold">{money(row.amount)}</td>
                <td className="whitespace-nowrap font-bold text-emerald-700">{money(row.paid)}</td>
                <td className="whitespace-nowrap font-bold text-amber-700">{money(row.pending)}</td>
                <td>
                  <Badge value={row.status} />
                </td>
                <td className="whitespace-nowrap text-sm text-slate-500">{row.date || "—"}</td>
              </tr>
            ))}
          </tbody>
        </Table>
        {!visibleRows.length && (
          <Empty title="No report data" text="Adjust the filters or select another report." />
        )}
        <Pager page={reportPage} pages={reportPages} setPage={setReportPage} />
      </Panel>
    </div>
  );
}
function PaymentReview({
  payment: p,
  close,
  reload,
}: {
  payment: api.Payment;
  close: () => void;
  reload: () => Promise<void>;
}) {
  const [zoom, setZoom] = useState(false),
    [remarks, setRemarks] = useState(""),
    [action, setAction] = useState<"verify" | "reject" | "resubmit" | null>(null),
    [proof, setProof] = useState<{ url: string; type: string } | null>(null),
    [proofError, setProofError] = useState(""),
    [proofLoading, setProofLoading] = useState(true),
    [proofRetry, setProofRetry] = useState(0);
  useEffect(() => {
    let active = true;
    let objectUrl = "";
    setProof(null);
    setProofError("");
    setProofLoading(true);
    void api
      .paymentProof(p.id)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setProof({ url: objectUrl, type: blob.type });
      })
      .catch((error) => {
        if (active) setProofError(handleApiError(error).message);
      })
      .finally(() => {
        if (active) setProofLoading(false);
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [p.id, proofRetry]);
  const act = async (type: "verify" | "reject" | "resubmit") => {
    if (action) return;
    setAction(type);
    try {
      if (type === "verify") await api.verify(p.id, remarks);
      else {
        const reason = remarks.trim();
        if (reason.length < 5) return toast.error("Enter a review reason of at least 5 characters");
        if (type === "reject") await api.reject(p.id, reason);
        else await api.resubmit(p.id, reason);
      }
      toast.success(
        type === "verify"
          ? "Payment approved"
          : type === "reject"
            ? "Payment rejected"
            : "New screenshot requested",
      );
      close();
      await reload();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setAction(null);
    }
  };
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/60 p-4">
      <div className="mx-auto my-5 max-w-7xl rounded-3xl bg-white p-6 shadow-2xl">
        <div className="mb-5 flex justify-between">
          <div>
            <h2 className="text-2xl font-bold">Payment Verification</h2>
            <p className="text-sm text-slate-500">
              Submitted {new Date(p.submittedAt).toLocaleString()}
            </p>
          </div>
          <button className={secondary} onClick={close}>
            Close
          </button>
        </div>
        <div className="grid gap-5 xl:grid-cols-[.8fr_1.4fr_.8fr]">
          <Panel title="Student Information" subtitle="Account and fee position">
            <Info label="Student" value={p.student} />
            <Info label="PRN" value={p.prn} />
            <Info label="Department" value={p.department} />
            <Info label="Course / Division" value={`${p.course} ${p.division || ""}`} />
            <Info label="Remaining Fee" value={money(p.remainingFee)} />
            <Info label="Previous Payments" value={String(p.previousPayments)} />
            <Info label="Current Installment" value={String(p.currentInstallment)} />
            {p.duplicateDetected && (
              <div className="mt-3 rounded-xl bg-rose-50 p-3 text-sm font-bold text-rose-700">
                Duplicate Transaction Detected
              </div>
            )}
          </Panel>
          <Panel title="Payment Screenshot" subtitle="Click to zoom and inspect the uploaded proof">
            {proofLoading && (
              <div className="grid min-h-80 place-items-center rounded-2xl bg-slate-50 text-sm font-medium text-slate-500">
                Loading payment proof...
              </div>
            )}
            {!proofLoading && proofError && (
              <div className="grid min-h-80 place-items-center rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center">
                <div>
                  <p className="font-semibold text-rose-700">
                    Payment proof could not be displayed
                  </p>
                  <p className="mt-1 text-sm text-rose-600">{proofError}</p>
                  <button
                    className={`${secondary} mt-4`}
                    onClick={() => setProofRetry((value) => value + 1)}
                  >
                    <RefreshCw className="h-4 w-4" /> Retry
                  </button>
                </div>
              </div>
            )}
            {proof && proof.type === "application/pdf" && (
              <iframe
                src={proof.url}
                title="Payment proof"
                className="h-[560px] w-full rounded-2xl border border-slate-200"
              />
            )}
            {proof && proof.type !== "application/pdf" && (
              <button
                type="button"
                className="relative grid min-h-80 w-full cursor-zoom-in place-items-center overflow-hidden rounded-2xl bg-slate-100"
                onClick={() => setZoom(true)}
                aria-label="Open payment proof in full screen"
              >
                <img
                  src={proof.url}
                  alt="Payment proof"
                  className="mx-auto max-h-[560px] object-contain"
                />
                <span className="absolute right-3 top-3 rounded-lg bg-slate-950/70 p-2 text-white">
                  <Maximize2 className="h-4 w-4" />
                </span>
              </button>
            )}
          </Panel>
          <Panel title="Payment Details" subtitle="Verify against the uploaded screenshot">
            <Info label="Amount Paid" value={money(p.amount)} />
            <Info label="Transaction ID" value={p.transactionId} />
            <Info label="Payment Date" value={p.paymentDate} />
            <Info label="Payment Method" value={p.paymentMode} />
            <Info label="Student Remarks" value={p.remarks || "—"} />
            <div className="mt-4 rounded-2xl border border-blue-100 bg-blue-50/60 p-4">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-bold text-slate-900">Verification checklist</h3>
                <span className="text-[11px] font-semibold text-blue-700">Check before action</span>
              </div>
              <div className="mt-3 space-y-2.5">
                <VerificationCheck label="Payment proof loaded" complete={Boolean(proof)} />
                <VerificationCheck
                  label="Amount and transaction ID provided"
                  complete={Number(p.amount) > 0 && Boolean(p.transactionId?.trim())}
                />
                <VerificationCheck
                  label="No duplicate transaction detected"
                  complete={!p.duplicateDetected}
                />
                <VerificationCheck
                  label="No amount mismatch reported"
                  complete={!p.amountMismatch}
                />
              </div>
            </div>
            <label className="mt-4 block">
              <span className="text-xs font-bold text-slate-700">
                Officer remarks / rejection reason
              </span>
              <textarea
                className="mt-2 min-h-24 w-full rounded-xl border border-slate-200 p-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
                placeholder="Add a note, or enter at least 5 characters when rejecting..."
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            </label>
            {p.status === "PENDING" && (
              <div className="mt-4 grid gap-2">
                <button
                  className={primary}
                  disabled={Boolean(action)}
                  onClick={() => void act("verify")}
                >
                  {action === "verify" ? (
                    <RefreshCw className="h-4 w-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="h-4 w-4" />
                  )}
                  Verify & approve
                </button>
                <button
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rose-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-rose-100 disabled:cursor-not-allowed disabled:opacity-40"
                  disabled={Boolean(action)}
                  onClick={() => void act("reject")}
                >
                  {action === "reject" ? (
                    <RefreshCw className="h-4 w-4 animate-spin" />
                  ) : (
                    <XCircle className="h-4 w-4" />
                  )}
                  Reject payment
                </button>
                <button
                  className={secondary}
                  disabled={Boolean(action)}
                  onClick={() => void act("resubmit")}
                >
                  <RefreshCw className={`h-4 w-4 ${action === "resubmit" ? "animate-spin" : ""}`} />
                  Request new proof
                </button>
                <button
                  className="inline-flex items-center justify-center gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-xs font-semibold text-amber-700 transition hover:bg-amber-100"
                  onClick={() =>
                    setRemarks("Amount mismatch between submitted amount and screenshot")
                  }
                >
                  <CircleAlert className="h-4 w-4" />
                  Mark amount mismatch
                </button>
              </div>
            )}
          </Panel>
        </div>
      </div>
      {proof && (
        <DocumentViewer
          open={zoom}
          url={proof.url}
          contentType={proof.type}
          title={`Payment proof — ${p.student}`}
          filename={`payment-proof-${p.id}`}
          onClose={() => setZoom(false)}
        />
      )}
    </div>
  );
}

function VerificationCheck({ label, complete }: { label: string; complete: boolean }) {
  return (
    <div className="flex items-center gap-2 text-xs font-semibold">
      {complete ? (
        <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
      ) : (
        <CircleAlert className="h-4 w-4 shrink-0 text-amber-600" />
      )}
      <span className={complete ? "text-slate-700" : "text-amber-800"}>{label}</span>
    </div>
  );
}
function AccountModal({
  id,
  close,
  payment,
}: {
  id: number;
  close: () => void;
  payment: (p: api.Payment) => void;
}) {
  const [d, setD] = useState<api.AccountDetail | null>(null);
  useEffect(() => {
    api
      .account(id)
      .then(setD)
      .catch((e) => toast.error(handleApiError(e).message));
  }, [id]);
  if (!d)
    return (
      <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 text-white">
        Loading account…
      </div>
    );
  const a = d.account;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/60 p-4">
      <div className="mx-auto my-5 max-w-5xl rounded-3xl bg-white p-6">
        <div className="flex justify-between">
          <div>
            <h2 className="text-2xl font-bold">{a.student}</h2>
            <p className="text-sm text-slate-500">
              {a.prn} · {a.department}
            </p>
          </div>
          <button className={secondary} onClick={close}>
            Close
          </button>
        </div>
        <div className="mt-5 grid gap-4 sm:grid-cols-3">
          <Metric label="Total Fee" value={money(a.totalFee)} />
          <Metric label="Paid" value={money(a.paid)} />
          <Metric label="Pending" value={money(a.pending)} />
        </div>
        <h3 className="mt-7 font-bold">Payment Timeline & Receipts</h3>
        <div className="mt-3 space-y-3">
          {d.payments.map((p) => (
            <div
              className="grid gap-3 rounded-xl bg-slate-50 p-4 sm:grid-cols-[minmax(0,1fr)_140px_180px] sm:items-center"
              key={p.id}
            >
              <div>
                <p className="font-bold">
                  {money(p.amount)} · {p.paymentMode}
                </p>
                <p className="text-xs text-slate-500">
                  {p.transactionId} · {p.paymentDate}
                </p>
              </div>
              <div className="sm:flex sm:justify-center">
                <Badge value={p.status} />
              </div>
              <div className="flex gap-2 sm:justify-end">
                <button className={secondary} onClick={() => payment(p)}>
                  View
                </button>
                {p.receiptNumber && (
                  <button className={primary} onClick={() => void downloadReceipt(p)}>
                    Receipt
                  </button>
                )}
              </div>
            </div>
          ))}
          {!d.payments.length && (
            <Empty title="No payments" text="Student payments will appear here." />
          )}
        </div>
      </div>
    </div>
  );
}
async function downloadReceipt(p: api.Payment) {
  try {
    await downloadFeeReceipt(await api.paymentReceipt(p.id));
  } catch (error) {
    toast.error(handleApiError(error).message);
  }
}
function Chart({
  title,
  rows,
  bar = false,
}: {
  title: string;
  rows: api.MoneyPoint[];
  bar?: boolean;
}) {
  return (
    <Panel title={title} subtitle="Verified payment collection">
      <div className="h-72">
        <ResponsiveContainer>
          {bar ? (
            <BarChart data={rows}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip formatter={(v) => money(Number(v))} />
              <Bar dataKey="value" fill="#059669" radius={[7, 7, 0, 0]} />
            </BarChart>
          ) : (
            <AreaChart data={rows}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis
                dataKey="label"
                interval={0}
                minTickGap={0}
                height={48}
                angle={-28}
                textAnchor="end"
                tick={{ fontSize: 11 }}
              />
              <YAxis />
              <Tooltip formatter={(v) => money(Number(v))} />
              <Area dataKey="value" stroke="#059669" fill="#d1fae5" />
            </AreaChart>
          )}
        </ResponsiveContainer>
      </div>
    </Panel>
  );
}
function StudentIdentity({ name, prn }: { name: string; prn: string }) {
  const initials = name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  return (
    <div className="flex min-w-56 items-center gap-3">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-blue-100 text-sm font-bold text-blue-700">
        {initials}
      </span>
      <div className="min-w-0">
        <p className="truncate font-semibold text-slate-900">{name}</p>
        <p className="mt-1 font-mono text-[11px] text-slate-500">{prn}</p>
      </div>
    </div>
  );
}
function Table({ children }: { children: React.ReactNode }) {
  return (
    <div className="responsive-table rounded-2xl border border-slate-200">
      <table className="w-full">{children}</table>
    </div>
  );
}
function Panel({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-5">
        <h2 className="font-bold text-slate-900">{title}</h2>
        <p className="mt-1 text-xs text-slate-500">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}
function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-2xl border bg-white p-5">
      <p className="text-2xl font-bold">{value}</p>
      <p className="text-xs font-semibold text-slate-500">{label}</p>
    </div>
  );
}
function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3 border-b py-3 text-sm">
      <span className="text-slate-500">{label}</span>
      <b className="text-right">{value}</b>
    </div>
  );
}
function Badge({ value }: { value: string }) {
  const good = ["VERIFIED", "FULLY_PAID", "PAID"].includes(value),
    bad = ["REJECTED", "OVERDUE"].includes(value),
    warn = ["PENDING", "PARTIALLY_PAID", "RESUBMISSION_REQUESTED", "DUE_THIS_WEEK"].includes(value);
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${good ? "bg-emerald-50 text-emerald-700" : bad ? "bg-rose-50 text-rose-700" : warn ? "bg-amber-50 text-amber-700" : "bg-slate-100 text-slate-600"}`}
    >
      {value.replaceAll("_", " ")}
    </span>
  );
}
function Pager({
  page,
  pages,
  setPage,
}: {
  page: number;
  pages: number;
  setPage: (v: number) => void;
}) {
  return (
    <div className="mt-4 flex justify-end gap-2">
      <button className={secondary} disabled={page === 0} onClick={() => setPage(page - 1)}>
        Previous
      </button>
      <span className="px-3 py-2 text-sm">
        {page + 1} / {Math.max(1, pages)}
      </span>
      <button className={secondary} disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>
        Next
      </button>
    </div>
  );
}
function Empty({ title, text }: { title: string; text: string }) {
  return (
    <div className="py-10 text-center">
      <p className="font-semibold">{title}</p>
      <p className="text-sm text-slate-400">{text}</p>
    </div>
  );
}
function Skeleton() {
  return (
    <div className="animate-pulse space-y-5">
      <div className="h-44 rounded-3xl bg-slate-200" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-32 rounded-2xl bg-slate-100" />
        ))}
      </div>
    </div>
  );
}
