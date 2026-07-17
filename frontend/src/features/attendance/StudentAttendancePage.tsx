import { useEffect, useState } from "react";
import { AlertTriangle, BookOpen, CalendarDays, CheckCircle2, TrendingUp } from "lucide-react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { attendanceApi, type StudentAttendance } from "./api";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";

const tone = (value: number) => value >= 75 ? "text-emerald-600" : value >= 60 ? "text-amber-600" : "text-rose-600";
const bar = (value: number) => value >= 75 ? "bg-emerald-500" : value >= 60 ? "bg-amber-500" : "bg-rose-500";

export function StudentAttendancePage() {
  const [data, setData] = useState<StudentAttendance>();
  useEffect(() => { attendanceApi.student().then(setData).catch((e) => toast.error(handleApiError(e).message)); }, []);
  if (!data) return <Loader label="Loading your attendance..." />;
  return <div className="page-container space-y-6 pb-12">
    <header><p className="text-xs font-bold uppercase tracking-widest text-brand-600">Student portal</p><h1 className="page-title mt-1">My Attendance</h1><p className="mt-1 text-sm text-slate-500">Lecture-wise attendance from submitted sessions.</p></header>
    <div className="grid gap-4 lg:grid-cols-[1.1fr_1.9fr]">
      <Card className="relative overflow-hidden border-0 bg-slate-950 p-6 text-white"><div className="absolute -right-12 -top-12 h-40 w-40 rounded-full bg-brand-500/20"/><p className="text-sm text-slate-300">Overall attendance</p><div className="mt-3 flex items-end gap-3"><span className={cn("text-5xl font-black", data.overallPercentage >= 75 ? "text-emerald-400" : data.overallPercentage >= 60 ? "text-amber-400" : "text-rose-400")}>{data.overallPercentage}%</span><TrendingUp className="mb-2 h-5 w-5" /></div><p className="mt-3 text-sm text-slate-300">{data.studentName} · Roll {data.rollNumber || "—"}</p><div className="mt-5 h-2 overflow-hidden rounded-full bg-white/10"><div className={cn("h-full rounded-full", bar(data.overallPercentage))} style={{ width: `${Math.min(100,data.overallPercentage)}%` }}/></div></Card>
      <Card className="p-6"><h2 className="font-bold">Attendance guidance</h2><div className="mt-4 grid gap-3 sm:grid-cols-3"><Guide icon={CheckCircle2} label="Good" detail="75% and above" color="text-emerald-600 bg-emerald-50"/><Guide icon={AlertTriangle} label="Warning" detail="60% to 74%" color="text-amber-600 bg-amber-50"/><Guide icon={AlertTriangle} label="Critical" detail="Below 60%" color="text-rose-600 bg-rose-50"/></div></Card>
    </div>
    <Card className="overflow-hidden"><div className="border-b p-5"><h2 className="flex items-center gap-2 font-bold"><BookOpen className="h-5 w-5 text-brand-600"/>Subject-wise attendance</h2></div>{data.subjects.length === 0 ? <Empty/> : <div className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">{data.subjects.map((s) => <div key={s.subjectId} className="rounded-2xl border p-5"><div className="flex justify-between gap-3"><div><h3 className="font-bold">{s.subject}</h3><p className="mt-1 text-xs text-slate-500">{s.attended} attended of {s.total} lectures</p></div><span className={cn("text-xl font-black", tone(s.percentage))}>{s.percentage}%</span></div><div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-100"><div className={cn("h-full rounded-full", bar(s.percentage))} style={{ width: `${Math.min(100,s.percentage)}%` }}/></div><div className="mt-4 grid grid-cols-3 text-center text-xs"><span><b className="block text-rose-600">{s.absent}</b>Absent</span><span><b className="block text-amber-600">{s.late}</b>Late</span><span><b className="block text-blue-600">{s.leave}</b>Leave</span></div></div>)}</div>}</Card>
    <Card className="overflow-hidden"><div className="border-b p-5"><h2 className="flex items-center gap-2 font-bold"><CalendarDays className="h-5 w-5 text-brand-600"/>Lecture history</h2></div>{data.history.length === 0 ? <Empty/> : <div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Date","Time","Subject","Teacher","Status","Remarks"].map(h => <th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{data.history.map((r,i) => <tr key={`${r.date}-${r.time}-${i}`} className="border-t"><td className="px-4 py-3">{r.date}</td><td className="px-4 py-3 text-slate-500">{r.time}</td><td className="px-4 py-3 font-semibold">{r.subject}</td><td className="px-4 py-3">{r.teacher}</td><td className="px-4 py-3"><span className={cn("rounded-full border px-2 py-1 text-xs font-bold", r.status === "PRESENT" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : r.status === "ABSENT" ? "border-rose-200 bg-rose-50 text-rose-700" : r.status === "LATE" ? "border-amber-200 bg-amber-50 text-amber-700" : "border-blue-200 bg-blue-50 text-blue-700")}>{r.status}</span></td><td className="px-4 py-3 text-slate-500">{r.remarks || "—"}</td></tr>)}</tbody></table></div>}</Card>
  </div>;
}

function Guide({ icon: Icon, label, detail, color }: { icon: typeof CheckCircle2; label: string; detail: string; color: string }) { return <div className="flex items-center gap-3 rounded-xl border p-3"><div className={cn("grid h-9 w-9 place-items-center rounded-lg", color)}><Icon className="h-4 w-4"/></div><div><p className="text-sm font-bold">{label}</p><p className="text-xs text-slate-500">{detail}</p></div></div>; }
function Empty(){ return <div className="p-12 text-center text-sm text-slate-500">No submitted attendance is available yet.</div>; }
