import { X } from "lucide-react";
import { Sidebar } from "./Sidebar";

export function MobileSidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <button
        className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm"
        onClick={onClose}
        aria-label="Close navigation"
      />
      <div className="relative h-full w-72 shadow-2xl">
        <button
          className="absolute right-3 top-3 z-10 rounded-lg p-2 text-slate-500 hover:bg-slate-100"
          onClick={onClose}
        >
          <X className="h-5 w-5" />
        </button>
        <Sidebar collapsed={false} onToggle={() => undefined} mobile onNavigate={onClose} />
      </div>
    </div>
  );
}
