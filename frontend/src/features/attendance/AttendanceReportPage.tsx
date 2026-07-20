import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle, BarChart3, BookOpen, Building2, CalendarDays, CheckCircle2,
  ChevronRight, Clock3, Download, Eye, Filter, GraduationCap, Printer, RotateCcw,
  Search, TrendingUp, UserCheck, Users, X,
} from "lucide-react";
import {
  Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Select as ResponsiveSelect } from "@/components/common/Select";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";
import { handleApiError } from "@/lib/handleApiError";
import {
  attendanceApi, type AttendanceReport, type SessionSummary,
  type OperationalSummary, type StudentAnalyticsRow, type TrendPoint,
} from "./api";
import { exportAttendanceCsv, exportAttendanceExcel, exportAttendancePdf } from "./export";

const iso = (date: Date) => date.toISOString().slice(0, 10);
const today = iso(new Date());
const initialDates = () => {
  const end = new Date(); const start = new Date(); start.setDate(end.getDate() - 29);
  return { from: iso(start), to: iso(end) };
};
type Filters = ReturnType<typeof emptyFilters>;
const emptyFilters = () => ({ academicYear: "", departmentId: "", course: "", year: "", divisionId: "", teacher: "", subject: "", range: "", ...initialDates() });
type Drill = { departmentId?: number; department?: string; year?: string; divisionId?: number; division?: string };

export function AttendanceReportPage() {
  const { user } = useAuth();
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [applied, setApplied] = useState<Filters>(emptyFilters);
  const [data, setData] = useState<AttendanceReport>();
  const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const [drill, setDrill] = useState<Drill>({}); const [selected, setSelected] = useState<StudentAnalyticsRow>();
  const [trendMode, setTrendMode] = useState<"daily" | "weekly" | "monthly">("daily");
  const [studentSearch, setStudentSearch] = useState(""); const [gender, setGender] = useState("");
  const [studentSort, setStudentSort] = useState("lowest"); const [registerSearch, setRegisterSearch] = useState("");
  const [registerPage, setRegisterPage] = useState(1); const [registerSort, setRegisterSort] = useState("date-desc");
  const path = user?.roles.includes(ROLES.PRINCIPAL) || user?.roles.includes(ROLES.SUPER_ADMIN)
    ? "/api/principal/attendance/report" : user?.roles.includes(ROLES.HOD)
      ? "/api/hod/attendance/report" : "/api/class-teacher/attendance/division";

  const load = (next = applied) => {
    setLoading(true); setError("");
    attendanceApi.report(path, {
      from: next.from, to: next.to, departmentId: next.departmentId,
      divisionId: next.divisionId, teacherId: "", subjectId: "",
    }).then(setData).catch(e => { const message = handleApiError(e).message; setError(message); toast.error(message); })
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(applied); }, [path]);

  const departments = useMemo(() => uniqueBy(data?.students ?? [], s => s.departmentId).map(s => ({ id: s.departmentId, name: s.department })), [data]);
  const academicYears = useMemo(() => unique((data?.students ?? []).map(s => s.academicYear)), [data]);
  const years = useMemo(() => unique((data?.students ?? []).filter(s => !applied.departmentId || s.departmentId === Number(applied.departmentId)).map(s => s.year)), [data, applied.departmentId]);
  const divisions = useMemo(() => uniqueBy((data?.students ?? []).filter(s => !applied.departmentId || s.departmentId === Number(applied.departmentId)), s => s.divisionId).map(s => ({ id: s.divisionId, name: s.division })), [data, applied.departmentId]);
  const teachers = useMemo(() => unique((data?.rows ?? []).map(r => r.teacher)), [data]);
  const subjects = useMemo(() => unique((data?.rows ?? []).map(r => r.subject)), [data]);

  const students = useMemo(() => (data?.students ?? []).filter(s =>
    (!applied.academicYear || s.academicYear === applied.academicYear) &&
    (!applied.departmentId || s.departmentId === Number(applied.departmentId)) &&
    (!applied.course || s.department === applied.course) && (!applied.year || s.year === applied.year) &&
    (!applied.divisionId || s.divisionId === Number(applied.divisionId)) &&
    (!applied.teacher || s.history.some(h => h.teacher === applied.teacher)) &&
    (!applied.subject || s.subjects.some(x => x.subject === applied.subject)) &&
    (!applied.range || (applied.range === "below75" ? s.total > 0 && s.percentage < 75 : s.total > 0 && s.percentage < 50))
  ), [data, applied]);
  const rows = useMemo(() => (data?.rows ?? []).filter(r =>
    (!applied.departmentId || r.departmentId === Number(applied.departmentId)) &&
    (!applied.divisionId || r.divisionId === Number(applied.divisionId)) &&
    (!applied.year || r.year === applied.year) && (!applied.teacher || r.teacher === applied.teacher) &&
    (!applied.subject || r.subject === applied.subject)
  ), [data, applied]);
  const trend = useMemo(() => aggregateTrend(data?.trend ?? [], trendMode), [data, trendMode]);
  const totalRecords = students.reduce((n, s) => n + s.total, 0);
  const attendedRecords = students.reduce((n, s) => n + s.present + s.late, 0);
  const overall = totalRecords ? round(attendedRecords * 100 / totalRecords) : 0;
  const below75 = students.filter(s => s.total > 0 && s.percentage < 75).length;
  const below50 = students.filter(s => s.total > 0 && s.percentage < 50).length;
  const presentToday = new Set(students.filter(s => s.history.some(h => h.date === today && (h.status === "PRESENT" || h.status === "LATE"))).map(s => s.studentId)).size;
  const absentToday = new Set(students.filter(s => s.history.some(h => h.date === today && h.status === "ABSENT")).map(s => s.studentId)).size;
  const deptStats = useMemo(() => aggregate(students, s => String(s.departmentId), s => s.department), [students]);
  const divisionStats = useMemo(() => aggregate(students, s => String(s.divisionId), s => s.division), [students]);

  const apply = () => { setApplied(draft); setDrill({}); setRegisterPage(1); load(draft); };
  const reset = () => { const clean = emptyFilters(); setDraft(clean); setApplied(clean); setDrill({}); setStudentSearch(""); load(clean); };
  const exportData = data ? { ...data, students, rows, totalStudents: students.length, percentage: overall, below75, below50 } : undefined;

  return <div className="page-container space-y-6 pb-12 print:bg-white">
    <header className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
      <div><p className="text-xs font-bold uppercase tracking-[.18em] text-brand-600">Principal analytics</p><h1 className="page-title mt-1">Attendance Analytics Dashboard</h1><p className="mt-1 text-sm text-slate-500">Monitor attendance risks from college level down to individual student records.</p></div>
      <ExportActions data={exportData} />
    </header>

    <Card className="sticky top-0 z-20 border-slate-200/80 bg-white/95 p-5 shadow-sm backdrop-blur dark:bg-slate-900/95 print:hidden">
      <div className="mb-4 flex items-center gap-2 text-sm font-bold"><Filter className="h-4 w-4 text-brand-600"/>Report filters</div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <Select label="Academic Year" value={draft.academicYear} onChange={v => setDraft(x => ({ ...x, academicYear: v }))} options={academicYears} all="All academic years" />
        <Select label="Department" value={draft.departmentId} onChange={v => setDraft(x => ({ ...x, departmentId: v, course: "", year: "", divisionId: "" }))} options={departments.map(x => ({ value: String(x.id), label: x.name }))} all="All departments" />
        <Select label="Course" value={draft.course} onChange={v => setDraft(x => ({ ...x, course: v }))} options={departments.map(x => x.name)} all="All courses" />
        <Select label="Year" value={draft.year} onChange={v => setDraft(x => ({ ...x, year: v, divisionId: "" }))} options={years} all="All years" />
        <Select label="Division" value={draft.divisionId} onChange={v => setDraft(x => ({ ...x, divisionId: v }))} options={divisions.map(x => ({ value: String(x.id), label: x.name }))} all="All divisions" />
        <Select label="Teacher" value={draft.teacher} onChange={v => setDraft(x => ({ ...x, teacher: v }))} options={teachers} all="All teachers" />
        <Select label="Subject" value={draft.subject} onChange={v => setDraft(x => ({ ...x, subject: v }))} options={subjects} all="All subjects" />
        <Select label="Attendance Range" value={draft.range} onChange={v => setDraft(x => ({ ...x, range: v }))} options={[{ value: "below75", label: "Below 75%" }, { value: "below50", label: "Below 50%" }]} all="All attendance" />
        <Field label="Date From" type="date" value={draft.from} onChange={v => setDraft(x => ({ ...x, from: v }))}/>
        <Field label="Date To" type="date" value={draft.to} onChange={v => setDraft(x => ({ ...x, to: v }))}/>
      </div>
      <div className="mt-4 flex flex-wrap gap-2"><Button onClick={apply} disabled={loading}>Apply</Button><Button variant="secondary" onClick={reset}><RotateCcw className="mr-2 h-4 w-4"/>Reset</Button></div>
    </Card>

    {loading && !data ? <DashboardSkeleton/> : error && !data ? <ErrorState message={error} retry={() => load(applied)}/> : data ? <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <Metric icon={TrendingUp} label="Overall Student Attendance" value={`${overall}%`} subtitle={`${attendedRecords.toLocaleString()} of ${totalRecords.toLocaleString()} records attended`} tone="blue"/>
        <Metric icon={Users} label="Total Students" value={students.length} subtitle="Distinct active students" tone="indigo"/>
        <Metric icon={CalendarDays} label="Today's Lectures" value={data.todayLectures} subtitle="Scheduled lectures" tone="violet"/>
        <Metric icon={CheckCircle2} label="Attendance Submitted" value={data.submittedToday} subtitle="Submitted today" tone="green"/>
        <Metric icon={Clock3} label="Pending Attendance" value={data.pendingToday} subtitle="Requires submission" tone="orange"/>
        <Metric icon={Building2} label="Departments" value={new Set(students.map(s => s.departmentId)).size} subtitle="Departments in scope" tone="cyan"/>
        <Metric icon={GraduationCap} label="Divisions" value={new Set(students.map(s => s.divisionId)).size} subtitle="Active divisions" tone="blue"/>
        <Metric icon={AlertTriangle} label="Students Below 75%" value={below75} subtitle="Need intervention" tone="orange"/>
        <Metric icon={AlertTriangle} label="Students Below 50%" value={below50} subtitle="Critical attendance" tone="red"/>
        <Metric icon={UserCheck} label="Unique Students Present Today" value={presentToday} subtitle="Distinct students, not lecture totals" tone="green"/>
        <Metric icon={Users} label="Unique Students Absent Today" value={absentToday} subtitle="Distinct students, not lecture totals" tone="red"/>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <AlertCard tone="amber" value={below75} label="Students below 75%" onClick={() => { setApplied(x => ({ ...x, range: "below75" })); setDrill({}); }}/>
        <AlertCard tone="red" value={below50} label="Students below 50%" onClick={() => { setApplied(x => ({ ...x, range: "below50" })); setDrill({}); }}/>
        <AlertCard tone="orange" value={divisionStats.filter(x => x.percentage < 80).length} label="Divisions below 80%" onClick={() => setDrill({})}/>
        <AlertCard tone="rose" value={deptStats.filter(x => x.percentage < 85).length} label="Departments below 85%" onClick={() => setDrill({})}/>
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <Card className="p-5 xl:col-span-2"><SectionTitle icon={TrendingUp} title="Attendance trend" subtitle="Record-based attendance percentage over time" action={<div className="flex rounded-lg bg-slate-100 p-1">{(["daily","weekly","monthly"] as const).map(m => <button key={m} onClick={() => setTrendMode(m)} className={`rounded-md px-3 py-1.5 text-xs font-semibold capitalize ${trendMode === m ? "bg-white text-brand-700 shadow-sm" : "text-slate-500"}`}>{m}</button>)}</div>}/><div className="mt-5 h-72">{trend.length ? <ResponsiveContainer width="100%" height="100%"><LineChart data={trend}><CartesianGrid strokeDasharray="3 3" vertical={false}/><XAxis dataKey="label" tick={{fontSize:11}}/><YAxis domain={[0,100]} tick={{fontSize:11}}/><Tooltip/><Line type="monotone" dataKey="percentage" stroke="#2563eb" strokeWidth={3} dot={{r:3}} name="Attendance %"/></LineChart></ResponsiveContainer> : <Empty label="No trend data for this range"/>}</div></Card>
        <Card className="p-5"><SectionTitle icon={BarChart3} title="Attendance records" subtitle="Records, not unique student counts"/><div className="mt-5 h-72"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={[{name:"Present",value:data.present},{name:"Absent",value:data.absent},{name:"Late",value:data.late},{name:"Leave",value:data.leave}]} dataKey="value" innerRadius={62} outerRadius={92} paddingAngle={3}>{["#10b981","#ef4444","#f59e0b","#3b82f6"].map(c => <Cell key={c} fill={c}/>)}</Pie><Tooltip/></PieChart></ResponsiveContainer></div><div className="grid grid-cols-2 gap-2 text-xs"><Legend c="bg-emerald-500" label="Present records" value={data.present}/><Legend c="bg-rose-500" label="Absent records" value={data.absent}/><Legend c="bg-amber-500" label="Late records" value={data.late}/><Legend c="bg-blue-500" label="Leave records" value={data.leave}/></div></Card>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card className="overflow-hidden"><Breadcrumb drill={drill} setDrill={setDrill}/><AnalyticsTable students={students} operations={data.departmentOperations} drill={drill} setDrill={setDrill} search={studentSearch} setSearch={setStudentSearch} gender={gender} setGender={setGender} sort={studentSort} setSort={setStudentSort} select={setSelected}/></Card>
        <AttentionPanel students={students} onSelect={setSelected}/>
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <Card className="p-5"><SectionTitle icon={BarChart3} title="Department comparison" subtitle="Average attendance across active students"/><div className="mt-5 h-72">{deptStats.length ? <ResponsiveContainer width="100%" height="100%"><BarChart data={deptStats}><CartesianGrid strokeDasharray="3 3" vertical={false}/><XAxis dataKey="label" tick={{fontSize:11}}/><YAxis domain={[0,100]}/><Tooltip/><Bar dataKey="percentage" fill="#2563eb" radius={[7,7,0,0]} name="Attendance %"/></BarChart></ResponsiveContainer> : <Empty label="No department data"/>}</div></Card>
        <Card className="p-5"><SectionTitle icon={GraduationCap} title="Division attendance" subtitle="Horizontal comparison; lowest divisions surface first"/><div className="mt-5 space-y-4">{[...divisionStats].sort((a,b) => a.percentage-b.percentage).slice(0,8).map(x => <Progress key={x.key} label={x.label} value={x.percentage}/>)}</div></Card>
      </div>

      <Card className="overflow-hidden"><SectionTitleWrap><SectionTitle icon={Building2} title="Attendance heat map" subtitle="Department and year comparison; click a cell to drill down"/></SectionTitleWrap><HeatMap students={students} setDrill={setDrill}/></Card>
      <div className="grid gap-6 xl:grid-cols-2"><TeacherAnalytics rows={rows} operations={data.teacherOperations}/><SubjectAnalytics students={students} rows={rows}/></div>
      <LectureRegister rows={rows} search={registerSearch} setSearch={setRegisterSearch} page={registerPage} setPage={setRegisterPage} sort={registerSort} setSort={setRegisterSort}/>
    </> : null}
    {selected && <StudentDrawer student={selected} close={() => setSelected(undefined)}/>}
  </div>;
}

function ExportActions({data}:{data?:AttendanceReport}) { return <div className="flex flex-wrap gap-2 print:hidden"><Button variant="secondary" disabled={!data} onClick={() => data && void exportAttendancePdf(data)}><Printer className="mr-2 h-4 w-4"/>PDF</Button><Button variant="secondary" disabled={!data} onClick={() => data && void exportAttendanceExcel(data)}><Download className="mr-2 h-4 w-4"/>Excel</Button><Button variant="secondary" disabled={!data} onClick={() => data && exportAttendanceCsv(data)}><Download className="mr-2 h-4 w-4"/>CSV</Button><Button variant="secondary" onClick={() => window.print()}><Printer className="mr-2 h-4 w-4"/>Print</Button></div> }
function Select({label,value,onChange,options,all}:{label:string;value:string;onChange:(v:string)=>void;options:(string|{value:string;label:string})[];all:string}) { return <ResponsiveSelect label={label} value={value} onChange={e=>onChange(e.target.value)} options={[{value:"",label:all},...options.map(o=>typeof o==="string"?{value:o,label:o}:o)]}/> }
function Field({label,type,value,onChange}:{label:string;type:string;value:string;onChange:(v:string)=>void}) { return <label className="text-xs font-semibold text-slate-600">{label}<input type={type} value={value} onChange={e=>onChange(e.target.value)} className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"/></label> }
const tones:Record<string,string>={blue:"bg-blue-50 text-blue-600",indigo:"bg-indigo-50 text-indigo-600",violet:"bg-violet-50 text-violet-600",green:"bg-emerald-50 text-emerald-600",orange:"bg-amber-50 text-amber-600",cyan:"bg-cyan-50 text-cyan-600",red:"bg-rose-50 text-rose-600"};
function Metric({icon:Icon,label,value,subtitle,tone}:{icon:typeof Users;label:string;value:string|number;subtitle:string;tone:string}) { return <Card className="group p-5 transition duration-200 hover:-translate-y-1 hover:shadow-md"><div className={`inline-flex rounded-xl p-2.5 ${tones[tone]}`}><Icon className="h-5 w-5"/></div><p className="mt-4 text-2xl font-black tracking-tight text-slate-900 dark:text-white">{value}</p><p className="mt-1 text-xs font-bold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 flex items-center gap-1 text-[11px] text-slate-400"><TrendingUp className="h-3 w-3"/>{subtitle}</p></Card> }
function AlertCard({tone,value,label,onClick}:{tone:string;value:number;label:string;onClick:()=>void}) { return <button onClick={onClick} className={`flex items-center justify-between rounded-xl border p-4 text-left transition hover:-translate-y-0.5 hover:shadow-sm ${tone==="red"||tone==="rose"?"border-rose-200 bg-rose-50 text-rose-800":"border-amber-200 bg-amber-50 text-amber-800"}`}><span><b className="text-xl">{value}</b><span className="ml-2 text-sm font-semibold">{label}</span></span><span className="text-xs font-bold">View <ChevronRight className="inline h-3 w-3"/></span></button> }
function SectionTitle({icon:Icon,title,subtitle,action}:{icon:typeof Users;title:string;subtitle:string;action?:React.ReactNode}) { return <div className="flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-3"><span className="rounded-lg bg-brand-50 p-2 text-brand-600"><Icon className="h-4 w-4"/></span><div><h2 className="font-bold text-slate-900 dark:text-white">{title}</h2><p className="text-xs text-slate-500">{subtitle}</p></div></div>{action}</div> }
function SectionTitleWrap({children}:{children:React.ReactNode}) { return <div className="border-b border-slate-100 p-5">{children}</div> }
function Legend({c,label,value}:{c:string;label:string;value:number}) { return <div className="flex items-center justify-between rounded-lg bg-slate-50 p-2"><span className="flex items-center gap-2"><i className={`h-2 w-2 rounded-full ${c}`}/>{label}</span><b>{value}</b></div> }
function Progress({label,value}:{label:string;value:number}) { const c=value>=95?"bg-emerald-500":value>=85?"bg-blue-500":value>=75?"bg-amber-500":"bg-rose-500"; return <div><div className="mb-1.5 flex justify-between text-sm"><span className="font-semibold">{label}</span><b>{value}%</b></div><div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${c}`} style={{width:`${Math.min(100,value)}%`}}/></div></div> }
function Breadcrumb({drill,setDrill}:{drill:Drill;setDrill:(d:Drill)=>void}) { return <div className="flex flex-wrap items-center gap-1 border-b bg-slate-50/70 px-5 py-4 text-sm"><button className="font-bold text-brand-700" onClick={()=>setDrill({})}>Attendance Dashboard</button>{drill.department&&<><ChevronRight className="h-4 w-4 text-slate-400"/><button onClick={()=>setDrill({departmentId:drill.departmentId,department:drill.department})} className="font-semibold">{drill.department}</button></>}{drill.year&&<><ChevronRight className="h-4 w-4 text-slate-400"/><button onClick={()=>setDrill({departmentId:drill.departmentId,department:drill.department,year:drill.year})} className="font-semibold">{drill.year}</button></>}{drill.division&&<><ChevronRight className="h-4 w-4 text-slate-400"/><span className="font-semibold">{drill.division}</span><ChevronRight className="h-4 w-4 text-slate-400"/><span className="text-slate-500">Students</span></>}</div> }

function AnalyticsTable(p:{students:StudentAnalyticsRow[];operations:OperationalSummary[];drill:Drill;setDrill:(d:Drill)=>void;search:string;setSearch:(v:string)=>void;gender:string;setGender:(v:string)=>void;sort:string;setSort:(v:string)=>void;select:(s:StudentAnalyticsRow)=>void}) {
  const scoped=p.students.filter(s=>(!p.drill.departmentId||s.departmentId===p.drill.departmentId)&&(!p.drill.year||s.year===p.drill.year)&&(!p.drill.divisionId||s.divisionId===p.drill.divisionId));
  if(!p.drill.departmentId) { const rows=aggregate(scoped,s=>String(s.departmentId),s=>s.department).map(r=>({...r,pending:p.operations.find(o=>o.id===Number(r.key))?.pending??0})); return <GroupTable title="Department analytics" rows={rows} columns="department" click={r=>p.setDrill({departmentId:Number(r.key),department:r.label})}/>; }
  if(!p.drill.year) return <GroupTable title="Year analytics" rows={aggregate(scoped,s=>s.year,s=>s.year)} columns="year" click={r=>p.setDrill({...p.drill,year:r.label})}/>;
  if(!p.drill.divisionId) return <GroupTable title="Division analytics" rows={aggregate(scoped,s=>String(s.divisionId),s=>s.division)} columns="division" click={r=>p.setDrill({...p.drill,divisionId:Number(r.key),division:r.label})}/>;
  let list=scoped.filter(s=>!p.search||`${s.rollNumber} ${s.studentName}`.toLowerCase().includes(p.search.toLowerCase())).filter(s=>!p.gender||s.gender===p.gender);
  list=[...list].sort((a,b)=>p.sort==="highest"?b.percentage-a.percentage:p.sort==="roll"?(a.rollNumber??"").localeCompare(b.rollNumber??""):p.sort==="name"?a.studentName.localeCompare(b.studentName):a.percentage-b.percentage);
  return <div><div className="flex flex-wrap items-end gap-3 border-b p-5"><div className="min-w-[220px] flex-1"><label className="text-xs font-semibold text-slate-500">Search students</label><div className="relative mt-1"><Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400"/><input value={p.search} onChange={e=>p.setSearch(e.target.value)} placeholder="Roll number or student name" className="h-10 w-full rounded-lg border pl-9 pr-3 text-sm"/></div></div><Select label="Gender" value={p.gender} onChange={p.setGender} options={unique(scoped.map(s=>s.gender||"Not specified"))} all="All genders"/><Select label="Sort" value={p.sort} onChange={p.setSort} options={[{value:"lowest",label:"Lowest attendance"},{value:"highest",label:"Highest attendance"},{value:"roll",label:"Roll number"},{value:"name",label:"Alphabetically"}]} all="Sort students"/></div><div className="overflow-x-auto"><table className="w-full min-w-[1050px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Student","Roll no.","Gender","Attendance","Present","Absent","Late","Leave","Status","Action"].map(h=><th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{list.map(s=><tr key={s.studentId} className="border-t hover:bg-slate-50"><td className="px-4 py-3"><div className="flex items-center gap-3"><Avatar name={s.studentName}/><b>{s.studentName}</b></div></td><td className="px-4 py-3">{s.rollNumber||"—"}</td><td className="px-4 py-3">{s.gender||"—"}</td><td className="px-4 py-3"><Percent value={s.percentage}/></td><td className="px-4 py-3 text-emerald-600">{s.present}</td><td className="px-4 py-3 text-rose-600">{s.absent}</td><td className="px-4 py-3 text-amber-600">{s.late}</td><td className="px-4 py-3 text-blue-600">{s.leave}</td><td className="px-4 py-3"><Status value={s.indicator}/></td><td className="px-4 py-3"><button onClick={()=>p.select(s)} className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700"><Eye className="mr-1 inline h-3.5 w-3.5"/>View</button></td></tr>)}</tbody></table>{!list.length&&<Empty label="No students match these filters"/>}</div></div>;
}
type Agg={key:string;label:string;students:number;percentage:number;present:number;absent:number;late:number;leave:number;classTeacher:string;presentToday:number;absentToday:number;pending?:number};
function GroupTable({title,rows,columns,click}:{title:string;rows:Agg[];columns:"department"|"year"|"division";click:(r:Agg)=>void}) {
  const department=columns==="department";
  return <div><div className="px-5 py-4"><h2 className="font-bold">{title}</h2><p className="text-xs text-slate-500">Click a row to continue the drill-down.</p></div><div className="overflow-x-auto"><table className="w-full min-w-[850px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-5 py-3">{columns}</th>{columns==="division"&&<th className="px-4 py-3">Class Teacher</th>}<th className="px-4 py-3">Students</th><th className="px-4 py-3">Attendance</th><th className="px-4 py-3">{department?"Present Today":"Present Records"}</th><th className="px-4 py-3">{department?"Absent Today":"Absent Records"}</th>{department?<th className="px-4 py-3">Pending Lectures</th>:<><th className="px-4 py-3">Late</th><th className="px-4 py-3">Leave</th></>}<th className="px-4 py-3">Status</th><th className="px-4 py-3">Action</th></tr></thead><tbody>{rows.map(r=><tr key={r.key} onClick={()=>click(r)} className="cursor-pointer border-t transition hover:bg-brand-50/40"><td className="px-5 py-4 font-bold">{r.label}</td>{columns==="division"&&<td className="px-4 py-4">{r.classTeacher}</td>}<td className="px-4 py-4">{r.students}</td><td className="px-4 py-4"><Percent value={r.percentage}/></td><td className="px-4 py-4 text-emerald-600">{department?r.presentToday:r.present}</td><td className="px-4 py-4 text-rose-600">{department?r.absentToday:r.absent}</td>{department?<td className={`px-4 py-4 ${(r.pending??0)>0?"font-bold text-rose-600":"text-slate-500"}`}>{r.pending??0}</td>:<><td className="px-4 py-4">{r.late}</td><td className="px-4 py-4">{r.leave}</td></>}<td className="px-4 py-4"><Status value={indicator(r.percentage)}/></td><td className="px-4 py-4 text-brand-700">Drill down <ChevronRight className="inline h-4 w-4"/></td></tr>)}</tbody></table>{!rows.length&&<Empty label="No analytics available"/>}</div></div>
}
function AttentionPanel({students,onSelect}:{students:StudentAnalyticsRow[];onSelect:(s:StudentAnalyticsRow)=>void}) { const low=[...students].filter(s=>s.total>0&&s.percentage<75).sort((a,b)=>a.percentage-b.percentage).slice(0,10); return <Card className="self-start p-5 xl:sticky xl:top-48"><SectionTitle icon={AlertTriangle} title="Students Requiring Attention" subtitle={`${students.filter(s=>s.total>0&&s.percentage<75).length} below 75% · ${students.filter(s=>s.total>0&&s.percentage<60).length} below 60% · ${students.filter(s=>s.total>0&&s.percentage<50).length} below 50%`}/><div className="mt-4 space-y-2">{low.map(s=><button key={s.studentId} onClick={()=>onSelect(s)} className="flex w-full items-center gap-3 rounded-xl border border-slate-100 p-3 text-left transition hover:border-rose-200 hover:bg-rose-50"><Avatar name={s.studentName}/><span className="min-w-0 flex-1"><b className="block truncate text-sm">{s.studentName}</b><small className="text-slate-500">{s.department} · {s.rollNumber||"No roll no."}</small></span><b className="text-sm text-rose-600">{s.percentage}%</b></button>)}{!low.length&&<Empty label="No students below 75%"/>}</div></Card> }

function HeatMap({students,setDrill}:{students:StudentAnalyticsRow[];setDrill:(d:Drill)=>void}) { const depts=uniqueBy(students,s=>s.departmentId); const years=unique(students.map(s=>s.year)); return <div className="overflow-x-auto p-5"><table className="w-full min-w-[620px] text-sm"><thead><tr><th className="p-2 text-left">Department</th>{years.map(y=><th key={y} className="p-2 text-center">{y}</th>)}</tr></thead><tbody>{depts.map(d=><tr key={d.departmentId}><td className="p-2 font-semibold">{d.department}</td>{years.map(y=>{const group=students.filter(s=>s.departmentId===d.departmentId&&s.year===y); const pct=studentPct(group); return <td key={y} className="p-2"><button disabled={!group.length} onClick={()=>setDrill({departmentId:d.departmentId,department:d.department,year:y})} className={`w-full rounded-lg p-3 font-bold ${!group.length?"bg-slate-50 text-slate-300":pct>=85?"bg-emerald-100 text-emerald-800":pct>=75?"bg-amber-100 text-amber-800":"bg-rose-100 text-rose-800"}`}>{group.length?`${pct}%`:"—"}</button></td>})}</tr>)}</tbody></table></div> }
function TeacherAnalytics({rows,operations}:{rows:SessionSummary[];operations:OperationalSummary[]}) { const names=unique([...rows.map(r=>r.teacher),...operations.map(o=>o.name)]); const grouped=names.map(teacher=>{const lectures=rows.filter(r=>r.teacher===teacher);const op=operations.find(o=>o.name===teacher);return{teacher,lectures:op?.lectures??lectures.length,submitted:op?.submitted??lectures.length,pending:op?.pending??0,percentage:lectures.length?round(lectures.reduce((n,x)=>n+x.percentage,0)/lectures.length):0}}); return <Card className="overflow-hidden"><SectionTitleWrap><SectionTitle icon={UserCheck} title="Teacher analytics" subtitle="Today's scheduled submission and report-range attendance"/></SectionTitleWrap><SimpleTable heads={["Teacher","Lectures Today","Submitted","Pending","Average"]} rows={grouped.map(x=>[x.teacher,x.lectures,x.submitted,<span className={x.pending?"font-bold text-rose-600":"text-slate-500"}>{x.pending}</span>,<Percent value={x.percentage}/>])}/></Card> }
function SubjectAnalytics({students,rows}:{students:StudentAnalyticsRow[];rows:SessionSummary[]}) { const names=unique(students.flatMap(s=>s.subjects.map(x=>x.subject))); const items=names.map(name=>{const values=students.map(s=>s.subjects.find(x=>x.subject===name)?.percentage).filter((x):x is number=>x!==undefined); const teachers=unique(rows.filter(r=>r.subject===name).map(r=>r.teacher)).join(", ")||"—"; const avg=values.length?round(values.reduce((a,b)=>a+b,0)/values.length):0; return [name,<Percent value={avg}/>,values.length?Math.max(...values):0,values.length?Math.min(...values):0,teachers,<Status value={indicator(avg)}/>];}); return <Card className="overflow-hidden"><SectionTitleWrap><SectionTitle icon={BookOpen} title="Subject analytics" subtitle="Average, highest and lowest student attendance"/></SectionTitleWrap><SimpleTable heads={["Subject","Average","Highest","Lowest","Teacher","Status"]} rows={items}/></Card> }
function SimpleTable({heads,rows}:{heads:string[];rows:React.ReactNode[][]}) { return <div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{heads.map(h=><th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{rows.map((row,i)=><tr key={i} className="border-t">{row.map((c,j)=><td key={j} className="px-4 py-3">{c}</td>)}</tr>)}</tbody></table>{!rows.length&&<Empty label="No records available"/>}</div> }

function LectureRegister({rows,search,setSearch,page,setPage,sort,setSort}:{rows:SessionSummary[];search:string;setSearch:(v:string)=>void;page:number;setPage:(v:number)=>void;sort:string;setSort:(v:string)=>void}) { const size=10; let list=rows.filter(r=>!search||`${r.subject} ${r.department} ${r.teacher} ${r.division}`.toLowerCase().includes(search.toLowerCase())); list=[...list].sort((a,b)=>sort==="percentage"?a.percentage-b.percentage:sort==="date-asc"?a.date.localeCompare(b.date):b.date.localeCompare(a.date)); const pages=Math.max(1,Math.ceil(list.length/size)); const shown=list.slice((page-1)*size,page*size); return <Card className="overflow-hidden"><div className="flex flex-wrap items-end justify-between gap-3 border-b p-5"><SectionTitle icon={BarChart3} title="Lecture register" subtitle="Submitted lecture-level attendance records"/><div className="flex gap-2"><div className="relative"><Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400"/><input value={search} onChange={e=>{setSearch(e.target.value);setPage(1)}} placeholder="Search register" className="h-10 rounded-lg border pl-9 pr-3 text-sm"/></div><select value={sort} onChange={e=>setSort(e.target.value)} className="rounded-lg border px-3 text-sm"><option value="date-desc">Newest first</option><option value="date-asc">Oldest first</option><option value="percentage">Lowest attendance</option></select></div></div><div className="max-h-[560px] overflow-auto"><table className="w-full min-w-[1050px] text-left text-sm"><thead className="sticky top-0 z-10 bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Date & time","Subject","Department","Class","Teacher","Present","Absent","Late","Leave","%","Status"].map(h=><th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{shown.map(r=><tr key={r.id} className="border-t hover:bg-slate-50"><td className="px-4 py-3">{r.date}<br/><small className="text-slate-400">{r.time}</small></td><td className="px-4 py-3 font-semibold">{r.subject}</td><td className="px-4 py-3">{r.department}</td><td className="px-4 py-3">{r.year} · {r.division}</td><td className="px-4 py-3">{r.teacher}</td><td className="px-4 py-3 text-emerald-600">{r.present}</td><td className="px-4 py-3 text-rose-600">{r.absent}</td><td className="px-4 py-3 text-amber-600">{r.late}</td><td className="px-4 py-3 text-blue-600">{r.leave}</td><td className="px-4 py-3"><Percent value={r.percentage}/></td><td className="px-4 py-3"><Status value={r.status}/></td></tr>)}</tbody></table>{!shown.length&&<Empty label="No lecture sessions match the search"/>}</div><div className="flex items-center justify-between border-t px-5 py-4 text-sm"><span>{list.length} sessions · Page {Math.min(page,pages)} of {pages}</span><div className="flex gap-2"><Button variant="secondary" disabled={page<=1} onClick={()=>setPage(page-1)}>Previous</Button><Button variant="secondary" disabled={page>=pages} onClick={()=>setPage(page+1)}>Next</Button></div></div></Card> }

function StudentDrawer({student,close}:{student:StudentAnalyticsRow;close:()=>void}) { return <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/35" onMouseDown={close}><aside onMouseDown={e=>e.stopPropagation()} className="h-full w-full max-w-2xl overflow-y-auto bg-white shadow-2xl dark:bg-slate-900"><div className="sticky top-0 z-10 flex items-center justify-between border-b bg-white/95 p-5 backdrop-blur dark:bg-slate-900/95"><div className="flex items-center gap-3"><Avatar name={student.studentName} large/><div><h2 className="font-bold text-slate-900 dark:text-white">{student.studentName}</h2><p className="text-xs text-slate-500">PRN {student.admissionNumber} · Roll {student.rollNumber||"—"}</p></div></div><button onClick={close} className="rounded-lg p-2 hover:bg-slate-100"><X className="h-5 w-5"/></button></div><div className="space-y-6 p-5"><div className="grid gap-3 sm:grid-cols-2"><Info label="Department / Course" value={student.department}/><Info label="Academic Year" value={student.academicYear}/><Info label="Year / Division" value={`${student.year} · ${student.division}`}/><Info label="Class Teacher" value={student.classTeacher}/><Info label="Guardian" value={student.guardianName||"—"}/><Info label="Mobile" value={student.mobile||"—"}/></div><Card className="p-5"><p className="text-xs font-bold uppercase text-slate-500">Overall attendance</p><div className="mt-2 flex items-end justify-between"><b className="text-4xl text-brand-700">{student.percentage}%</b><Status value={student.indicator}/></div><div className="mt-4 grid grid-cols-4 gap-2 text-center text-xs"><Stat label="Present" value={student.present} c="text-emerald-600"/><Stat label="Absent" value={student.absent} c="text-rose-600"/><Stat label="Late" value={student.late} c="text-amber-600"/><Stat label="Leave" value={student.leave} c="text-blue-600"/></div></Card><div><h3 className="mb-3 font-bold">Subject-wise attendance</h3><div className="space-y-3">{student.subjects.map(s=><Progress key={s.subjectId} label={s.subject} value={s.percentage}/>)}</div></div><Card className="p-5"><h3 className="font-bold">Monthly attendance</h3><div className="mt-4 h-52"><ResponsiveContainer width="100%" height="100%"><BarChart data={student.monthly}><CartesianGrid strokeDasharray="3 3" vertical={false}/><XAxis dataKey="month" tick={{fontSize:10}}/><YAxis domain={[0,100]}/><Tooltip/><Bar dataKey="percentage" fill="#2563eb" radius={[6,6,0,0]}/></BarChart></ResponsiveContainer></div></Card><div><h3 className="mb-3 font-bold">Attendance history</h3><SimpleTable heads={["Date","Subject","Teacher","Status"]} rows={student.history.map(h=>[h.date,h.subject,h.teacher,<Status value={h.status}/>])}/></div></div></aside></div> }

function Info({label,value}:{label:string;value:string}) { return <div className="rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">{label}</p><p className="mt-1 text-sm font-semibold">{value}</p></div> }
function Stat({label,value,c}:{label:string;value:number;c:string}) { return <div className="rounded-lg bg-slate-50 p-2"><b className={c}>{value}</b><span className="block text-slate-500">{label}</span></div> }
function Avatar({name,large=false}:{name:string;large?:boolean}) { return <span className={`grid shrink-0 place-items-center rounded-full bg-brand-100 font-bold text-brand-700 ${large?"h-12 w-12 text-lg":"h-9 w-9 text-sm"}`}>{name.split(/\s+/).slice(0,2).map(x=>x[0]).join("").toUpperCase()}</span> }
function Percent({value}:{value:number}) { const c=value>=95?"text-emerald-600":value>=85?"text-blue-600":value>=75?"text-amber-600":"text-rose-600"; return <b className={c}>{round(value)}%</b> }
function Status({value}:{value:string}) { const v=value.replaceAll("_"," "); const c=v==="EXCELLENT"||v==="SUBMITTED"?"bg-emerald-100 text-emerald-700":v==="GOOD"?"bg-blue-100 text-blue-700":v==="AVERAGE"?"bg-amber-100 text-amber-700":v==="WARNING"?"bg-orange-100 text-orange-700":v==="NO DATA"?"bg-slate-100 text-slate-600":"bg-rose-100 text-rose-700"; return <span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-[10px] font-bold ${c}`}>{v}</span> }
function Empty({label}:{label:string}) { return <div className="grid min-h-28 place-items-center p-8 text-center text-sm text-slate-400"><div><BarChart3 className="mx-auto mb-2 h-7 w-7 opacity-40"/>{label}</div></div> }
function ErrorState({message,retry}:{message:string;retry:()=>void}) { return <Card className="grid min-h-72 place-items-center p-8 text-center"><div><AlertTriangle className="mx-auto h-10 w-10 text-rose-500"/><h2 className="mt-3 font-bold">Unable to load attendance analytics</h2><p className="mt-1 text-sm text-slate-500">{message}</p><div className="mt-4"><Button onClick={retry}>Try again</Button></div></div></Card> }
function DashboardSkeleton() { return <div className="space-y-6 animate-pulse"><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">{Array.from({length:9}).map((_,i)=><div key={i} className="h-36 rounded-xl bg-slate-100"/>)}</div><div className="h-80 rounded-xl bg-slate-100"/></div> }

function unique<T>(items:T[]) { return [...new Set(items)].filter(Boolean).sort((a,b)=>String(a).localeCompare(String(b))); }
function uniqueBy<T>(items:T[], key:(item:T)=>string|number) { return [...new Map(items.map(x=>[key(x),x])).values()]; }
function groupObjects<T>(items:T[], key:(item:T)=>string) { return items.reduce<Record<string,T[]>>((a,x)=>{(a[key(x)]??=[]).push(x);return a;},{}); }
function round(n:number) { return Math.round((Number.isFinite(n)?n:0)*100)/100; }
function indicator(p:number) { return p>=95?"EXCELLENT":p>=85?"GOOD":p>=75?"AVERAGE":p>=50?"WARNING":"CRITICAL"; }
function studentPct(list:StudentAnalyticsRow[]) { const total=list.reduce((n,s)=>n+s.total,0); return total?round(list.reduce((n,s)=>n+s.present+s.late,0)*100/total):0; }
function aggregate(items:StudentAnalyticsRow[], key:(s:StudentAnalyticsRow)=>string, label:(s:StudentAnalyticsRow)=>string):Agg[] { return Object.entries(groupObjects(items,key)).map(([k,g])=>({key:k,label:label(g[0]),students:g.length,percentage:studentPct(g),present:g.reduce((n,s)=>n+s.present,0),absent:g.reduce((n,s)=>n+s.absent,0),late:g.reduce((n,s)=>n+s.late,0),leave:g.reduce((n,s)=>n+s.leave,0),classTeacher:unique(g.map(s=>s.classTeacher)).join(", "),presentToday:g.filter(s=>s.history.some(h=>h.date===today&&(h.status==="PRESENT"||h.status==="LATE"))).length,absentToday:g.filter(s=>s.history.some(h=>h.date===today&&h.status==="ABSENT")).length})).sort((a,b)=>a.label.localeCompare(b.label)); }
function aggregateTrend(points:TrendPoint[],mode:"daily"|"weekly"|"monthly") { if(mode==="daily") return points.map(p=>({...p,label:p.date.slice(5)})); const grouped=groupObjects(points,p=>{const d=new Date(`${p.date}T00:00:00`); if(mode==="monthly") return p.date.slice(0,7); const start=new Date(d); start.setDate(d.getDate()-((d.getDay()+6)%7)); return iso(start);}); return Object.entries(grouped).map(([label,g])=>{const total=g.reduce((n,p)=>n+p.total,0),attended=g.reduce((n,p)=>n+p.attended,0);return{label:mode==="monthly"?label:`W ${label.slice(5)}`,percentage:total?round(attended*100/total):0};}); }
