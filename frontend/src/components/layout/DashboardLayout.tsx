import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Bell, Clock3, X } from "lucide-react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/authStore";
import { acknowledgeNotice, getNoticeInbox } from "@/features/notices/api";
import type { Notice } from "@/features/notices/types";
import { ROUTES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { MobileSidebar } from "./MobileSidebar";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [popup, setPopup] = useState<Notice | null>(null);
  const [acknowledgeSeconds, setAcknowledgeSeconds] = useState(8);
  const [acknowledging, setAcknowledging] = useState(false);
  const { user } = useAuth();
  const location = useLocation();
  const storageKey = `jadhavr-seen-notices-${user?.id ?? "guest"}`;
  const seenIds = useMemo(() => {
    try { return new Set<number>(JSON.parse(localStorage.getItem(storageKey) || "[]")); }
    catch { return new Set<number>(); }
  }, [storageKey, notices]);
  const unread = notices.filter((notice) => !seenIds.has(notice.id));
  const priorityNotice = notices
    .filter((notice) => notice.priority !== "NORMAL" && !notice.acknowledged)
    .sort((a, b) => Number(b.priority === "URGENT") - Number(a.priority === "URGENT"))[0] ?? null;

  useEffect(() => {
    getNoticeInbox().then(setNotices).catch(() => setNotices([]));
  }, [location.pathname]);

  useEffect(() => {
    if (!mobileOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = previousOverflow; };
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
    const isDashboard = location.pathname === ROUTES.dashboard || location.pathname.endsWith("/dashboard");
    if (priorityNotice || !isDashboard || unread.length === 0) { setPopup(null); return; }
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
      toast.success("Priority notice acknowledged");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setAcknowledging(false);
    }
  }

  function dismissPopup() {
    if (popup) {
      const updated = new Set(seenIds); updated.add(popup.id);
      localStorage.setItem(storageKey, JSON.stringify([...updated]));
      setNotices((current) => [...current]);
    }
    setPopup(null);
  }
  return (
    <div className="min-h-screen w-full overflow-x-clip bg-slate-50">
      <div className="fixed inset-y-0 left-0 z-40 hidden lg:block"><Sidebar collapsed={collapsed} onToggle={() => setCollapsed((value) => !value)} /></div>
      <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
      <div className={collapsed ? "min-w-0 transition-[padding] duration-300 lg:pl-20" : "min-w-0 transition-[padding] duration-300 lg:pl-64"}>
        <Topbar onMenu={() => setMobileOpen(true)} unreadNotices={unread.length} />
        <main className="min-w-0 pt-16 lg:pt-0">
          {priorityNotice ? (
            <div className="grid min-h-[calc(100vh-4rem)] place-items-center bg-slate-100 p-4 sm:p-8">
              <section className="w-full max-w-2xl overflow-hidden rounded-3xl border border-rose-200 bg-white shadow-2xl">
                <div className="flex items-start gap-4 bg-gradient-to-r from-rose-600 to-orange-500 p-6 text-white">
                  <div className="rounded-2xl bg-white/15 p-3"><AlertTriangle className="h-6 w-6" /></div>
                  <div><p className="text-xs font-black uppercase tracking-[0.18em]">{priorityNotice.priority} priority notice</p><h1 className="mt-2 text-2xl font-black">{priorityNotice.title}</h1><p className="mt-1 text-sm text-white/80">From {priorityNotice.createdByName}</p></div>
                </div>
                <div className="p-6 sm:p-8">
                  <p className="max-h-[45vh] overflow-y-auto whitespace-pre-wrap text-sm leading-7 text-slate-700">{priorityNotice.message}</p>
                  <div className="mt-7 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                    Read this notice completely. The dashboard unlocks after acknowledgement.
                  </div>
                  <button
                    type="button"
                    disabled={acknowledgeSeconds > 0 || acknowledging}
                    onClick={() => void acceptPriorityNotice()}
                    className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-600 font-bold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-slate-300"
                  >
                    {acknowledgeSeconds > 0 ? <><Clock3 className="h-4 w-4" />Please read — {acknowledgeSeconds}s</> : acknowledging ? "Saving…" : "I have read and accept this notice"}
                  </button>
                </div>
              </section>
            </div>
          ) : <Outlet />}
        </main>
        {popup && <div className="fixed right-4 top-24 z-50 w-[calc(100%-2rem)] max-w-md animate-[fadeIn_.2s_ease-out] overflow-hidden rounded-2xl border border-brand-200 bg-white shadow-2xl sm:right-6">
          <div className="flex items-start gap-3 bg-brand-50 px-5 py-4"><div className="rounded-xl bg-brand-600 p-2 text-white"><Bell className="h-5 w-5" /></div><div className="min-w-0 flex-1"><p className="text-xs font-bold uppercase tracking-wider text-brand-600">New notice</p><h3 className="mt-1 font-bold text-slate-900">{popup.title}</h3></div><button onClick={dismissPopup} className="rounded-lg p-1 text-slate-400 hover:bg-white hover:text-slate-700" aria-label="Dismiss notice"><X className="h-5 w-5" /></button></div>
          <div className="px-5 py-4"><p className="line-clamp-4 whitespace-pre-wrap text-sm leading-6 text-slate-600">{popup.message}</p><div className="mt-4 flex items-center justify-between"><span className="text-xs text-slate-400">From {popup.createdByName}</span><Link to={ROUTES.notices} onClick={dismissPopup} className="text-sm font-semibold text-brand-600 hover:text-brand-700">Open Notice Board</Link></div></div>
          <div className="h-1 animate-[shrink_5s_linear_forwards] bg-brand-500" />
        </div>}
      </div>
    </div>
  );
}
