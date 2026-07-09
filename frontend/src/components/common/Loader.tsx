import { LoaderCircle } from "lucide-react";

export function Loader({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-slate-500">
      <LoaderCircle className="h-8 w-8 animate-spin text-brand-600" />
      <p className="text-sm">{label}</p>
    </div>
  );
}
