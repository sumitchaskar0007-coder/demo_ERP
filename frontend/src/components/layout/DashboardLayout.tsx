import { useState } from "react";
import { Outlet } from "react-router-dom";
import { MobileSidebar } from "./MobileSidebar";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  return (
    <div className="min-h-screen bg-[#f4f7fb]">
      <div className="fixed inset-y-0 left-0 z-40 hidden lg:block"><Sidebar collapsed={collapsed} onToggle={() => setCollapsed((value) => !value)} /></div>
      <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
      <div className={collapsed ? "lg:pl-20" : "lg:pl-64"}>
        <Topbar onMenu={() => setMobileOpen(true)} />
        <main><Outlet /></main>
      </div>
    </div>
  );
}
