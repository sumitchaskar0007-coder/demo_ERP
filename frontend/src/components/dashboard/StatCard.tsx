import type { ComponentType } from "react";
import { ArrowUpRight } from "lucide-react";
import { Card } from "@/components/common/Card";

export function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  accent,
}: {
  title: string;
  value: string | number;
  subtitle: string;
  icon: ComponentType<{ className?: string }>;
  accent: string;
}) {
  return (
    <Card className="group p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-400">{title}</p>
          <p className="mt-3 text-3xl font-black tracking-tight text-slate-900">{value}</p>
          <p className="mt-1.5 text-xs text-slate-500">{subtitle}</p>
        </div>
        <div
          className={`grid h-12 w-12 place-items-center rounded-2xl ${accent} transition-transform duration-200 group-hover:scale-105`}
        >
          <Icon className="h-6 w-6" />
        </div>
      </div>
      <div className="mt-4 flex items-center gap-1 text-xs font-semibold text-blue-600">
        <ArrowUpRight className="h-3.5 w-3.5" />
        Live overview
      </div>
    </Card>
  );
}
