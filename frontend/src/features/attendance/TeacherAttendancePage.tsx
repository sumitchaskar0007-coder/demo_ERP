import { useEffect, useMemo, useState } from "react";
import { CalendarCheck2, Check, Clock3, History, Search, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { attendanceApi, type AttendanceStatus, type Lecture, type Roster, type SessionSummary } from "./api";
import { handleApiError } from "@/lib/handleApiError";
import { cn } from "@/lib/utils";

const statusStyles: Record<AttendanceStatus, string> = {
  PRESENT: "border-emerald-200 bg-emerald-50 text-emerald-700",
  ABSENT: "border-rose-200 bg-rose-50 text-rose-700",
  LATE: "border-amber-200 bg-amber-50 text-amber-700",
  LEAVE: "border-blue-200 bg-blue-50 text-blue-700",
};
const statuses = Object.keys(statusStyles) as AttendanceStatus[];

export function TeacherAttendancePage() {
  const [lecture, setLecture] = useState<Lecture | null>();
  const [roster, setRoster] = useState<Roster>();
  const [history, setHistory] = useState<SessionSummary[]>([]);
  const [query, setQuery] = useState("");
  const [tab, setTab] = useState<"current" | "history">("current");
  const [busy, setBusy] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const load = async () => {
    try {
      const active = await attendanceApi.current();
      setLecture(active);
      setRoster(active ? await attendanceApi.roster(active.lectureId) : undefined);
    } catch (error) { toast.error(handleApiError(error).message); }
  };
  useEffect(() => { void load(); }, []);
  useEffect(() => {
    if (tab === "history") attendanceApi.history({}).then(setHistory).catch((e) => toast.error(handleApiError(e).message));
  }, [tab]);

  const visible = useMemo(() => roster?.students.filter((s) => `${s.studentName} ${s.rollNumber} ${s.admissionNumber}`.toLowerCase().includes(query.toLowerCase())) ?? [], [roster, query]);
  const counts = useMemo(() => Object.fromEntries(statuses.map((status) => [status, roster?.students.filter((s) => s.status === status).length ?? 0])) as Record<AttendanceStatus, number>, [roster]);
  const updateStudent = (studentId: number, patch: Partial<{ status: AttendanceStatus; remarks: string }>) =>
    setRoster((current) => current && ({ ...current, students: current.students.map((s) => s.studentId === studentId ? { ...s, ...patch } : s) }));
  const markAll = (status: AttendanceStatus) => setRoster((current) => current && ({ ...current, students: current.students.map((s) => ({ ...s, status })) }));

  async function save(submit: boolean) {
    if (!roster || !lecture) return;
    setBusy(true);
    try {
      const payload = roster.students.map(({ studentId, status, remarks }) => ({ studentId, status, remarks }));
      const updated = lecture.sessionId
        ? await attendanceApi.update(lecture.sessionId, payload, submit)
        : await attendanceApi.create(lecture.lectureId, payload, submit);
      setRoster(updated); setLecture(updated.lecture); setConfirming(false);
      toast.success(submit ? "Attendance submitted and locked" : "Draft saved");
    } catch (error) { toast.error(handleApiError(error).message); } finally { setBusy(false); }
  }

  if (lecture === undefined) return <Loader label="Checking your scheduled lecture..." />;
  return (
    <div className="page-container space-y-6 pb-12">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div><p className="text-xs font-bold uppercase tracking-widest text-brand-600">Teacher workspace</p><h1 className="page-title mt-1">Attendance</h1><p className="mt-1 text-sm text-slate-500">Attendance is available only for your current scheduled lecture.</p></div>
        <div className="flex rounded-xl border bg-white p-1 shadow-sm">
          <button onClick={() => setTab("current")} className={cn("rounded-lg px-4 py-2 text-sm font-semibold", tab === "current" && "bg-brand-600 text-white")}><CalendarCheck2 className="mr-2 inline h-4 w-4" />Current</button>
          <button onClick={() => setTab("history")} className={cn("rounded-lg px-4 py-2 text-sm font-semibold", tab === "history" && "bg-brand-600 text-white")}><History className="mr-2 inline h-4 w-4" />History</button>
        </div>
      </header>

      {tab === "history" ? <HistoryTable rows={history} /> : !lecture ? (
        <Card className="grid min-h-[360px] place-items-center p-8 text-center"><div><Clock3 className="mx-auto h-12 w-12 text-slate-300" /><h2 className="mt-4 text-lg font-bold">No active lecture right now</h2><p className="mt-2 max-w-md text-sm text-slate-500">Attendance opens automatically when one of your timetable lectures starts.</p></div></Card>
      ) : (
        <>
          <Card className="overflow-hidden border-0 bg-gradient-to-r from-indigo-600 to-blue-600 text-white shadow-lg">
            <div className="grid gap-5 p-6 md:grid-cols-[1fr_auto] md:items-center"><div><div className="flex flex-wrap items-center gap-2 text-xs font-bold uppercase tracking-wide text-blue-100"><span>{lecture.period}</span><span>•</span><span>{lecture.lectureType}</span><span className="rounded-full bg-white/15 px-2.5 py-1">{lecture.sessionStatus || "Not started"}</span></div><h2 className="mt-3 text-2xl font-bold">{lecture.subject}</h2><p className="mt-1 text-blue-100">{lecture.year} - {lecture.division} · {lecture.department}{lecture.room ? ` · Room ${lecture.room}` : ""}</p></div><div className="rounded-2xl bg-white/10 px-6 py-4 text-center backdrop-blur"><Clock3 className="mx-auto h-5 w-5" /><p className="mt-1 text-lg font-bold">{lecture.startTime.slice(0,5)} - {lecture.endTime.slice(0,5)}</p><p className="text-xs text-blue-100">Active lecture window</p></div></div>
          </Card>
          {!roster ? <Loader label="Loading class roster..." /> : (
            <Card className="overflow-hidden">
              <div className="border-b p-5"><div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center"><div><h2 className="font-bold">Student roster</h2><p className="text-xs text-slate-500">{roster.totalStudents} active students · default status is Present</p></div><div className="flex flex-wrap gap-2"><Button variant="secondary" disabled={!roster.editable} onClick={() => markAll("PRESENT")}>Mark all present</Button><Button variant="secondary" disabled={!roster.editable} onClick={() => markAll("ABSENT")}>Reset to absent</Button></div></div><div className="mt-4 grid gap-2 sm:grid-cols-4">{statuses.map((status) => <div key={status} className={cn("rounded-xl border px-4 py-3", statusStyles[status])}><p className="text-xs font-bold">{status}</p><p className="text-xl font-black">{counts[status]}</p></div>)}</div></div>
              <div className="p-5"><div className="relative mb-4"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><Input aria-label="Search students" className="pl-10" placeholder="Search by name, roll or admission number" value={query} onChange={(e) => setQuery(e.target.value)} /></div>
                <div className="space-y-3">{visible.map((student) => <div key={student.studentId} className="rounded-xl border p-4 transition hover:border-brand-200 hover:shadow-sm"><div className="grid gap-4 lg:grid-cols-[minmax(220px,1fr)_auto_minmax(180px,0.7fr)] lg:items-center"><div className="flex items-center gap-3"><div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-slate-100 text-sm font-bold text-slate-600">{student.studentName.split(" ").map((x) => x[0]).slice(0,2).join("")}</div><div><p className="font-semibold">{student.studentName}</p><p className="text-xs text-slate-500">Roll {student.rollNumber || "—"} · {student.admissionNumber}</p></div></div><div className="flex flex-wrap gap-1.5">{statuses.map((status) => <button key={status} disabled={!roster.editable} onClick={() => updateStudent(student.studentId, { status })} className={cn("rounded-lg border px-2.5 py-2 text-[11px] font-bold transition", student.status === status ? statusStyles[status] : "border-slate-200 text-slate-500 hover:bg-slate-50", !roster.editable && "cursor-not-allowed opacity-70")}>{student.status === status && <Check className="mr-1 inline h-3 w-3" />}{status}</button>)}</div><Input disabled={!roster.editable} placeholder="Remarks (optional)" value={student.remarks || ""} onChange={(e) => updateStudent(student.studentId, { remarks: e.target.value })} /></div></div>)}</div>
              </div>
              <div className="sticky bottom-0 flex flex-wrap items-center justify-between gap-3 border-t bg-white/95 p-5 backdrop-blur"><p className="text-xs text-slate-500">Submitted attendance cannot be changed.</p>{roster.editable ? <div className="flex gap-2"><Button variant="secondary" disabled={busy} onClick={() => void save(false)}>Save draft</Button><Button disabled={busy} onClick={() => setConfirming(true)}>Review & submit</Button></div> : <span className="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-700"><Check className="h-4 w-4" />Submitted</span>}</div>
            </Card>
          )}
        </>
      )}
      {confirming && roster && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 p-4"><Card className="w-full max-w-lg p-6"><div className="flex items-start justify-between"><div><h2 className="text-xl font-bold">Confirm attendance</h2><p className="mt-1 text-sm text-slate-500">Review the totals. This action permanently locks the session.</p></div><button onClick={() => setConfirming(false)}><X /></button></div><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">{statuses.map((s) => <div key={s} className={cn("rounded-xl border p-3 text-center", statusStyles[s])}><p className="text-xs font-bold">{s}</p><p className="text-2xl font-black">{counts[s]}</p></div>)}</div><div className="mt-6 flex justify-end gap-2"><Button variant="secondary" onClick={() => setConfirming(false)}>Go back</Button><Button disabled={busy} onClick={() => void save(true)}>Confirm & submit</Button></div></Card></div>}
    </div>
  );
}

function HistoryTable({ rows }: { rows: SessionSummary[] }) {
  return <Card className="overflow-hidden"><div className="border-b p-5"><h2 className="font-bold">My attendance history</h2><p className="text-xs text-slate-500">Only lectures conducted by you are shown.</p></div>{rows.length === 0 ? <div className="p-12 text-center text-sm text-slate-500">No attendance sessions found.</div> : <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Date & time","Subject","Class","Present","Absent","Late","Leave","Status"].map((h) => <th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{rows.map((r) => <tr key={r.id} className="border-t"><td className="px-4 py-3">{r.date}<br/><span className="text-xs text-slate-400">{r.time}</span></td><td className="px-4 py-3 font-semibold">{r.subject}</td><td className="px-4 py-3">{r.year} - {r.division}</td><td className="px-4 py-3 text-emerald-600">{r.present}</td><td className="px-4 py-3 text-rose-600">{r.absent}</td><td className="px-4 py-3 text-amber-600">{r.late}</td><td className="px-4 py-3 text-blue-600">{r.leave}</td><td className="px-4 py-3"><span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold">{r.status}</span></td></tr>)}</tbody></table></div>}</Card>;
}
