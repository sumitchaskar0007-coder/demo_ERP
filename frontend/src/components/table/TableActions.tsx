import { Ellipsis, Eye, Pencil, Power } from "lucide-react";
import { useState } from "react";

export function TableActions({ active, onView, onEdit, onToggle }: {
  active: boolean;
  onView?: () => void;
  onEdit?: () => void;
  onToggle: () => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="relative">
      <button className="rounded-lg p-2 text-slate-500 hover:bg-slate-100" onClick={() => setOpen((value) => !value)} aria-label="Open actions"><Ellipsis className="h-5 w-5" /></button>
      {open && (
        <div className="absolute right-0 z-20 mt-1 w-40 rounded-xl border bg-white p-1.5 shadow-xl">
          {onView && <button onClick={() => { setOpen(false); onView(); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-slate-50"><Eye className="h-4 w-4" />View</button>}
          {onEdit && <button onClick={() => { setOpen(false); onEdit(); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-slate-50"><Pencil className="h-4 w-4" />Edit</button>}
          <button onClick={() => { setOpen(false); onToggle(); }} className={`flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-slate-50 ${active ? "text-red-600" : "text-emerald-600"}`}><Power className="h-4 w-4" />{active ? "Deactivate" : "Activate"}</button>
        </div>
      )}
    </div>
  );
}
