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
    <Card className="group h-full p-4 sm:p-5">
      <div className="flex min-w-0 items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-400">{title}</p>
          <p className="mt-2 break-words text-2xl font-black tracking-tight text-slate-900 sm:mt-3 sm:text-3xl">
            {value}
          </p>
          <p className="mt-1.5 text-xs text-slate-500">{subtitle}</p>
        </div>
        <div
          className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl sm:h-12 sm:w-12 ${accent} transition-transform duration-200 group-hover:scale-105`}
        >
          <Icon className="h-5 w-5 sm:h-6 sm:w-6" />
        </div>
      </div>
      <div className="mt-4 flex items-center gap-1 text-xs font-semibold text-blue-600">
        <ArrowUpRight className="h-3.5 w-3.5" />
        Live overview
      </div>
    </Card>
  );
}
