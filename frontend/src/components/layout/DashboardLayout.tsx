import { useEffect, useMemo, useState } from "react";
import { Bell, X } from "lucide-react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/authStore";
import { getNoticeInbox } from "@/features/notices/api";
import type { Notice } from "@/features/notices/types";
import { ROUTES } from "@/lib/constants";
import { MobileSidebar } from "./MobileSidebar";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [popup, setPopup] = useState<Notice | null>(null);
  const { user } = useAuth();
  const location = useLocation();
  const storageKey = `jadhavr-seen-notices-${user?.id ?? "guest"}`;
  const seenIds = useMemo(() => {
    try { return new Set<number>(JSON.parse(localStorage.getItem(storageKey) || "[]")); }
    catch { return new Set<number>(); }
  }, [storageKey, notices]);
  const unread = notices.filter((notice) => !seenIds.has(notice.id));

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
    const isDashboard = location.pathname === ROUTES.dashboard || location.pathname.endsWith("/dashboard");
    if (!isDashboard || unread.length === 0) { setPopup(null); return; }
    setPopup(unread[0]);
    const timer = window.setTimeout(() => {
      const updated = new Set(seenIds);
      unread.forEach((notice) => updated.add(notice.id));
      localStorage.setItem(storageKey, JSON.stringify([...updated]));
      setPopup(null);
      setNotices((current) => [...current]);
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [location.pathname, notices.length, storageKey]);

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
        <main className="min-w-0 pt-16 lg:pt-0"><Outlet /></main>
        {popup && <div className="fixed right-4 top-24 z-50 w-[calc(100%-2rem)] max-w-md animate-[fadeIn_.2s_ease-out] overflow-hidden rounded-2xl border border-brand-200 bg-white shadow-2xl sm:right-6">
          <div className="flex items-start gap-3 bg-brand-50 px-5 py-4"><div className="rounded-xl bg-brand-600 p-2 text-white"><Bell className="h-5 w-5" /></div><div className="min-w-0 flex-1"><p className="text-xs font-bold uppercase tracking-wider text-brand-600">New notice</p><h3 className="mt-1 font-bold text-slate-900">{popup.title}</h3></div><button onClick={dismissPopup} className="rounded-lg p-1 text-slate-400 hover:bg-white hover:text-slate-700" aria-label="Dismiss notice"><X className="h-5 w-5" /></button></div>
          <div className="px-5 py-4"><p className="line-clamp-4 whitespace-pre-wrap text-sm leading-6 text-slate-600">{popup.message}</p><div className="mt-4 flex items-center justify-between"><span className="text-xs text-slate-400">From {popup.createdByName}</span><Link to={ROUTES.notices} onClick={dismissPopup} className="text-sm font-semibold text-brand-600 hover:text-brand-700">Open Notice Board</Link></div></div>
          <div className="h-1 animate-[shrink_5s_linear_forwards] bg-brand-500" />
        </div>}
      </div>
    </div>
  );
}
