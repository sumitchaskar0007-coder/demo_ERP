import { Bell, CalendarDays, ChevronDown, LogOut, Menu, UserRound } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/authStore";
import { API_BASE_URL } from "@/lib/apiClient";
import { ROUTES } from "@/lib/constants";
import { initials } from "@/lib/utils";

export function Topbar({ onMenu, unreadNotices = 0 }: { onMenu: () => void; unreadNotices?: number }) {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const primaryRole = user?.roles[0]?.replaceAll("_", " ") || "User";
  const year = new Date().getFullYear();
  const profileImage = user?.profileImageUrl?.startsWith("/")
    ? `${API_BASE_URL}${user.profileImageUrl}`
    : user?.profileImageUrl;
  const handleLogout = async () => {
    setOpen(false);
    try {
      await logout();
      toast.success("Logged out successfully");
    } catch {
      toast.error("The server session could not be closed. Please try again.");
    } finally {
      navigate(ROUTES.login, { replace: true });
    }
  };
  return (
    <header className="fixed inset-x-0 top-0 z-40 flex h-16 items-center gap-3 border-b border-slate-200/80 bg-white/95 px-3 shadow-sm backdrop-blur sm:px-6 lg:sticky lg:h-20 lg:gap-4 lg:px-8 lg:shadow-none">
      <button onClick={onMenu} className="rounded-xl p-2 text-slate-500 hover:bg-slate-100 lg:hidden" aria-label="Open navigation"><Menu className="h-6 w-6" /></button>
      <div className="ml-auto flex items-center gap-2 sm:gap-3">
        <div className="hidden h-11 items-center gap-2 rounded-xl border bg-white px-3 text-xs font-semibold text-slate-600 xl:flex"><CalendarDays className="h-4 w-4 text-brand-600" />Academic Year: {year} / {year + 1}</div>
        <Link to={ROUTES.notices} className="relative rounded-xl border bg-white p-2.5 text-slate-500 hover:bg-slate-50" aria-label={`Notifications${unreadNotices ? `, ${unreadNotices} unread` : ""}`}>
          <Bell className="h-5 w-5" />{unreadNotices > 0 && <span className="absolute -right-1.5 -top-1.5 grid min-h-5 min-w-5 place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white">{unreadNotices > 99 ? "99+" : unreadNotices}</span>}
        </Link>
        <div className="relative">
          <button onClick={() => setOpen((value) => !value)} className="flex items-center gap-3 rounded-xl border bg-white p-1.5 pr-3 hover:bg-slate-50">
            {profileImage ? <img src={profileImage} alt={user?.fullName || "Profile"} className="h-10 w-10 rounded-lg object-cover" /> : <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-100 text-sm font-bold text-brand-700">{initials(user?.fullName || "User")}</div>}
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
