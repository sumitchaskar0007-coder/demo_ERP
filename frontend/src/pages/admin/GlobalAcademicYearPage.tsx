import { CalendarDays, CheckCircle2, Pencil, Plus, ShieldCheck } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import {
  globalAcademicYearApi,
  type GlobalAcademicYear,
  type SaveGlobalAcademicYear,
} from "@/features/globalAcademicYears/api";
import { handleApiError } from "@/lib/handleApiError";

const inputClass =
  "mt-1 h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
const primaryButton =
  "inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-bold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50";

function defaultForm(): SaveGlobalAcademicYear {
  const now = new Date();
  const startYear = now.getMonth() >= 4 ? now.getFullYear() + 1 : now.getFullYear();
  return {
    name: `${startYear}-${startYear + 1}`,
    startDate: `${startYear}-06-15`,
    endDate: `${startYear + 1}-04-30`,
  };
}

export function GlobalAcademicYearPage() {
  const [years, setYears] = useState<GlobalAcademicYear[]>([]);
  const [form, setForm] = useState<SaveGlobalAcademicYear>(defaultForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const active = useMemo(() => years.find((year) => year.status === "ACTIVE"), [years]);

  const load = useCallback(async () => {
    setYears(await globalAcademicYearApi.list());
  }, []);

  useEffect(() => {
    load().catch((error) => toast.error(handleApiError(error).message));
  }, [load]);

  function edit(year: GlobalAcademicYear) {
    setEditingId(year.id);
    setForm({ name: year.name, startDate: year.startDate, endDate: year.endDate });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(defaultForm());
  }

  async function save() {
    setBusy(true);
    try {
      if (editingId) {
        await globalAcademicYearApi.update(editingId, form);
        toast.success("Academic year dates updated for all attached colleges");
      } else {
        await globalAcademicYearApi.create(form);
        toast.success("Draft academic year created");
      }
      cancelEdit();
      await load();
      window.dispatchEvent(new Event("academic-year:changed"));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setBusy(false);
    }
  }

  async function activate(year: GlobalAcademicYear) {
    const warning = active
      ? `Activate ${year.name} and close ${active.name} as the global current year?`
      : `Activate ${year.name} for every active college?`;
    if (!window.confirm(`${warning}\n\nStudent promotion will still require Principal confirmation.`)) return;
    setBusy(true);
    try {
      await globalAcademicYearApi.activate(year.id);
      toast.success(`${year.name} is now the global current academic year`);
      await load();
      window.dispatchEvent(new Event("academic-year:changed"));
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6 pb-12">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-blue-600">Global academic control</p>
        <h1 className="mt-2 text-3xl font-black text-slate-950">Academic years</h1>
        <p className="mt-2 max-w-3xl text-slate-600">
          Super Admin defines the organization-wide year. Principals configure Odd and Even semester dates inside this range.
        </p>
      </div>

      {active && (
        <Card className="border-emerald-200 bg-gradient-to-r from-emerald-50 to-white p-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="grid h-11 w-11 place-items-center rounded-xl bg-emerald-100 text-emerald-700"><ShieldCheck className="h-5 w-5" /></span>
              <div><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Current academic year</p><h2 className="text-2xl font-black text-slate-950">{active.name}</h2><p className="text-sm text-slate-600">{active.startDate} to {active.endDate} · {active.attachedColleges} colleges attached</p></div>
            </div>
            <button className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-emerald-200 bg-white px-4 text-sm font-bold text-emerald-800 hover:bg-emerald-50" onClick={() => edit(active)}><Pencil className="h-4 w-4" />Edit current dates</button>
          </div>
        </Card>
      )}

      <Card className="p-5">
        <div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-50 text-blue-700">{editingId ? <Pencil className="h-5 w-5" /> : <Plus className="h-5 w-5" />}</span><div><h2 className="text-lg font-bold">{editingId ? "Edit academic year" : "Prepare next academic year"}</h2><p className="text-sm text-slate-500">Dates may be edited while the year is draft or active.</p></div></div>
        <div className="mt-5 grid gap-4 md:grid-cols-[1fr_1fr_1fr_auto]">
          <label className="text-sm font-semibold text-slate-700">Year name<input className={inputClass} value={form.name} placeholder="2026-2027" onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
          <label className="text-sm font-semibold text-slate-700">Starts<input type="date" className={inputClass} value={form.startDate} onChange={(event) => setForm({ ...form, startDate: event.target.value })} /></label>
          <label className="text-sm font-semibold text-slate-700">Ends<input type="date" className={inputClass} value={form.endDate} onChange={(event) => setForm({ ...form, endDate: event.target.value })} /></label>
          <div className="flex items-end gap-2"><button className={primaryButton} disabled={busy || !form.name || !form.startDate || !form.endDate} onClick={save}>{editingId ? "Save changes" : "Create draft"}</button>{editingId && <button className="min-h-11 rounded-xl border px-4 text-sm font-bold text-slate-600" disabled={busy} onClick={cancelEdit}>Cancel</button>}</div>
        </div>
      </Card>

      <div className="space-y-3">
        {years.map((year) => (
          <Card key={year.id} className="p-5">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-start gap-3"><span className="grid h-11 w-11 place-items-center rounded-xl bg-slate-100 text-blue-700"><CalendarDays className="h-5 w-5" /></span><div><div className="flex items-center gap-2"><h2 className="text-xl font-black">{year.name}</h2><span className={`rounded-full px-2.5 py-1 text-[11px] font-bold ${year.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : year.status === "CLOSED" ? "bg-slate-200 text-slate-600" : "bg-blue-50 text-blue-700"}`}>{year.status}</span></div><p className="mt-1 text-sm text-slate-500">{year.startDate} to {year.endDate} · {year.attachedColleges} colleges</p></div></div>
              <div className="flex gap-2">{year.status !== "CLOSED" && <button className="min-h-11 rounded-xl border px-4 text-sm font-bold text-slate-700 hover:bg-slate-50" disabled={busy} onClick={() => edit(year)}><Pencil className="mr-2 inline h-4 w-4" />Edit</button>}{year.status === "DRAFT" && <button className={primaryButton} disabled={busy} onClick={() => activate(year)}><CheckCircle2 className="h-4 w-4" />Activate globally</button>}</div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
