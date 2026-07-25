import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  FileImage,
  QrCode,
  ShieldCheck,
  Upload,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { StatusBadge } from "@/components/common/Badge";
import { handleApiError } from "@/lib/handleApiError";
import { localDateString } from "@/lib/date";
import * as api from "@/features/fees/api";
import type {
  FeeTransactionResponse,
  PaymentMode,
  PaymentResponse,
  StudentFeeAccountResponse,
} from "@/features/fees/types";
export function StudentFeesPage() {
  const [a, setA] = useState<StudentFeeAccountResponse | null>(null);
  const [p, setP] = useState<PaymentResponse[]>([]);
  const [error, setError] = useState("");
  useEffect(() => {
    Promise.all([api.getMyFeeAccount(), api.getMyPayments()])
      .then(([x, y]) => {
        setA(x);
        setP(y);
      })
      .catch((e) => setError(handleApiError(e).message));
  }, []);
  if (error)
    return (
      <div className="page-container">
        <Card className="p-8 text-center text-slate-500">{error}</Card>
      </div>
    );
  if (!a) return <div className="page-container">Loading fee account…</div>;
  const pct = a.totalFee ? Math.round((a.paidAmount / a.totalFee) * 100) : 0;
  return (
    <div className="page-container space-y-5">
      <div className="flex justify-between">
        <div>
          <h1 className="page-title">My Fees</h1>
          <p className="page-subtitle">
            {a.admissionNumber} · {a.departmentName}
          </p>
        </div>
        <Link to="/student/fees/payments/new">
          <Button>Submit Payment Proof</Button>
        </Link>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        {[
          ["Total Fee", a.totalFee],
          ["Paid", a.paidAmount],
          ["Remaining", a.remainingAmount],
          ["Minimum Required", a.minimumAmountForAdmission],
        ].map(([k, v]) => (
          <Card key={String(k)} className="p-5">
            <p className="text-xs font-bold uppercase text-slate-400">{k}</p>
            <p className="mt-2 text-2xl font-black">₹{Number(v).toLocaleString("en-IN")}</p>
          </Card>
        ))}
      </div>
      <Card className="p-6">
        <div className="flex justify-between">
          <div>
            <h2 className="font-bold">Fee History</h2>
            <p className="mt-1 text-sm text-slate-500">Your submitted and verified fee payments.</p>
          </div>
          <div className="text-right">
            <StatusBadge status={a.status} />
            <p className="mt-1 text-sm font-semibold text-slate-600">{pct}% paid</p>
          </div>
        </div>
        <div className="mt-4 h-2 rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-gradient-to-r from-blue-600 to-teal-500"
            style={{ width: `${pct}%` }}
          />
        </div>
        <PaymentList data={p} />
      </Card>
    </div>
  );
}
export function SubmitPaymentPage() {
  const nav = useNavigate();
  const [a, setA] = useState<StudentFeeAccountResponse | null>(null);
  const [proof, setProof] = useState<File | null>(null);
  const [accountConfirmed, setAccountConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [v, setV] = useState({
    amount: 0,
    paymentMode: "UPI" as PaymentMode,
    transactionReference: "",
    paymentDate: localDateString(),
    remarks: "",
  });
  useEffect(() => {
    api
      .getMyFeeAccount()
      .then(setA)
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);
  const chooseProof = (file: File | null) => {
    if (!file) return setProof(null);
    const allowed = ["application/pdf", "image/jpeg", "image/png", "image/webp"];
    if (!allowed.includes(file.type)) {
      toast.error("Select a PDF, JPEG, PNG, or WebP receipt");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error("Payment proof must not exceed 5 MB");
      return;
    }
    setProof(file);
  };
  const submit = async () => {
    if (!a || v.amount <= 0 || v.amount > a.remainingAmount) {
      return toast.error("Payment amount must be within the remaining fee");
    }
    if (v.transactionReference.trim().length < 3) {
      return toast.error("Enter a valid transaction reference");
    }
    if (!proof) return toast.error("Attach your payment receipt or screenshot");
    if (!accountConfirmed) {
      return toast.error("Confirm the QR account name before submitting");
    }
    setSubmitting(true);
    try {
      await api.submitPayment(v, proof);
      toast.success("Payment proof submitted for verification");
      nav("/student/fees");
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setSubmitting(false);
    }
  };
  if (!a)
    return <div className="page-container text-sm text-slate-500">Loading fee account...</div>;
  return (
    <div className="page-container space-y-5 pb-10">
      <div>
        <Link
          to="/student/fees"
          className="mb-3 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to My Fees
        </Link>
        <h1 className="page-title">Submit Payment Proof</h1>
        <p className="page-subtitle">
          Pay using the college QR and attach the receipt for verification.
        </p>
      </div>

      <div className="grid items-start gap-5 lg:grid-cols-[0.85fr_1.15fr]">
        <div className="space-y-5">
          <Card className="overflow-hidden">
            <div className="flex items-center gap-3 border-b bg-gradient-to-r from-blue-50 to-indigo-50 px-5 py-4">
              <div className="rounded-xl bg-blue-600 p-2.5 text-white">
                <QrCode className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-bold">Scan and pay</h2>
                <p className="text-xs text-slate-500">Official college payment QR</p>
              </div>
            </div>
            <div className="p-5 text-center">
              {a.collegeQrCodeUrl ? (
                <img
                  src={a.collegeQrCodeUrl}
                  alt="College payment QR code"
                  className="mx-auto h-56 w-56 rounded-2xl border-2 border-slate-100 bg-white object-contain p-2 shadow-sm"
                />
              ) : (
                <div className="mx-auto grid h-56 w-56 place-items-center rounded-2xl bg-slate-50 text-sm text-slate-400">
                  QR code is not configured
                </div>
              )}
              <p className="mt-4 text-sm text-slate-500">Remaining fee</p>
              <p className="text-2xl font-black text-slate-900">
                {"\u20B9"}
                {Number(a.remainingAmount).toLocaleString("en-IN")}
              </p>
            </div>
          </Card>

          <div className="rounded-2xl border border-amber-300 bg-amber-50 p-5 shadow-sm">
            <div className="flex items-start gap-3">
              <div className="rounded-xl bg-amber-500 p-2 text-white">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <div>
                <h3 className="font-bold text-amber-950">Verify before you pay</h3>
                <p className="mt-2 text-sm leading-6 text-amber-900">
                  Pay only if the account name shown after scanning the QR code is exactly
                  <strong className="mx-1">Jadhavar Senior College</strong>. Do not continue if any
                  other account name appears.
                </p>
              </div>
            </div>
          </div>
        </div>

        <Card className="p-6 sm:p-7">
          <div className="flex items-center gap-3 border-b pb-5">
            <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-700">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold">Payment details</h2>
              <p className="text-xs text-slate-500">All fields are checked by the Fee Section.</p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <Input
              label="Amount paid"
              type="number"
              min="1"
              max={a.remainingAmount}
              placeholder="Enter amount"
              onChange={(e) => setV({ ...v, amount: Number(e.target.value) })}
            />
            <label className="text-sm font-semibold text-slate-700">
              Payment mode
              <select
                className="mt-2 h-11 w-full rounded-xl border border-slate-200 bg-white px-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                value={v.paymentMode}
                onChange={(e) => setV({ ...v, paymentMode: e.target.value as PaymentMode })}
              >
                {["UPI", "BANK_TRANSFER", "CASH", "CHEQUE", "OTHER"].map((mode) => (
                  <option key={mode} value={mode}>
                    {mode.replaceAll("_", " ")}
                  </option>
                ))}
              </select>
            </label>
            <Input
              label="Transaction reference / UTR"
              placeholder="Example: 461375416852"
              value={v.transactionReference}
              onChange={(e) => setV({ ...v, transactionReference: e.target.value })}
            />
            <Input
              label="Payment date"
              type="date"
              value={v.paymentDate}
              onChange={(e) => setV({ ...v, paymentDate: e.target.value })}
            />
          </div>

          <div className="mt-5">
            <p className="text-sm font-semibold text-slate-700">Payment receipt / screenshot</p>
            {proof ? (
              <div className="mt-2 flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <div className="rounded-xl bg-white p-2.5 text-emerald-700 shadow-sm">
                  <FileImage className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold text-slate-800">{proof.name}</p>
                  <p className="text-xs text-slate-500">{(proof.size / 1024).toFixed(1)} KB</p>
                </div>
                <button
                  type="button"
                  onClick={() => setProof(null)}
                  className="rounded-lg p-2 text-slate-400 hover:bg-white hover:text-rose-600"
                  aria-label="Remove payment proof"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            ) : (
              <label className="mt-2 flex cursor-pointer flex-col items-center rounded-2xl border-2 border-dashed border-blue-200 bg-blue-50/50 px-5 py-8 text-center transition hover:border-blue-400 hover:bg-blue-50">
                <span className="rounded-2xl bg-white p-3 text-blue-600 shadow-sm">
                  <Upload className="h-6 w-6" />
                </span>
                <span className="mt-3 text-sm font-bold text-slate-800">Choose payment proof</span>
                <span className="mt-1 text-xs text-slate-500">
                  PDF, JPEG, PNG or WebP - maximum 5 MB
                </span>
                <input
                  type="file"
                  accept="application/pdf,image/jpeg,image/png,image/webp"
                  className="sr-only"
                  onChange={(event) => chooseProof(event.target.files?.[0] ?? null)}
                />
              </label>
            )}
          </div>

          <Textarea
            label="Remarks (optional)"
            className="mt-5"
            placeholder="Add any helpful payment information"
            value={v.remarks}
            onChange={(e) => setV({ ...v, remarks: e.target.value })}
          />

          <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={accountConfirmed}
              onChange={(event) => setAccountConfirmed(event.target.checked)}
              className="mt-0.5 h-4 w-4 rounded border-slate-300 text-blue-600"
            />
            <span>
              I verified that the QR account name is <strong>Jadhavar Senior College</strong> before
              paying.
            </span>
          </label>

          <Button
            className="mt-6 w-full"
            onClick={submit}
            loading={submitting}
            disabled={!proof || !accountConfirmed}
          >
            <CheckCircle2 className="h-4 w-4" />
            Submit Proof for Verification
          </Button>
        </Card>
      </div>
    </div>
  );
}
export function MyPaymentsPage() {
  const [d, setD] = useState<PaymentResponse[]>([]);
  useEffect(() => {
    api.getMyPayments().then(setD);
  }, []);
  return (
    <div className="page-container">
      <h1 className="page-title">My Payments</h1>
      <Card className="mt-5 p-6">
        <PaymentList data={d} />
      </Card>
    </div>
  );
}
export function MyFeeTransactionsPage() {
  const [d, setD] = useState<FeeTransactionResponse[]>([]);
  useEffect(() => {
    api.getMyFeeTransactions().then(setD);
  }, []);
  return (
    <div className="page-container">
      <h1 className="page-title">Fee Transactions</h1>
      <Card className="mt-5 divide-y">
        {d.length ? (
          d.map((x) => (
            <div key={x.id} className="flex justify-between p-5">
              <div>
                <b>{x.transactionType.replaceAll("_", " ")}</b>
                <p className="text-sm text-slate-500">{x.remarks || "Fee account transaction"}</p>
              </div>
              <b>₹{x.amount.toLocaleString("en-IN")}</b>
            </div>
          ))
        ) : (
          <p className="p-6 text-slate-500">No verified fee transactions yet.</p>
        )}
      </Card>
    </div>
  );
}
function PaymentList({ data }: { data: PaymentResponse[] }) {
  return (
    <div className="mt-4 divide-y">
      {data.length ? (
        data.map((x) => (
          <div key={x.id} className="flex flex-wrap items-center justify-between gap-3 py-4">
            <div>
              <b>₹{x.amount.toLocaleString("en-IN")}</b>
              <p className="text-xs text-slate-500">
                {x.transactionReference} · {x.paymentMode}
              </p>
            </div>
            <StatusBadge status={x.status} />
          </div>
        ))
      ) : (
        <p className="py-5 text-sm text-slate-500">No payment proofs submitted yet.</p>
      )}
    </div>
  );
}
