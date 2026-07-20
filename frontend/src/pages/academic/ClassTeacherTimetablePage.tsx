import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Calendar } from "lucide-react";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Select } from "@/components/common/Select";
import { Loader } from "@/components/common/Loader";
import { EmptyState } from "@/components/common/EmptyState";
import { handleApiError } from "@/lib/handleApiError";
import { timetableApi, classTeacherApi } from "@/features/timetable/api";
import { WeekGrid } from "@/features/timetable/WeekGrid";
import { DAYS, DAY_LABELS, DEFAULT_PERIODS } from "@/features/timetable/constants";
import type { Timetable, TimetableDay, SectionInfo } from "@/features/timetable/types";

export function ClassTeacherTimetablePage() {
  const [section, setSection] = useState<SectionInfo | null>(null);
  const [timetables, setTimetables] = useState<Timetable[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [dayTab, setDayTab] = useState<TimetableDay>("MONDAY");
  const [weekInput, setWeekInput] = useState("");
  const [creating, setCreating] = useState(false);

  const load = useCallback(async () => {
    try {
      const [sec, , , tables] = await Promise.all([
        classTeacherApi.mySection(),
        classTeacherApi.subjects(),
        classTeacherApi.periods(),
        timetableApi.list(),
      ]);
      setSection(sec as unknown as SectionInfo);
      setTimetables(tables);
      if (tables.length > 0 && !selectedId) setSelectedId(tables[0].id);
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const selected = timetables.find((t) => t.id === selectedId);

  const refresh = async () => {
    await load();
  };

  const createWeek = async () => {
    if (!weekInput || !section) return;
    try {
      setCreating(true);
      await timetableApi.create({
        academicYearId: section.academicYearId || null,
        academicTermId: section.academicTermId || null,
        classId: section.classId,
        sectionId: section.sectionId,
        weekStart: weekInput,
      });
      toast.success("Timetable week created");
      setWeekInput("");
      await load();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setCreating(false);
    }
  };

  if (loading) return <div className="page-container"><Loader /></div>;

  return (
    <div className="page-container space-y-4">
      <div>
        <h1 className="page-title">My Class Timetable</h1>
        <p className="page-subtitle">View and manage your weekly class schedule.</p>
      </div>

      {section && (
        <Card className="p-4">
          <div className="grid gap-2 text-sm md:grid-cols-4">
            <div><b>Class</b><p>{section.courseYear || "—"}</p></div>
            <div><b>Division</b><p>{section.division || "—"}</p></div>
            <div><b>Year</b><p>{(section.yearName || "").replace("_", " ")}</p></div>
            <div><b>Academic Year</b><p>{section.academicYear || "—"}</p></div>
          </div>
        </Card>
      )}

      <Card className="p-4">
        <div className="flex flex-wrap items-end gap-4">
          <Select
            label="Select timetable"
            value={selectedId ? String(selectedId) : ""}
            onChange={(e) => setSelectedId(Number(e.target.value))}
            options={timetables.map((t) => ({
              label: `${t.weekStart} — ${t.section} [${t.status}]`,
              value: String(t.id),
            }))}
          />
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={weekInput}
              onChange={(e) => setWeekInput(e.target.value)}
              className="h-9 rounded-lg border px-2 text-sm"
            />
            <Button className="h-8 px-3 text-xs" onClick={createWeek} loading={creating} disabled={!weekInput}>
              <Calendar className="h-3.5 w-3.5" /> New Week
            </Button>
          </div>
        </div>
        {selected && (
          <p className="mt-2 text-xs text-slate-500">
            Status: <span className={`font-medium ${selected.status === "PUBLISHED" ? "text-teal-600" : "text-orange-600"}`}>{selected.status}</span>
            · {(selected.entries || []).length} entries
          </p>
        )}
      </Card>

      {selected && (
        <>
          <div className="flex gap-1.5 overflow-x-auto rounded-xl border border-slate-200 bg-slate-50 p-1.5">
            {DAYS.map((day) => (
              <button
                key={day}
                onClick={() => setDayTab(day)}
                className={`whitespace-nowrap rounded-lg px-4 py-2 text-sm font-semibold transition-colors ${
                  dayTab === day
                    ? "bg-gradient-to-r from-brand-600 to-sky-500 text-white shadow-sm"
                    : "text-slate-500 hover:bg-white hover:text-sky-700"
                }`}
              >
                {DAY_LABELS[day]}
              </button>
            ))}
          </div>

          <div className="hidden md:block">
            <WeekGrid
              timetableId={selectedId!}
              entries={selected.entries || []}
              onSaved={refresh}
            />
          </div>

          <div className="md:hidden space-y-3">
            {DEFAULT_PERIODS.filter((p) => !p.isBreak).map((period) => {
              const entry = (selected.entries || []).find(
                (e) => e.dayOfWeek === dayTab && e.periodNumber === period.number,
              );
              return (
                <div key={period.number} className="flex items-start gap-3 rounded-lg border border-slate-200 p-3">
                  <div className="shrink-0 text-xs text-slate-500">
                    <p className="font-medium">P{period.number}</p>
                    <p>{period.start}</p>
                  </div>
                  <div className="min-w-0 flex-1">
                    {entry ? (
                      <div>
                        <p className="text-sm font-semibold">{entry.subject || entry.type}</p>
                        {entry.teacher && <p className="text-xs text-slate-500">{entry.teacher}</p>}
                      </div>
                    ) : (
                      <p className="text-xs text-slate-400">Empty</p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}

      {!loading && timetables.length === 0 && (
        <EmptyState
          title="No timetables yet"
          description="Create a weekly timetable to get started."
        />
      )}
    </div>
  );
}
