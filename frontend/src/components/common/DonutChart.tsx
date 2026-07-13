interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

export function DonutChart({ segments, centerLabel = "Total" }: { segments: DonutSegment[]; centerLabel?: string }) {
  const total = segments.reduce((sum, segment) => sum + Math.max(0, segment.value), 0);
  let cursor = 0;
  const stops = segments.map((segment) => {
    const start = cursor;
    cursor += total ? (Math.max(0, segment.value) / total) * 100 : 0;
    return `${segment.color} ${start}% ${cursor}%`;
  });
  const background = total ? `conic-gradient(${stops.join(", ")})` : "conic-gradient(#e2e8f0 0% 100%)";

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
      <div className="relative h-48 w-48 shrink-0 rounded-full shadow-inner" style={{ background }} aria-label={`${centerLabel}: ${total}`} role="img">
        <div className="absolute inset-[26px] grid place-items-center rounded-full bg-white shadow-[inset_0_0_0_1px_rgba(226,232,240,0.8)]">
          <div className="text-center"><p className="text-3xl font-bold text-slate-900">{total}</p><p className="mt-1 text-xs font-medium text-slate-400">{centerLabel}</p></div>
        </div>
      </div>
      <div className="w-full space-y-3">
        {segments.map((segment) => (
          <div key={segment.label} className="flex items-center gap-3 text-sm">
            <span className="h-3 w-3 rounded-full" style={{ backgroundColor: segment.color }} />
            <span className="min-w-0 flex-1 truncate text-slate-500">{segment.label}</span>
            <span className="font-bold text-slate-800">{segment.value}</span>
            <span className="w-10 text-right text-xs text-slate-400">{total ? Math.round((segment.value / total) * 100) : 0}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}
