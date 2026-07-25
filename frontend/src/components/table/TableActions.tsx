import { Ellipsis, Eye, Pencil, Power } from "lucide-react";
import { useState } from "react";

export function TableActions({
  active,
  onView,
  onEdit,
  onToggle,
}: {
  active: boolean;
  onView?: () => void;
  onEdit?: () => void;
  onToggle: () => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="table-actions-menu relative ml-auto w-fit">
      <button
        className="grid h-10 w-10 place-items-center rounded-xl border border-slate-200 bg-white text-slate-500 shadow-sm transition hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Open actions"
      >
        <Ellipsis className="h-5 w-5" />
      </button>
      {open && (
        <div
          className="absolute bottom-full right-0 z-30 mb-2 w-44 rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl lg:bottom-auto lg:top-full lg:mb-0 lg:mt-2"
          role="menu"
        >
          {onView && (
            <button
              onClick={() => {
                setOpen(false);
                onView();
              }}
              className="flex min-h-10 w-full items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              role="menuitem"
            >
              <Eye className="h-4 w-4" />
              View
            </button>
          )}
          {onEdit && (
            <button
              onClick={() => {
                setOpen(false);
                onEdit();
              }}
              className="flex min-h-10 w-full items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              role="menuitem"
            >
              <Pencil className="h-4 w-4" />
              Edit
            </button>
          )}
          <button
            onClick={() => {
              setOpen(false);
              onToggle();
            }}
            className={`flex min-h-10 w-full items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium hover:bg-slate-50 ${active ? "text-red-600" : "text-emerald-600"}`}
            role="menuitem"
          >
            <Power className="h-4 w-4" />
            {active ? "Deactivate" : "Activate"}
          </button>
        </div>
      )}
    </div>
  );
}
