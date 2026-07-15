import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { Card } from "@/components/common/Card";

export interface DonutDatum {
  name: string;
  value: number;
  color: string;
}
export function DonutChartCard({
  title,
  subtitle,
  data,
  centerLabel = "0%",
}: {
  title: string;
  subtitle: string;
  data: DonutDatum[];
  centerLabel?: string;
}) {
  const hasData = data.some((item) => item.value > 0);
  const chartData = hasData ? data : [{ name: "No data", value: 1, color: "#e2e8f0" }];
  return (
    <Card className="p-6">
      <h2 className="text-lg font-bold text-slate-900">{title}</h2>
      <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
      <div className="relative mt-4 h-48">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              dataKey="value"
              nameKey="name"
              innerRadius={58}
              outerRadius={78}
              paddingAngle={hasData ? 3 : 0}
              stroke="none"
            >
              {chartData.map((item) => (
                <Cell key={item.name} fill={item.color} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => Array.isArray(value) ? value.join(", ") : Number(value ?? 0)} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 grid place-items-center">
          <div className="text-center">
            <p className="text-2xl font-black text-slate-900">{hasData ? centerLabel : "0"}</p>
            <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">Overview</p>
          </div>
        </div>
      </div>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {data.map((item) => (
          <div key={item.name} className="flex items-center gap-2 text-xs text-slate-600">
            <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: item.color }} />
            {item.name}
            <span className="ml-auto font-bold text-slate-800">{item.value}</span>
          </div>
        ))}
      </div>
    </Card>
  );
}
