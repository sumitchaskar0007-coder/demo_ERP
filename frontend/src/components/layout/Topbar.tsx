import {
  Bell,
  BookOpen,
  Building2,
  CalendarDays,
  ChevronDown,
  LockKeyhole,
  LogOut,
  Menu,
  UserRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/authStore";
import { academicSessionApi, type AcademicContext } from "@/features/academicSessions/api";
import { globalAcademicYearApi, type GlobalAcademicYear } from "@/features/globalAcademicYears/api";
import { API_BASE_URL } from "@/lib/apiClient";
import { ROUTES } from "@/lib/constants";
import { initials } from "@/lib/utils";
import type { LeadershipWorkspaceMode } from "./workspaceMode";

export function Topbar({
  onMenu,
  unreadNotices = 0,
  workspaceMode,
  onWorkspaceModeChange,
}: {
  onMenu: () => void;
  unreadNotices?: number;
  workspaceMode?: LeadershipWorkspaceMode;
  onWorkspaceModeChange?: (mode: LeadershipWorkspaceMode) => void;
}) {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const menuRef = useRef<HTMLDivElement>(null);
  const primaryRole = user?.roles[0]?.replaceAll("_", " ") || "User";
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;
  const hasWorkspaceSwitcher = workspaceMode !== undefined && onWorkspaceModeChange !== undefined;
  const mobileTitle = getMobilePageTitle(location.pathname);
  const [academicContext, setAcademicContext] = useState<AcademicContext | null>(null);
  const [globalAcademicYear, setGlobalAcademicYear] = useState<GlobalAcademicYear | null>(null);
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

  useEffect(() => {
    const loadYear = () => {
      if (isSuperAdmin) {
        globalAcademicYearApi.active().then(setGlobalAcademicYear).catch(() => setGlobalAcademicYear(null));
      } else {
        academicSessionApi.context().then(setAcademicContext).catch(() => setAcademicContext(null));
      }
    };
    loadYear();
    window.addEventListener("academic-year:changed", loadYear);
    return () => window.removeEventListener("academic-year:changed", loadYear);
  }, [isSuperAdmin]);

  useEffect(() => {
    if (!open) return;
    const closeMenu = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", closeMenu);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeMenu);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  return (
    <header className="fixed inset-x-0 top-0 z-40 flex h-16 min-w-0 items-center gap-2 border-b border-slate-200/80 bg-white/95 px-3 shadow-sm backdrop-blur sm:gap-3 sm:px-6 lg:sticky lg:h-20 lg:gap-4 lg:px-8 lg:shadow-none">
      <button
        onClick={onMenu}
        className="grid h-11 w-11 shrink-0 place-items-center rounded-xl text-slate-600 hover:bg-slate-100 lg:hidden"
        aria-label="Open navigation"
      >
        <Menu className="h-6 w-6" />
      </button>
      <div className="min-w-0 flex-1 lg:hidden">
        <p className="truncate text-sm font-bold leading-5 text-slate-900">{mobileTitle}</p>
        <p className="hidden truncate text-[11px] leading-4 text-slate-500 min-[390px]:block">
          Jadhavar ERP
        </p>
      </div>
      <div className="ml-auto flex shrink-0 items-center gap-1.5 sm:gap-3">
        {hasWorkspaceSwitcher && (
          <div
            className="hidden items-center rounded-xl border border-slate-200 bg-slate-100 p-1 md:flex"
            aria-label="Select workspace"
          >
            <button
              type="button"
              onClick={() => onWorkspaceModeChange?.("leadership")}
              aria-pressed={workspaceMode === "leadership"}
              className={`flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold transition ${
                workspaceMode === "leadership"
                  ? "bg-white text-brand-700 shadow-sm ring-1 ring-slate-200"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <Building2 className="h-4 w-4" />
              Leadership
            </button>
            <button
              type="button"
              onClick={() => onWorkspaceModeChange?.("teaching")}
              aria-pressed={workspaceMode === "teaching"}
              className={`flex h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold transition ${
                workspaceMode === "teaching"
                  ? "bg-white text-brand-700 shadow-sm ring-1 ring-slate-200"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <BookOpen className="h-4 w-4" />
              Teaching
            </button>
          </div>
        )}
        <div className="hidden h-11 items-center gap-2 rounded-xl border bg-white px-3 text-xs font-semibold text-slate-600 xl:flex">
          <CalendarDays className="h-4 w-4 text-brand-600" />
          {(isSuperAdmin ? globalAcademicYear?.name : academicContext?.academicYear)
            ? `Academic Year: ${isSuperAdmin ? globalAcademicYear?.name : academicContext?.academicYear}${!isSuperAdmin && academicContext?.termName ? ` · ${academicContext.termName}` : ""}`
            : "Academic year not activated"}
        </div>
        <Link
          to={ROUTES.notices}
          className="relative rounded-xl border bg-white p-2.5 text-slate-500 hover:bg-slate-50"
          aria-label={`Notifications${unreadNotices ? `, ${unreadNotices} unread` : ""}`}
        >
          <Bell className="h-5 w-5" />
          {unreadNotices > 0 && (
            <span className="absolute -right-1.5 -top-1.5 grid min-h-5 min-w-5 place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white">
              {unreadNotices > 99 ? "99+" : unreadNotices}
            </span>
          )}
        </Link>
        <div ref={menuRef} className="relative">
          <button
            onClick={() => setOpen((value) => !value)}
            className="flex min-h-11 items-center gap-3 rounded-xl border bg-white p-1 pr-1 hover:bg-slate-50 sm:p-1.5 sm:pr-3"
            aria-label="Open profile menu"
            aria-haspopup="menu"
            aria-expanded={open}
          >
            {profileImage ? (
              <img
                src={profileImage}
                alt={user?.fullName || "Profile"}
                className="h-9 w-9 rounded-lg object-cover sm:h-10 sm:w-10"
              />
            ) : (
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-brand-100 text-sm font-bold text-brand-700 sm:h-10 sm:w-10">
                {initials(user?.fullName || "User")}
              </div>
            )}
            <div className="hidden text-left sm:block">
              <p className="max-w-36 truncate text-sm font-semibold">{user?.fullName}</p>
              <p className="text-xs capitalize text-slate-400">
                {hasWorkspaceSwitcher
                  ? `${primaryRole.toLowerCase()} · ${workspaceMode} workspace`
                  : primaryRole.toLowerCase()}
              </p>
            </div>
            <ChevronDown className="hidden h-4 w-4 text-slate-400 sm:block" />
          </button>
          {open && (
            <div
              className="absolute right-0 mt-2 w-[min(13rem,calc(100vw-1.5rem))] rounded-xl border bg-white p-1.5 shadow-xl"
              role="menu"
            >
              {hasWorkspaceSwitcher && (
                <div className="mb-1 grid grid-cols-2 gap-1 border-b border-slate-100 p-1 pb-2 md:hidden">
                  <button
                    type="button"
                    onClick={() => {
                      setOpen(false);
                      onWorkspaceModeChange?.("leadership");
                    }}
                    className={`rounded-lg px-2 py-2 text-xs font-bold ${
                      workspaceMode === "leadership"
                        ? "bg-brand-50 text-brand-700"
                        : "text-slate-500 hover:bg-slate-50"
                    }`}
                  >
                    Leadership
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setOpen(false);
                      onWorkspaceModeChange?.("teaching");
                    }}
                    className={`rounded-lg px-2 py-2 text-xs font-bold ${
                      workspaceMode === "teaching"
                        ? "bg-brand-50 text-brand-700"
                        : "text-slate-500 hover:bg-slate-50"
                    }`}
                  >
                    Teaching
                  </button>
                </div>
              )}
              <Link
                to={ROUTES.profile}
                onClick={() => setOpen(false)}
                className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm hover:bg-slate-50"
                role="menuitem"
              >
                <UserRound className="h-4 w-4" />
                Profile
              </Link>
              <Link
                to={ROUTES.accountChangePassword}
                onClick={() => setOpen(false)}
                className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm hover:bg-slate-50"
                role="menuitem"
              >
                <LockKeyhole className="h-4 w-4" />
                Security
              </Link>
              <div className="my-1 border-t border-slate-100" />
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-red-600 hover:bg-red-50"
                role="menuitem"
              >
                <LogOut className="h-4 w-4" />
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function getMobilePageTitle(pathname: string) {
  const exactTitles: Record<string, string> = {
    "/dashboard": "Dashboard",
    "/profile": "My Profile",
    "/account": "Account",
    "/notices": "Notice Board",
    "/staff": "Staff",
    "/students": "Students",
    "/colleges": "Colleges",
    "/principals": "Principals",
    "/departments": "Departments",
    "/attendance": "Attendance",
    "/timetable": "Timetable",
  };
  if (exactTitles[pathname]) return exactTitles[pathname];

  const segment = pathname
    .split("/")
    .filter(Boolean)
    .filter((part) => !/^\d+$/.test(part))
    .at(-1);
  if (!segment) return "Dashboard";
  if (segment === "create") return "Create Record";
  if (segment === "edit") return "Edit Record";
  return segment.replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
