import { useEffect, useState } from "react";
import { BarChart3, Download, Printer } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import { attendanceApi, type AttendanceReport } from "./api";
import { exportAttendanceCsv, exportAttendanceExcel, exportAttendancePdf } from "./export";

const iso = (date: Date) => date.toISOString().slice(0,10);
export function AttendanceReportPage() {
  const { user } = useAuth();
  const now = new Date(); const before = new Date(); before.setDate(now.getDate()-29);
  const [from,setFrom]=useState(iso(before)); const [to,setTo]=useState(iso(now));
  const [data,setData]=useState<AttendanceReport>(); const [loading,setLoading]=useState(true);
  const path = user?.roles.includes(ROLES.PRINCIPAL) || user?.roles.includes(ROLES.SUPER_ADMIN) ? "/api/principal/attendance/report" : user?.roles.includes(ROLES.HOD) ? "/api/hod/attendance/report" : "/api/class-teacher/attendance/division";
  const load=()=>{setLoading(true);attendanceApi.report(path,{from,to}).then(setData).catch(e=>toast.error(handleApiError(e).message)).finally(()=>setLoading(false));};
  useEffect(load,[path]);
  if(loading&&!data)return <Loader label="Preparing attendance report..."/>;
  return <div className="page-container space-y-6 pb-12"><header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-xs font-bold uppercase tracking-widest text-brand-600">Analytics</p><h1 className="page-title mt-1">Attendance Reports</h1><p className="mt-1 text-sm text-slate-500">Secure role-scoped lecture attendance analytics.</p></div><div className="flex flex-wrap gap-2"><Button variant="secondary" disabled={!data} onClick={()=>data&&void exportAttendancePdf(data)}><Printer className="mr-2 h-4 w-4"/>PDF</Button><Button variant="secondary" disabled={!data} onClick={()=>data&&void exportAttendanceExcel(data)}><Download className="mr-2 h-4 w-4"/>Excel</Button><Button variant="secondary" disabled={!data} onClick={()=>data&&exportAttendanceCsv(data)}><Download className="mr-2 h-4 w-4"/>CSV</Button><Button variant="secondary" onClick={()=>window.print()}><Printer className="mr-2 h-4 w-4"/>Print</Button></div></header><Card className="p-5"><div className="flex flex-wrap items-end gap-3"><Input label="From" type="date" value={from} onChange={e=>setFrom(e.target.value)}/><Input label="To" type="date" value={to} onChange={e=>setTo(e.target.value)}/><Button onClick={load}>Apply filters</Button></div></Card>{data&&<><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5"><Metric label="Overall" value={`${data.percentage}%`}/><Metric label="Sessions" value={data.sessions}/><Metric label="Present" value={data.present}/><Metric label="Absent" value={data.absent}/><Metric label="Late / Leave" value={`${data.late} / ${data.leave}`}/></div><Card className="overflow-hidden"><div className="border-b p-5"><h2 className="flex items-center gap-2 font-bold"><BarChart3 className="h-5 w-5 text-brand-600"/>Lecture register</h2></div>{data.rows.length===0?<div className="p-12 text-center text-sm text-slate-500">No submitted attendance sessions match this range.</div>:<div className="overflow-x-auto"><table className="w-full min-w-[980px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Date & time","Subject","Department","Class","Teacher","Present","Absent","Late","Leave","%","Status"].map(h=><th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{data.rows.map(r=><tr key={r.id} className="border-t"><td className="px-4 py-3">{r.date}<br/><span className="text-xs text-slate-400">{r.time}</span></td><td className="px-4 py-3 font-semibold">{r.subject}</td><td className="px-4 py-3">{r.department}</td><td className="px-4 py-3">{r.year} - {r.division}</td><td className="px-4 py-3">{r.teacher}</td><td className="px-4 py-3 text-emerald-600">{r.present}</td><td className="px-4 py-3 text-rose-600">{r.absent}</td><td className="px-4 py-3 text-amber-600">{r.late}</td><td className="px-4 py-3 text-blue-600">{r.leave}</td><td className="px-4 py-3 font-bold">{r.percentage}</td><td className="px-4 py-3 text-xs font-bold">{r.status}</td></tr>)}</tbody></table></div>}</Card></>}</div>;
}
function Metric({label,value}:{label:string;value:string|number}){return <Card className="p-5"><p className="text-xs font-semibold uppercase text-slate-400">{label}</p><p className="mt-2 text-2xl font-black">{value}</p></Card>}
