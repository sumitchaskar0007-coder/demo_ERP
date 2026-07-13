import { ArrowRight, CheckCircle2, FileText, Printer, Search, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { DonutChart } from "@/components/common/DonutChart";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import * as api from "./api";
import type { AdmissionStatus } from "./types";

const cards: Array<{ label: string; status?: AdmissionStatus; icon: typeof FileText; color: string; accent: string }> = [
  { label: "Submitted Admissions", status: "SUBMITTED", icon: FileText, color: "bg-blue-50 text-blue-600", accent: "bg-blue-500" },
  { label: "Under Review", status: "STUDENT_SECTION_REVIEW_PENDING", icon: Search, color: "bg-amber-50 text-amber-600", accent: "bg-amber-500" },
  { label: "Approved", status: "STUDENT_SECTION_APPROVED", icon: CheckCircle2, color: "bg-emerald-50 text-emerald-600", accent: "bg-emerald-500" },
  { label: "Rejected", status: "STUDENT_SECTION_REJECTED", icon: XCircle, color: "bg-red-50 text-red-600", accent: "bg-red-500" },
  { label: "Printed Forms", status: "STUDENT_SECTION_APPROVED", icon: Printer, color: "bg-indigo-50 text-indigo-600", accent: "bg-indigo-500" },
];

export function StudentSectionDashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    Promise.all(cards.map((card) => api.searchStudentSectionAdmissions({ status: card.status, size: 1 }).then((page) => [card.label, page.totalElements] as const)))
      .then((pairs) => setStats(Object.fromEntries(pairs)))
      .catch((err) => toast.error(handleApiError(err).message))
      .finally(() => setLoading(false));
  }, []);
  const total = Object.values(stats).reduce((sum, value) => sum + value, 0);
  return (
    <div className="page-container pb-10">
      <div className="mb-6"><p className="text-xs font-semibold text-slate-400">Dashboard&nbsp;&nbsp;/&nbsp;&nbsp;Student Section</p><h1 className="mt-2 text-2xl font-bold">Student Section Dashboard</h1><p className="mt-1 text-sm text-slate-500">Review and manage college admission applications.</p></div>
      <section className="erp-welcome-banner p-7 sm:p-9"><div className="absolute -right-12 -top-20 h-64 w-64 rounded-full border-[32px] border-blue-300/20" /><div className="relative flex flex-col justify-between gap-5 sm:flex-row sm:items-center"><div><p className="text-sm text-blue-100">{user?.collegeName || "Student Section"}</p><h2 className="mt-2 text-3xl font-bold">Welcome back, {user?.fullName?.split(" ")[0]}</h2><p className="mt-2 text-sm text-blue-100">Your admission verification workspace is ready.</p></div><Link to={ROUTES.studentSectionAdmissions} className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-white px-4 text-sm font-semibold text-brand-700 shadow-sm">View Admissions <ArrowRight className="h-4 w-4" /></Link></div></section>
      {loading ? <Loader label="Loading dashboard..." /> : <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">{cards.map(({ label, icon: Icon, color, accent }) => <Card key={label} className="relative overflow-hidden p-5"><span className={`absolute inset-x-0 top-0 h-1 ${accent}`} /><div className="flex items-start justify-between"><div className={`grid h-11 w-11 place-items-center rounded-xl ${color}`}><Icon className="h-5 w-5" /></div><span className="rounded-full bg-slate-50 px-2 py-1 text-[10px] font-bold text-slate-400">Live</span></div><p className="mt-4 text-2xl font-bold">{stats[label] ?? 0}</p><p className="mt-1 text-xs font-medium text-slate-500">{label}</p></Card>)}</div>}
      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <Card className="overflow-hidden"><div className="border-b px-6 py-5"><h2 className="font-bold">Admission pipeline</h2><p className="mt-1 text-xs text-slate-500">Current application distribution by status</p></div><div className="space-y-5 p-6">{cards.slice(0, 4).map((card) => <div key={card.label}><div className="mb-2 flex justify-between text-sm"><span className="font-medium text-slate-600">{card.label}</span><span className="font-bold">{stats[card.label] ?? 0}</span></div><div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${card.accent}`} style={{ width: `${Math.max(5, ((stats[card.label] ?? 0) / Math.max(1, total)) * 100)}%` }} /></div></div>)}</div></Card>
        <Card className="overflow-hidden"><div className="border-b px-6 py-5"><h2 className="font-bold">Application status</h2><p className="mt-1 text-xs text-slate-500">Circular overview of the workflow</p></div><div className="p-6"><DonutChart centerLabel="Applications" segments={cards.slice(0, 4).map((card) => ({ label: card.label, value: stats[card.label] ?? 0, color: card.accent === "bg-blue-500" ? "#3b82f6" : card.accent === "bg-amber-500" ? "#f59e0b" : card.accent === "bg-emerald-500" ? "#22c55e" : "#ef4444" }))} /></div><div className="border-t px-6 py-4"><Link to={ROUTES.studentSectionAdmissions} className="inline-flex items-center gap-2 text-sm font-semibold text-brand-600">Open admission list <ArrowRight className="h-4 w-4" /></Link></div></Card>
      </div>
    </div>
  );
}
