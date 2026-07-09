import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";

export function Modal({ open, onClose, title, description, children, size = "lg" }: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  size?: "md" | "lg" | "xl";
}) {
  useEffect(() => {
    if (!open) return;
    const handler = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [onClose, open]);
  if (!open) return null;
  const sizes = { md: "max-w-lg", lg: "max-w-2xl", xl: "max-w-4xl" };
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className={`max-h-[90vh] w-full overflow-y-auto rounded-2xl bg-white shadow-2xl ${sizes[size]}`}>
        <div className="sticky top-0 z-10 flex items-start justify-between border-b bg-white px-6 py-5">
          <div><h2 className="text-lg font-bold">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500">{description}</p>}</div>
          <button onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" aria-label="Close modal"><X className="h-5 w-5" /></button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}
