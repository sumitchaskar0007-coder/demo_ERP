import { IndianRupee, WalletCards } from "lucide-react";
import { Card } from "@/components/common/Card";
export function FeeCollectionCard({
  collected = 0,
  pending = 0,
}: {
  collected?: number;
  pending?: number;
}) {
  const total = collected + pending;
  const percentage = total ? Math.round((collected / total) * 100) : 0;
  return (
    <Card className="p-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Fees Collection</h2>
          <p className="mt-1 text-sm text-slate-500">Verified payment overview</p>
        </div>
        <div className="rounded-2xl bg-emerald-50 p-3 text-emerald-600">
          <WalletCards className="h-6 w-6" />
        </div>
      </div>
      <div className="mt-7 flex items-end gap-1 text-slate-900">
        <IndianRupee className="mb-1 h-5 w-5" />
        <span className="text-3xl font-black">{collected.toLocaleString("en-IN")}</span>
      </div>
      <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-gradient-to-r from-blue-600 to-teal-500 transition-all duration-500"
          style={{ width: `${percentage}%` }}
        />
      </div>
      <div className="mt-3 flex justify-between text-xs">
        <span className="font-semibold text-emerald-600">{percentage}% collected</span>
        <span className="text-slate-500">Pending ₹{pending.toLocaleString("en-IN")}</span>
      </div>
      <div className="mt-6 rounded-xl bg-blue-50 p-3 text-xs text-blue-700">
        Fee analytics will populate automatically when the fee dashboard API is available.
      </div>
    </Card>
  );
}
