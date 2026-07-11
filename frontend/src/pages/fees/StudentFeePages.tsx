import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Textarea } from "@/components/common/Textarea";
import { StatusBadge } from "@/components/common/Badge";
import { handleApiError } from "@/lib/handleApiError";
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
          <StatusBadge status={a.status} />
          <b>{pct}% paid</b>
        </div>
        <div className="mt-4 h-3 rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-gradient-to-r from-blue-600 to-teal-500"
            style={{ width: `${pct}%` }}
          />
        </div>
        <div className="mt-6 grid gap-5 md:grid-cols-[180px_1fr]">
          {a.collegeQrCodeUrl ? (
            <img src={a.collegeQrCodeUrl} className="h-44 w-44 rounded-xl border object-contain" />
          ) : (
            <div className="grid h-44 place-items-center rounded-xl bg-slate-50 text-sm text-slate-400">
              QR not configured
            </div>
          )}
          <div>
            <h2 className="font-bold">Manual Payment Instructions</h2>
            <p className="mt-2 text-sm text-slate-500">{a.paymentInstructions}</p>
          </div>
        </div>
      </Card>
      <Card className="p-6">
        <div className="flex justify-between">
          <h2 className="font-bold">Recent Payments</h2>
          <Link className="text-sm text-blue-600" to="/student/fees/payments">
            View all
          </Link>
        </div>
        <PaymentList data={p.slice(0, 5)} />
      </Card>
    </div>
  );
}
export function SubmitPaymentPage() {
  const nav = useNavigate();
  const [a, setA] = useState<StudentFeeAccountResponse | null>(null);
  const [v, setV] = useState({
    amount: 0,
    paymentMode: "UPI" as PaymentMode,
    transactionReference: "",
    paymentDate: new Date().toISOString().slice(0, 10),
    proofUrl: "",
    remarks: "",
  });
  useEffect(() => {
    api.getMyFeeAccount().then(setA);
  }, []);
  const submit = async () => {
    if (!a || v.amount <= 0 || v.amount > a.remainingAmount)
      return toast.error("Payment amount must be within the remaining fee");
    try {
      await api.submitPayment(v);
      toast.success("Payment proof submitted for verification");
      nav("/student/fees");
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };
  return (
    <div className="page-container">
      <Card className="mx-auto max-w-2xl p-7">
        <h1 className="page-title">Submit Payment Proof</h1>
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <Input
            label="Amount"
            type="number"
            onChange={(e) => setV({ ...v, amount: Number(e.target.value) })}
          />
          <label className="text-sm font-semibold">
            Payment mode
            <select
              className="mt-2 h-10 w-full rounded-xl border px-3"
              value={v.paymentMode}
              onChange={(e) => setV({ ...v, paymentMode: e.target.value as PaymentMode })}
            >
              {["UPI", "BANK_TRANSFER", "CASH", "CHEQUE", "OTHER"].map((x) => (
                <option key={x}>{x}</option>
              ))}
            </select>
          </label>
          <Input
            label="Transaction Reference"
            onChange={(e) => setV({ ...v, transactionReference: e.target.value })}
          />
          <Input
            label="Payment Date"
            type="date"
            value={v.paymentDate}
            onChange={(e) => setV({ ...v, paymentDate: e.target.value })}
          />
          <Input
            label="Proof URL"
            className="md:col-span-2"
            onChange={(e) => setV({ ...v, proofUrl: e.target.value })}
          />
          <Textarea
            label="Remarks"
            className="md:col-span-2"
            onChange={(e) => setV({ ...v, remarks: e.target.value })}
          />
        </div>
        <Button className="mt-6" onClick={submit}>
          Submit Proof
        </Button>
      </Card>
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
