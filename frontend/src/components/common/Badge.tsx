import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Badge({ children, tone = "neutral" }: {
  children: ReactNode;
  tone?: "success" | "danger" | "warning" | "info" | "neutral";
}) {
  const tones = {
    success: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
    danger: "bg-red-50 text-red-700 ring-red-600/20",
    warning: "bg-amber-50 text-amber-700 ring-amber-600/20",
    info: "bg-blue-50 text-blue-700 ring-blue-600/20",
    neutral: "bg-slate-100 text-slate-700 ring-slate-500/20",
  };
  return (
    <span className={cn("inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset", tones[tone])}>
      {children}
    </span>
  );
}

export function StatusBadge({ status }: { status: "ACTIVE" | "INACTIVE" }) {
  return <Badge tone={status === "ACTIVE" ? "success" : "danger"}>{status}</Badge>;
}
