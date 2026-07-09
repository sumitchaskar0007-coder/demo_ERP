import { Bell, ChevronDown, LogOut, Menu, UserRound } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/authStore";
import { ROUTES } from "@/lib/constants";
import { initials } from "@/lib/utils";

export function Topbar({ onMenu }: { onMenu: () => void }) {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const primaryRole = user?.roles[0]?.replaceAll("_", " ") || "User";
  const handleLogout = () => {
    logout();
    toast.success("Logged out successfully");
    navigate(ROUTES.login);
  };
  return (
    <header className="sticky top-0 z-30 flex h-20 items-center justify-between border-b bg-white/95 px-4 backdrop-blur sm:px-6">
      <button onClick={onMenu} className="rounded-xl p-2 text-slate-500 hover:bg-slate-100 lg:hidden" aria-label="Open navigation"><Menu className="h-6 w-6" /></button>
      <div className="hidden lg:block"><p className="text-sm font-medium text-slate-400">ERP Administration Portal</p></div>
      <div className="ml-auto flex items-center gap-3">
        <button className="relative rounded-xl border p-2.5 text-slate-500 hover:bg-slate-50" aria-label="Notifications">
          <Bell className="h-5 w-5" /><span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-red-500 ring-2 ring-white" />
        </button>
        <div className="relative">
          <button onClick={() => setOpen((value) => !value)} className="flex items-center gap-3 rounded-xl border bg-white p-1.5 pr-3 hover:bg-slate-50">
            <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-100 text-sm font-bold text-brand-700">{initials(user?.fullName || "User")}</div>
            <div className="hidden text-left sm:block"><p className="max-w-36 truncate text-sm font-semibold">{user?.fullName}</p><p className="text-xs capitalize text-slate-400">{primaryRole.toLowerCase()}</p></div>
            <ChevronDown className="hidden h-4 w-4 text-slate-400 sm:block" />
          </button>
          {open && (
            <div className="absolute right-0 mt-2 w-52 rounded-xl border bg-white p-1.5 shadow-xl">
              <Link to={ROUTES.profile} onClick={() => setOpen(false)} className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm hover:bg-slate-50"><UserRound className="h-4 w-4" />Profile</Link>
              <button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-red-600 hover:bg-red-50"><LogOut className="h-4 w-4" />Logout</button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
