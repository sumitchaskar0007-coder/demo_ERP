import { X } from "lucide-react";
import { useEffect, useRef } from "react";
import { Sidebar } from "./Sidebar";

export function MobileSidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    closeButtonRef.current?.focus();
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose, open]);

  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-[60] lg:hidden"
      role="dialog"
      aria-modal="true"
      aria-label="Main navigation"
    >
      <button
        className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm"
        onClick={onClose}
        aria-label="Close navigation"
      />
      <div className="relative h-full w-[min(18rem,86vw)] overscroll-contain shadow-2xl">
        <button
          ref={closeButtonRef}
          type="button"
          className="absolute right-3 top-3 z-10 grid h-11 w-11 place-items-center rounded-xl text-slate-600 hover:bg-slate-100"
          onClick={onClose}
          aria-label="Close navigation"
        >
          <X className="h-5 w-5" />
        </button>
        <Sidebar collapsed={false} onToggle={() => undefined} mobile onNavigate={onClose} />
      </div>
    </div>
  );
}
