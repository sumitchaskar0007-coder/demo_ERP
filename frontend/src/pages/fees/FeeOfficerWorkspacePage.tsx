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
  AlertTriangle,
  BadgeIndianRupee,
  CheckCircle2,
  Clock3,
  Download,
  Eye,
  FileText,
  History,
  LayoutDashboard,
  Maximize2,
  RefreshCw,
  Search,
  Users,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import { handleApiError } from "@/lib/handleApiError";
import * as api from "@/features/feeOfficer/api";
const tabs = [
  ["dashboard", "Dashboard", LayoutDashboard],
  ["accounts", "Fee Accounts", Users],
  ["pending", "Pending Verifications", Clock3],
  ["verified", "Verified Payments", CheckCircle2],
  ["rejected", "Rejected Payments", XCircle],
  ["history", "Payment History", History],
  ["dues", "Pending Dues", AlertTriangle],
  ["reports", "Fee Reports", FileText],
] as const;
const input =
  "h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100";
const primary =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-40";
const secondary =
  "inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40";
const money = (v: number) => `₹${Number(v || 0).toLocaleString("en-IN")}`;
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
    <div className="mx-auto max-w-[1550px] space-y-6 pb-10">
      <header className="overflow-hidden rounded-3xl bg-gradient-to-r from-slate-950 via-emerald-950 to-teal-900 p-7 text-white shadow-xl">
        <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold">
              Fee Section Officer
            </span>
            <h1 className="mt-4 text-3xl font-bold">Fee Collection & Verification</h1>
            <p className="mt-2 text-sm text-emerald-100">
              Accounts, student payments, dues, receipts and collection analytics
            </p>
          </div>
          <button
            className="rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold hover:bg-white/20"
            onClick={() => void load()}
          >
            <RefreshCw className="mr-2 inline h-4 w-4" />
            Refresh
          </button>
        </div>
      </header>
      <div className="overflow-x-auto rounded-2xl border bg-white p-2 shadow-sm">
        <div className="flex min-w-max gap-1">
          {tabs.map(([key, label, Icon]) => (
            <button
              key={key}
              onClick={() => setParams({ tab: key })}
              className={`flex items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-semibold ${tab === key ? "bg-emerald-600 text-white" : "text-slate-600 hover:bg-slate-100"}`}
            >
              <Icon className="h-4 w-4" />
              {label}
              {key === "pending" && data.summary.pendingVerifications > 0 && (
                <span className="rounded-full bg-rose-500 px-1.5 text-[10px] text-white">
                  {data.summary.pendingVerifications}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>
      {tab === "dashboard" && <Dashboard data={data} go={(x) => setParams({ tab: x })} />}{" "}
      {tab !== "dashboard" && tab !== "dues" && (
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
      {tab === "rejected" && <Rejected rows={data.rejected} open={setPayment} reload={load} />}{" "}
      {tab === "history" && <HistoryView rows={data.history} open={setPayment} />}{" "}
      {tab === "dues" && <Dues rows={data.dues} open={setAccountId} />}{" "}
      {tab === "reports" && <Reports data={data} />}{" "}
      {payment && <PaymentReview payment={payment} close={() => setPayment(null)} reload={load} />}{" "}
      {accountId && (
        <AccountModal id={accountId} close={() => setAccountId(null)} payment={setPayment} />
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
      <div className="grid gap-5 lg:grid-cols-2 xl:grid-cols-5">
        <Widget
          title="Today's Pending Payments"
          rows={data.pending.slice(0, 6)}
          go={() => go("pending")}
        />
        <Widget title="Recent Approvals" rows={data.recentApprovals} go={() => go("verified")} />
        <Widget title="Recent Rejections" rows={data.recentRejections} go={() => go("rejected")} />
        <Widget title="Large Payments" rows={data.largePayments} go={() => go("history")} />
        <Panel title="Pending Dues" subtitle="Highest priority accounts">
          <div className="space-y-3">
            {data.studentsWithPendingDues.slice(0, 6).map((d) => (
              <div key={d.accountId}>
                <p className="text-sm font-semibold">{d.student}</p>
                <p className="text-xs text-rose-600">
                  {money(d.pending)} · {d.urgency.replaceAll("_", " ")}
                </p>
              </div>
            ))}
          </div>
          <button className={`${secondary} mt-4 w-full`} onClick={() => go("dues")}>
            View all
          </button>
        </Panel>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label="Fully Paid Students" value={s.fullyPaid} />
        <Metric label="Partially Paid" value={s.partiallyPaid} />
        <Metric label="Average Verification" value={`${s.averageVerificationMinutes} min`} />
        <Metric label="Pending Requests Today" value={s.todayPendingRequests} />
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
            <tr key={a.id}>
              <td>
                <b>{a.student}</b>
                <small>{a.prn}</small>
              </td>
              <td>{a.department}</td>
              <td>
                {a.course}
                <small>
                  {a.division || "Not allocated"} · {a.academicYear}
                </small>
              </td>
              <td>{a.feeStructure}</td>
              <td>{money(a.totalFee)}</td>
              <td className="text-emerald-600">{money(a.paid)}</td>
              <td className="text-rose-600">{money(a.pending)}</td>
              <td>
                <Badge value={a.status} />
              </td>
              <td>
                <button className={secondary} onClick={() => open(a.id)}>
                  <Eye className="h-4 w-4" />
                  View Account
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
            <th>Proof</th>
            <th>Status</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td>
                <b>{p.student}</b>
                <small>{p.prn}</small>
              </td>
              <td>
                {p.department}
                <small>
                  {p.course} {p.division}
                </small>
              </td>
              <td className="font-bold">
                {money(p.amount)}
                {p.duplicateDetected && <small className="text-rose-600">Duplicate detected</small>}
              </td>
              <td>
                {p.paymentMode}
                <small>{p.transactionId}</small>
              </td>
              <td>{p.paymentDate}</td>
              <td>{new Date(p.submittedAt).toLocaleString()}</td>
              <td>
                <a
                  className="font-semibold text-emerald-600"
                  href={p.proofUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  View Screenshot
                </a>
              </td>
              <td>
                <Badge value={p.status} />
              </td>
              <td>
                <button
                  className={primary}
                  onClick={() => (receipt ? void downloadReceipt(p) : open(p))}
                >
                  {receipt ? <Download className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  {receipt ? "Receipt" : "Review"}
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
function Rejected({
  rows,
  open,
  reload,
}: {
  rows: api.Payment[];
  open: (v: api.Payment) => void;
  reload: () => Promise<void>;
}) {
  const reopen = async (p: api.Payment) => {
    try {
      await api.reopen(p.id);
      toast.success("Payment reopened");
      await reload();
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  return (
    <Panel
      title="Rejected Payments"
      subtitle="Review reasons, approve later, or request a new screenshot"
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
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td>
                <b>{p.student}</b>
                <small>{p.prn}</small>
              </td>
              <td>{money(p.amount)}</td>
              <td className="text-rose-600">{p.rejectionReason}</td>
              <td>{p.rejectedBy}</td>
              <td>{p.rejectedAt && new Date(p.rejectedAt).toLocaleString()}</td>
              <td>
                <a href={p.proofUrl} target="_blank" rel="noreferrer" className="text-emerald-600">
                  View
                </a>
              </td>
              <td>
                <div className="flex gap-2">
                  <button className={secondary} onClick={() => open(p)}>
                    Details
                  </button>
                  <button className={primary} onClick={() => void reopen(p)}>
                    Approve Later
                  </button>
                </div>
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
            <th>Installment</th>
            <th>Status</th>
            <th>Officer</th>
            <th>Timeline</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td>{p.receiptNumber || "Pending"}</td>
              <td>
                <b>{p.student}</b>
                <small>{p.prn}</small>
              </td>
              <td>{money(p.amount)}</td>
              <td>Installment {p.currentInstallment}</td>
              <td>
                <Badge value={p.status} />
              </td>
              <td>{p.verifiedBy || p.rejectedBy || "Awaiting officer"}</td>
              <td>
                <small>Uploaded · {new Date(p.submittedAt).toLocaleDateString()}</small>
                <small>
                  {p.verifiedAt
                    ? `Verified · ${new Date(p.verifiedAt).toLocaleDateString()}`
                    : p.rejectedAt
                      ? `Reviewed · ${new Date(p.rejectedAt).toLocaleDateString()}`
                      : "Verification pending"}
                </small>
              </td>
              <td>
                <button className={secondary} onClick={() => open(p)}>
                  View
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
  return (
    <Panel title="Pending Dues" subtitle="Overdue and upcoming student balances">
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
            <tr className={d.urgency === "OVERDUE" ? "bg-rose-50/50" : ""} key={d.accountId}>
              <td>
                <b>{d.student}</b>
                <small>{d.prn}</small>
              </td>
              <td>{d.department}</td>
              <td>{money(d.totalFee)}</td>
              <td>{money(d.paid)}</td>
              <td className="font-bold text-rose-600">{money(d.pending)}</td>
              <td>{d.dueDate}</td>
              <td>{d.lastPayment || "No payment"}</td>
              <td>
                <Badge value={d.urgency} />
              </td>
              <td>
                <button className={secondary} onClick={() => open(d.accountId)}>
                  View History
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Panel>
  );
}
function Reports({ data }: { data: api.Workspace }) {
  const exportExcel = async (kind: string) => {
    const { default: XLSX } = await import("xlsx-js-style");
    const source =
      kind === "Pending Fees"
        ? data.dues
        : kind === "Rejected Payments"
          ? data.rejected
          : kind === "Fully Paid"
            ? data.accounts.content.filter((a) => a.status === "FULLY_PAID")
            : kind === "Partial Payments"
              ? data.accounts.content.filter((a) => a.status === "PARTIALLY_PAID")
              : data.history;
    const sheet = XLSX.utils.json_to_sheet(source);
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, sheet, kind.slice(0, 31));
    XLSX.writeFile(book, `${kind.toLowerCase().replaceAll(" ", "-")}.xlsx`);
  };
  const csv = (kind: string) => {
    const rows = kind === "Pending Fees" ? data.dues : data.history;
    const keys = Object.keys(rows[0] || {});
    const value = [
      keys.join(","),
      ...rows.map((r) =>
        keys
          .map(
            (k) =>
              `"${String((r as unknown as Record<string, unknown>)[k] ?? "").replaceAll('"', '""')}"`,
          )
          .join(","),
      ),
    ].join("\n");
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([value], { type: "text/csv" }));
    a.download = `${kind}.csv`;
    a.click();
  };
  const reports = [
    "Department Collection",
    "Course Collection",
    "Division Collection",
    "Daily Collection",
    "Monthly Collection",
    "Pending Fees",
    "Partial Payments",
    "Fully Paid",
    "Rejected Payments",
  ];
  return (
    <Panel title="Fee Reports" subtitle="Collection, payment status and pending fee reports">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {reports.map((r) => (
          <div className="rounded-2xl border p-5" key={r}>
            <FileText className="h-6 w-6 text-emerald-600" />
            <p className="mt-3 font-bold">{r} Report</p>
            <div className="mt-4 flex gap-2">
              <button className={primary} onClick={() => void exportExcel(r)}>
                Excel
              </button>
              <button className={secondary} onClick={() => csv(r)}>
                CSV
              </button>
              <button className={secondary} onClick={() => window.print()}>
                PDF
              </button>
            </div>
          </div>
        ))}
      </div>
    </Panel>
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
    [remarks, setRemarks] = useState("");
  const act = async (type: string) => {
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
            <button
              className="relative block w-full overflow-hidden rounded-2xl bg-slate-100"
              onClick={() => setZoom(!zoom)}
            >
              <img
                src={p.proofUrl}
                alt="Payment proof"
                className={`mx-auto object-contain ${zoom ? "max-h-none" : "max-h-[560px]"}`}
              />
              <span className="absolute right-3 top-3 rounded-lg bg-slate-950/70 p-2 text-white">
                <Maximize2 className="h-4 w-4" />
              </span>
            </button>
          </Panel>
          <Panel title="Payment Details" subtitle="Verify against the uploaded screenshot">
            <Info label="Amount Paid" value={money(p.amount)} />
            <Info label="Transaction ID" value={p.transactionId} />
            <Info label="Payment Date" value={p.paymentDate} />
            <Info label="Payment Method" value={p.paymentMode} />
            <Info label="Student Remarks" value={p.remarks || "—"} />
            <textarea
              className="mt-4 min-h-24 w-full rounded-xl border p-3 text-sm"
              placeholder="Officer remarks / rejection reason"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
            {p.status === "PENDING" && (
              <div className="mt-4 grid gap-2">
                <button className={primary} onClick={() => void act("verify")}>
                  Approve Payment
                </button>
                <button
                  className="rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white"
                  onClick={() => void act("reject")}
                >
                  Reject Payment
                </button>
                <button className={secondary} onClick={() => void act("resubmit")}>
                  Request New Screenshot
                </button>
                <button
                  className="text-xs font-semibold text-amber-600"
                  onClick={() =>
                    setRemarks("Amount mismatch between submitted amount and screenshot")
                  }
                >
                  Mark Amount Mismatch
                </button>
              </div>
            )}
          </Panel>
        </div>
      </div>
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
        <h3 className="mt-7 font-bold">Installments</h3>
        <div className="mt-3 grid gap-3 sm:grid-cols-3">
          {d.installments.map((i) => (
            <div className="rounded-xl border p-4" key={i.number}>
              <p className="font-bold">
                {i.number}
                {i.number === 1 ? "st" : i.number === 2 ? "nd" : "rd"} Installment
              </p>
              <p>{money(i.amount)}</p>
              <p className="text-xs text-slate-500">Due {i.dueDate}</p>
              <Badge value={i.status} />
            </div>
          ))}
        </div>
        <h3 className="mt-7 font-bold">Payment Timeline & Receipts</h3>
        <div className="mt-3 space-y-3">
          {d.payments.map((p) => (
            <div
              className="flex flex-col justify-between gap-3 rounded-xl bg-slate-50 p-4 sm:flex-row sm:items-center"
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
              <Badge value={p.status} />
              <div className="flex gap-2">
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
  const { default: JsPdf } = await import("jspdf");
  const doc = new JsPdf();
  doc.setFontSize(20);
  doc.text("Jadhavr ERP - Payment Receipt", 20, 25);
  doc.setFontSize(11);
  [
    ["Receipt", p.receiptNumber || "Pending"],
    ["Student", p.student],
    ["PRN", p.prn],
    ["Department", p.department],
    ["Amount", money(p.amount)],
    ["Payment Mode", p.paymentMode],
    ["Transaction ID", p.transactionId],
    ["Payment Date", p.paymentDate],
    ["Verified By", p.verifiedBy || "—"],
    ["Verification Time", p.verifiedAt ? new Date(p.verifiedAt).toLocaleString() : "—"],
  ].forEach(([k, v], i) => doc.text(`${k}: ${v}`, 20, 45 + i * 10));
  doc.save(`${p.receiptNumber || "payment"}.pdf`);
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
              <XAxis dataKey="label" />
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
function Widget({ title, rows, go }: { title: string; rows: api.Payment[]; go: () => void }) {
  return (
    <Panel title={title} subtitle="Latest records">
      <div className="space-y-3">
        {rows.slice(0, 6).map((p) => (
          <div key={p.id}>
            <p className="truncate text-sm font-semibold">{p.student}</p>
            <p className="text-xs text-slate-500">
              {money(p.amount)} · {p.status.replaceAll("_", " ")}
            </p>
          </div>
        ))}
        {!rows.length && <p className="text-sm text-slate-400">No records</p>}
      </div>
      <button className={`${secondary} mt-4 w-full`} onClick={go}>
        View all
      </button>
    </Panel>
  );
}
function Table({ children }: { children: React.ReactNode }) {
  return (
    <div className="responsive-table">
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
      <div className="grid grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-32 rounded-2xl bg-slate-100" />
        ))}
      </div>
    </div>
  );
}
