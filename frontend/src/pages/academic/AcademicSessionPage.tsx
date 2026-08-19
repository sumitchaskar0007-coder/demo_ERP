import { AlertTriangle, CalendarRange, CheckCircle2, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { academicSessionApi, type AcademicTerm, type AcademicYear, type RolloverPreview } from "@/features/academicSessions/api";
import { calendarState, dateFallsInTerm, localDateIso } from "@/features/academicSessions/calendar";
import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";

type Department = { id: number; name: string; status: string };

const inputClass = "h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-blue-500";
const buttonClass = "inline-flex min-h-11 items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50";

function message(error: unknown) {
  if (typeof error === "object" && error && "response" in error) {
    const value = error as { response?: { data?: { message?: string } } };
    return value.response?.data?.message || "The request could not be completed";
  }
  return "The request could not be completed";
}

export function AcademicSessionPage() {
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState(0);
  const [durationYears, setDurationYears] = useState(3);
  const [sourceTermId, setSourceTermId] = useState(0);
  const [targetTermId, setTargetTermId] = useState(0);
  const [preview, setPreview] = useState<RolloverPreview | null>(null);
  const [overrideTerm, setOverrideTerm] = useState<AcademicTerm | null>(null);
  const [overrideReason, setOverrideReason] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const [yearRows, departmentResponse] = await Promise.all([
      academicSessionApi.years(),
      apiClient.get<ApiResponse<Department[]>>("/api/principal/departments"),
    ]);
    setYears(yearRows);
    const active = departmentResponse.data.data.filter((row) => row.status === "ACTIVE");
    setDepartments(active);
    setDepartmentId((current) => current || active[0]?.id || 0);
    const allTerms = yearRows.flatMap((year) => year.terms);
    const activeTerm = allTerms.find((term) => term.status === "ACTIVE");
    setSourceTermId((current) => current || activeTerm?.id || 0);
  }, []);

  useEffect(() => {
    load().catch((error) => toast.error(message(error)));
  }, [load]);

  const terms = useMemo(() => years.flatMap((year) => year.terms.map((term) => ({ ...term, yearName: year.name }))), [years]);
  const today = localDateIso();
  const currentCalendar = useMemo(() => calendarState(years, today), [years, today]);
  const targetTerm = terms.find((term) => term.id === targetTermId);
  const rolloverTooEarly = Boolean(targetTerm && today < targetTerm.startDate);

  async function action(work: () => Promise<unknown>, success: string) {
    setBusy(true);
    try {
      await work();
      toast.success(success);
      setPreview(null);
      await load();
      return true;
    } catch (error) {
      toast.error(message(error));
      return false;
    } finally {
      setBusy(false);
    }
  }

  function requestActivation(term: AcademicTerm) {
    if (dateFallsInTerm(today, term)) {
      action(() => academicSessionApi.activateTerm(term.id), `${term.name} activated`);
      return;
    }
    setOverrideTerm(term);
    setOverrideReason("");
  }

  return (
    <div className="space-y-6 pb-12">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">Academic control</p>
        <h1 className="mt-2 text-3xl font-black text-slate-950">Academic years and semesters</h1>
        <p className="mt-2 text-slate-600">Configure your college semesters inside the Super Admin academic year and promote students only after a checked preview.</p>
      </div>

      {currentCalendar.mismatch && currentCalendar.expectedTerm && <Card className="border-red-300 bg-red-50 p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" /><div><h2 className="font-bold text-red-900">Active semester does not match today&apos;s calendar</h2><p className="mt-1 text-sm text-red-800">Today is {today}. The configured calendar expects <b>{currentCalendar.expectedTerm.name}</b>, but {currentCalendar.activeTerm ? <><b>{currentCalendar.activeTerm.name}</b> is active</> : "no semester is active"}. Student promotion has not happened automatically.</p></div></div>
          <button className={buttonClass} disabled={busy} onClick={() => requestActivation(currentCalendar.expectedTerm!)}>Activate expected semester</button>
        </div>
      </Card>}

      {overrideTerm && <Card className="border-amber-300 bg-amber-50 p-5">
        <div className="flex gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" /><div><h2 className="font-bold text-amber-950">Authorized date override</h2><p className="mt-1 text-sm text-amber-900">{overrideTerm.name} runs from {overrideTerm.startDate} to {overrideTerm.endDate}. Activating it on {today} is outside its calendar and will be recorded in the audit log. This does not promote students.</p></div></div>
        <label className="mt-4 block text-sm font-bold text-amber-950">Reason for exceptional correction<textarea className="mt-1 min-h-24 w-full rounded-xl border border-amber-300 bg-white p-3 font-normal outline-none focus:border-amber-600" maxLength={500} value={overrideReason} onChange={(event) => setOverrideReason(event.target.value)} placeholder="Explain why this semester must be activated outside its dates (minimum 10 characters)" /></label>
        <div className="mt-3 flex gap-2"><button className="min-h-11 rounded-xl border border-slate-300 bg-white px-4 text-sm font-bold" disabled={busy} onClick={() => setOverrideTerm(null)}>Cancel</button><button className={buttonClass} disabled={busy || overrideReason.trim().length < 10} onClick={() => action(() => academicSessionApi.activateTerm(overrideTerm.id, true, overrideReason.trim()), `${overrideTerm.name} activated with authorized override`).then((success) => { if (success) setOverrideTerm(null); })}>Confirm override</button></div>
      </Card>}

      <Card className="p-5">
        <h2 className="text-lg font-bold">Department semester structure</h2>
        <p className="mt-1 text-sm text-slate-500">3 years creates semesters 1–6; 4 years creates semesters 1–8.</p>
        <div className="mt-4 grid gap-3 md:grid-cols-[1fr_180px_auto]">
          <select className={inputClass} value={departmentId} onChange={(e) => setDepartmentId(Number(e.target.value))}>
            {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
          </select>
          <select className={inputClass} value={durationYears} onChange={(e) => setDurationYears(Number(e.target.value))}>
            {[1, 2, 3, 4, 5].map((value) => <option key={value} value={value}>{value} year{value > 1 ? "s" : ""} ({value * 2} semesters)</option>)}
          </select>
          <button className={buttonClass} disabled={busy || !departmentId} onClick={() => action(() => academicSessionApi.configureSemesters(departmentId, durationYears), "Semester structure saved")}>Save structure</button>
        </div>
      </Card>

      <div className="space-y-4">
        {years.map((year) => <Card key={year.id} className="p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div><h2 className="text-xl font-black">{year.name}</h2><p className="text-sm text-slate-500">{year.startDate} to {year.endDate}</p></div>
            <div className="flex items-center gap-2"><span className={`rounded-full px-3 py-1 text-xs font-bold ${year.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"}`}>{year.status}</span></div>
          </div>
          <div className="mt-4 grid gap-3 lg:grid-cols-2">{year.terms.map((term) => <TermEditor key={term.id} term={term} today={today} busy={busy} onSave={(updated) => action(() => academicSessionApi.updateTerm(updated), "Semester dates updated")} onActivate={() => requestActivation(term)} />)}</div>
        </Card>)}
      </div>

      <Card className="border-amber-200 p-5">
        <div className="flex gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" /><div><h2 className="text-lg font-bold">Controlled semester rollover</h2><p className="text-sm text-slate-600">Nothing changes until preview has zero blocked students and you explicitly confirm. A single transaction rolls everything back if any student fails.</p></div></div>
        <div className="mt-4 grid gap-3 md:grid-cols-[1fr_1fr_auto]">
          <select className={inputClass} value={sourceTermId} onChange={(e) => { setSourceTermId(Number(e.target.value)); setPreview(null); }}><option value={0}>Source semester</option>{terms.map((term) => <option key={term.id} value={term.id}>{term.yearName} · {term.name} ({term.status})</option>)}</select>
          <select className={inputClass} value={targetTermId} onChange={(e) => { setTargetTermId(Number(e.target.value)); setPreview(null); }}><option value={0}>Target semester</option>{terms.map((term) => <option key={term.id} value={term.id}>{term.yearName} · {term.name} ({term.status})</option>)}</select>
          <button className={buttonClass} disabled={busy || !sourceTermId || !targetTermId} onClick={async () => { setBusy(true); try { setPreview(await academicSessionApi.preview(sourceTermId, targetTermId)); } catch (error) { toast.error(message(error)); } finally { setBusy(false); } }}><RefreshCw className="mr-2 h-4 w-4" />Preview</button>
        </div>
        {preview && <div className="mt-5 rounded-2xl border bg-slate-50 p-4"><div className="grid gap-3 sm:grid-cols-4"><Metric label="Students" value={preview.totalStudents} /><Metric label="Promote" value={preview.promotableStudents} /><Metric label="Graduate" value={preview.graduatingStudents} /><Metric label="Blocked" value={preview.blockedStudents} danger={preview.blockedStudents > 0} /></div>{preview.blockedStudents > 0 && <div className="mt-4 space-y-2">{preview.students.filter((row) => row.decision === "BLOCKED").slice(0, 10).map((row) => <p key={row.enrollmentId} className="rounded-lg bg-red-50 p-2 text-sm text-red-700"><b>{row.studentName}:</b> {row.reason}</p>)}</div>}{rolloverTooEarly && targetTerm && <p className="mt-4 rounded-xl bg-amber-100 p-3 text-sm font-semibold text-amber-900">Preview is available for planning, but rollover cannot be confirmed before {targetTerm.startDate}.</p>}<button className={`${buttonClass} mt-4`} disabled={busy || preview.blockedStudents > 0 || preview.totalStudents === 0 || rolloverTooEarly} onClick={() => { if (!window.confirm(`Promote ${preview.promotableStudents} students and graduate ${preview.graduatingStudents}? This cannot be partially applied.`)) return; action(() => academicSessionApi.rollover(sourceTermId, targetTermId), "Semester rollover completed"); }}><CheckCircle2 className="mr-2 h-4 w-4" />Confirm rollover</button></div>}
      </Card>
    </div>
  );
}

function TermEditor({ term, today, busy, onSave, onActivate }: { term: AcademicTerm; today: string; busy: boolean; onSave: (term: AcademicTerm) => void; onActivate: () => void }) {
  const [value, setValue] = useState(term);
  useEffect(() => setValue(term), [term]);
  const inWindow = dateFallsInTerm(today, value);
  return <div className="rounded-2xl border border-slate-200 p-4"><div className="flex items-center justify-between"><p className="font-bold"><CalendarRange className="mr-2 inline h-4 w-4 text-blue-600" />{value.name}</p><span className="text-xs font-bold text-slate-500">{value.status}</span></div><div className="mt-3 grid grid-cols-2 gap-2"><input type="date" className={inputClass} value={value.startDate} disabled={value.status === "CLOSED"} onChange={(e) => setValue({ ...value, startDate: e.target.value })} /><input type="date" className={inputClass} value={value.endDate} disabled={value.status === "CLOSED"} onChange={(e) => setValue({ ...value, endDate: e.target.value })} /></div><div className="mt-3 flex gap-2">{value.status !== "CLOSED" && <button className="min-h-10 rounded-xl border px-3 text-sm font-bold" disabled={busy} onClick={() => onSave(value)}>Save dates</button>}{value.status !== "ACTIVE" && <button className={buttonClass} disabled={busy} onClick={onActivate}>{inWindow ? "Activate" : "Override & activate"}</button>}</div></div>;
}

function Metric({ label, value, danger }: { label: string; value: number; danger?: boolean }) {
  return <div className={`rounded-xl bg-white p-3 ${danger ? "text-red-700" : "text-slate-800"}`}><p className="text-xs font-bold uppercase text-slate-400">{label}</p><p className="mt-1 text-2xl font-black">{value}</p></div>;
}
