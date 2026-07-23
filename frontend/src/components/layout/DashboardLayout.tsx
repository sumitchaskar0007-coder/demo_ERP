import { useEffect, useMemo, useState } from "react";
import { Bell, Clock3, LogOut, X } from "lucide-react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/authStore";
import { acknowledgeNotice, getNoticeInbox } from "@/features/notices/api";
import type { Notice } from "@/features/notices/types";
import { DASHBOARD_NAVIGATION_VISIBILITY_EVENT, ROLES, ROUTES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { MobileSidebar } from "./MobileSidebar";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function DashboardLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [navigationVisible, setNavigationVisible] = useState(
    () => !user?.roles.includes(ROLES.STUDENT),
  );
  const [notices, setNotices] = useState<Notice[]>([]);
  const [popup, setPopup] = useState<Notice | null>(null);
  const [acknowledgeSeconds, setAcknowledgeSeconds] = useState(8);
  const [acknowledging, setAcknowledging] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const location = useLocation();
  const storageKey = `jadhavr-seen-notices-${user?.id ?? "guest"}`;
  const seenIds = useMemo(() => {
    try {
      return new Set<number>(JSON.parse(localStorage.getItem(storageKey) || "[]"));
    } catch {
      return new Set<number>();
    }
  }, [storageKey, notices]);
  const unread = notices.filter((notice) => !seenIds.has(notice.id));
  const priorityNotice =
    notices
      .filter((notice) => notice.priority !== "NORMAL" && !notice.acknowledged)
      .sort((a, b) => Number(b.priority === "URGENT") - Number(a.priority === "URGENT"))[0] ?? null;

  useEffect(() => {
    const updateVisibility = (event: Event) => {
      setNavigationVisible((event as CustomEvent<boolean>).detail);
      setMobileOpen(false);
    };
    window.addEventListener(DASHBOARD_NAVIGATION_VISIBILITY_EVENT, updateVisibility);
    return () =>
      window.removeEventListener(DASHBOARD_NAVIGATION_VISIBILITY_EVENT, updateVisibility);
  }, []);

  useEffect(() => {
    getNoticeInbox()
      .then(setNotices)
      .catch(() => setNotices([]));
  }, [location.pathname]);

  useEffect(() => {
    if (!mobileOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [mobileOpen]);

  useEffect(() => {
    if (!priorityNotice) return;
    setAcknowledgeSeconds(8);
    const timer = window.setInterval(
      () => setAcknowledgeSeconds((value) => (value > 0 ? value - 1 : 0)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [priorityNotice?.id]);

  useEffect(() => {
    const isDashboard =
      location.pathname === ROUTES.dashboard || location.pathname.endsWith("/dashboard");
    if (priorityNotice || !isDashboard || unread.length === 0) {
      setPopup(null);
      return;
    }
    setPopup(unread[0]);
    const timer = window.setTimeout(() => {
      const updated = new Set(seenIds);
      unread.forEach((notice) => updated.add(notice.id));
      localStorage.setItem(storageKey, JSON.stringify([...updated]));
      setPopup(null);
      setNotices((current) => [...current]);
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [location.pathname, notices.length, storageKey, priorityNotice?.id]);

  async function acceptPriorityNotice() {
    if (!priorityNotice || acknowledgeSeconds > 0) return;
    setAcknowledging(true);
    try {
      await acknowledgeNotice(priorityNotice.id);
      setNotices((current) =>
        current.map((notice) =>
          notice.id === priorityNotice.id ? { ...notice, acknowledged: true } : notice,
        ),
      );
      toast.success("Notice acknowledged");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setAcknowledging(false);
    }
  }

  function dismissPopup() {
    if (popup) {
      const updated = new Set(seenIds);
      updated.add(popup.id);
      localStorage.setItem(storageKey, JSON.stringify([...updated]));
      setNotices((current) => [...current]);
    }
    setPopup(null);
  }

  async function signOut() {
    setLoggingOut(true);
    try {
      await logout();
      toast.success("Logged out successfully");
    } catch {
      toast.error("The server session could not be closed. Please try again.");
    } finally {
      navigate(ROUTES.login, { replace: true });
      setLoggingOut(false);
    }
  }

  return (
    <div className="min-h-screen w-full overflow-x-clip bg-slate-50">
      {navigationVisible && (
        <>
          <div className="fixed inset-y-0 left-0 z-40 hidden lg:block">
            <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((value) => !value)} />
          </div>
          <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
        </>
      )}
      {!navigationVisible && (
        <button
          type="button"
          onClick={() => void signOut()}
          disabled={loggingOut}
          className="fixed right-4 top-4 z-50 flex h-11 items-center gap-2 rounded-xl bg-slate-900 px-4 text-sm font-semibold text-white shadow-lg transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60 sm:right-6 sm:top-6"
        >
          <LogOut className="h-4 w-4" />
          {loggingOut ? "Logging out..." : "Logout"}
        </button>
      )}
      <div
        className={
          !navigationVisible
            ? "min-w-0"
            : collapsed
              ? "min-w-0 transition-[padding] duration-300 lg:pl-20"
              : "min-w-0 transition-[padding] duration-300 lg:pl-64"
        }
      >
        {navigationVisible && (
          <Topbar onMenu={() => setMobileOpen(true)} unreadNotices={unread.length} />
        )}
        <main className={navigationVisible ? "min-w-0 pt-16 lg:pt-0" : "min-w-0"}>
          {priorityNotice ? (
            <div className="relative grid min-h-[calc(100vh-4rem)] place-items-center overflow-hidden bg-slate-50 p-4 sm:p-8">
              <div
                className="pointer-events-none absolute inset-0 opacity-[0.025] [background-image:radial-gradient(#2563eb_1px,transparent_1px)] [background-size:24px_24px]"
                aria-hidden="true"
              />
              <section className="relative w-full max-w-2xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-[0_24px_70px_rgba(15,23,42,0.12)]">
                <div className="h-1.5 bg-gradient-to-r from-blue-600 via-indigo-500 to-cyan-500" />
                <div className="flex items-start gap-4 border-b border-slate-100 px-6 py-6 sm:px-8">
                  <div className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-blue-50 text-blue-600 ring-1 ring-blue-100">
                    <Bell className="h-6 w-6" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-xs font-bold uppercase tracking-[0.16em] text-blue-600">
                        Important notice
                      </p>
                      <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide text-slate-500">
                        Acknowledgement required
                      </span>
                    </div>
                    <h1 className="mt-2 text-2xl font-bold text-slate-950">
                      {priorityNotice.title}
                    </h1>
                    <p className="mt-1 text-sm text-slate-500">
                      From {priorityNotice.createdByName}
                    </p>
                  </div>
                </div>
                <div className="px-6 py-6 sm:px-8 sm:py-7">
                  <div className="max-h-[42vh] overflow-y-auto rounded-2xl bg-slate-50 px-5 py-5 ring-1 ring-slate-100">
                    <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                      {priorityNotice.message}
                    </p>
                  </div>
                  <div className="mt-5 flex items-start gap-3 rounded-2xl border border-blue-100 bg-blue-50/70 p-4 text-sm leading-6 text-slate-600">
                    <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-blue-500" />
                    Please read the notice, then confirm your acknowledgement to continue to the
                    dashboard.
                  </div>
                  <button
                    type="button"
                    disabled={acknowledgeSeconds > 0 || acknowledging}
                    onClick={() => void acceptPriorityNotice()}
                    className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 font-bold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:shadow-none"
                  >
                    {acknowledgeSeconds > 0 ? (
                      <>
                        <Clock3 className="h-4 w-4" />
                        Continue available in {acknowledgeSeconds}s
                      </>
                    ) : acknowledging ? (
                      "Saving…"
                    ) : (
                      "I have read this notice"
                    )}
                  </button>
                </div>
              </section>
            </div>
          ) : (
            <Outlet />
          )}
        </main>
        {popup && (
          <div className="fixed right-4 top-24 z-50 w-[calc(100%-2rem)] max-w-md animate-[fadeIn_.2s_ease-out] overflow-hidden rounded-2xl border border-brand-200 bg-white shadow-2xl sm:right-6">
            <div className="flex items-start gap-3 bg-brand-50 px-5 py-4">
              <div className="rounded-xl bg-brand-600 p-2 text-white">
                <Bell className="h-5 w-5" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-bold uppercase tracking-wider text-brand-600">
                  New notice
                </p>
                <h3 className="mt-1 font-bold text-slate-900">{popup.title}</h3>
              </div>
              <button
                onClick={dismissPopup}
                className="rounded-lg p-1 text-slate-400 hover:bg-white hover:text-slate-700"
                aria-label="Dismiss notice"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="px-5 py-4">
              <p className="line-clamp-4 whitespace-pre-wrap text-sm leading-6 text-slate-600">
                {popup.message}
              </p>
              <div className="mt-4 flex items-center justify-between">
                <span className="text-xs text-slate-400">From {popup.createdByName}</span>
                <Link
                  to={ROUTES.notices}
                  onClick={dismissPopup}
                  className="text-sm font-semibold text-brand-600 hover:text-brand-700"
                >
                  Open Notice Board
                </Link>
              </div>
            </div>
            <div className="h-1 animate-[shrink_5s_linear_forwards] bg-brand-500" />
          </div>
        )}
      </div>
    </div>
  );
}
