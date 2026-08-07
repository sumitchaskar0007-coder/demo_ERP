import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Download, FileSpreadsheet, Printer, Redo, Undo, Calendar } from "lucide-react";
import { Card } from "@/components/common/Card";
import { Button } from "@/components/common/Button";
import { Select } from "@/components/common/Select";
import { Loader } from "@/components/common/Loader";
import { EmptyState } from "@/components/common/EmptyState";
import { handleApiError } from "@/lib/handleApiError";
import { timetableApi } from "./api";
import { WeekGrid } from "./WeekGrid";
import { CopyToolbar } from "./CopyToolbar";
import { SearchPanel } from "./SearchPanel";
import { useUndoRedo } from "./useUndoRedo";
import { DAYS, DAY_LABELS, DEFAULT_PERIODS } from "./constants";
import { exportToPDF, exportToExcel } from "./exportUtils";
import { useAuth } from "@/features/auth/authStore";
import type { Timetable, TimetableEntry, TimetableDay } from "./types";

const BTN_SM = "h-8 px-3 text-xs";

export function WeeklyTimetablePage() {
  const { user } = useAuth();
  const [timetables, setTimetables] = useState<Timetable[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [dayTab, setDayTab] = useState<TimetableDay>("MONDAY");
  const [searchHighlights, setSearchHighlights] = useState("");
  const [showCopy, setShowCopy] = useState(false);
  const [creatingWeek, setCreatingWeek] = useState(false);
  const [weekInput, setWeekInput] = useState("");

  const { state: entries, undo, redo, reset, canUndo, canRedo } = useUndoRedo<TimetableEntry[]>([]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const tables = await timetableApi.list();
      setTimetables(tables);
      if (tables.length > 0) setSelectedId((current) => current ?? tables[0].id);
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const selected = timetables.find((t) => t.id === selectedId);

  useEffect(() => {
    if (selected) reset(selected.entries || []);
  }, [selected, reset]);

  const refresh = async () => {
    await load();
    if (selectedId) {
      const tables = await timetableApi.list();
      const fresh = tables.find((t) => t.id === selectedId);
      if (fresh) reset(fresh.entries || []);
    }
  };

  const publish = async () => {
    if (!selectedId) return;
    try {
      setPublishing(true);
      await timetableApi.publish(selectedId);
      toast.success("Timetable published!");
      await refresh();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setPublishing(false);
    }
  };

  const createWeek = async () => {
    if (!weekInput) return;
    try {
      setCreatingWeek(true);
      await timetableApi.create({ weekStart: weekInput });
      toast.success("Week timetable created");
      setWeekInput("");
      await load();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setCreatingWeek(false);
    }
  };

  const entryCount = entries.length;

  if (loading)
    return (
      <div className="page-container">
        <Loader />
      </div>
    );

  return (
    <div className="page-container space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="page-title">Weekly Timetable</h1>
          <p className="page-subtitle">
            Manage weekly schedules with drag-and-drop, copy, and publish.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {selected?.status === "DRAFT" && (
            <Button
              onClick={publish}
              loading={publishing}
              className="bg-teal-600 hover:bg-teal-700"
            >
              Publish
            </Button>
          )}
        </div>
      </div>

      <Card className="p-4">
        <div className="flex flex-wrap items-end gap-4">
          <Select
            label="Select timetable"
            value={selectedId ? String(selectedId) : ""}
            onChange={(e) => setSelectedId(Number(e.target.value))}
            options={timetables.map((t) => ({
              label: `${t.weekStart} — ${t.className} (${t.section}) [${t.status}]`,
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
            <Button
              className={BTN_SM}
              onClick={createWeek}
              loading={creatingWeek}
              disabled={!weekInput}
            >
              <Calendar className="h-3.5 w-3.5" /> New Week
            </Button>
          </div>
        </div>

        {selected && (
          <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500">
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${selected.status === "PUBLISHED" ? "bg-teal-50 text-teal-700" : selected.status === "ARCHIVED" ? "bg-slate-100 text-slate-600" : "bg-orange-50 text-orange-700"}`}
            >
              {selected.status}
            </span>
            <span>·</span>
            <span>{entryCount} entries</span>
            <span>·</span>
            <span>
              {selected.className} — {selected.section}
            </span>
          </div>
        )}
      </Card>

      {selected && (
        <>
          <Card className="p-4">
            <div className="flex flex-wrap items-center gap-3">
              <SearchPanel
                onResults={(r) => {
                  const days = [...new Set(r.map((e) => e.dayOfWeek))];
                  setSearchHighlights(days.join(","));
                }}
                onClear={() => setSearchHighlights("")}
              />
              <div className="ml-auto flex items-center gap-1">
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={undo}
                  disabled={!canUndo}
                  title="Undo"
                >
                  <Undo className="h-3.5 w-3.5" />
                </Button>
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={redo}
                  disabled={!canRedo}
                  title="Redo"
                >
                  <Redo className="h-3.5 w-3.5" />
                </Button>
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={() => setShowCopy(!showCopy)}
                  title="Copy"
                >
                  Copy
                </Button>
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={() =>
                    exportToPDF(
                      entries,
                      `${selected.className} Timetable`,
                      DEFAULT_PERIODS,
                      user?.collegeName ?? undefined,
                    )
                  }
                  disabled={entryCount === 0}
                  title="Export PDF"
                >
                  <Download className="h-3.5 w-3.5" /> PDF
                </Button>
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={() =>
                    exportToExcel(
                      entries,
                      `${selected.className} Timetable`,
                      DEFAULT_PERIODS,
                      user?.collegeName ?? undefined,
                    )
                  }
                  disabled={entryCount === 0}
                  title="Export Excel"
                >
                  <FileSpreadsheet className="h-3.5 w-3.5" /> Excel
                </Button>
                <Button
                  className={BTN_SM}
                  variant="ghost"
                  onClick={() => window.print()}
                  disabled={entryCount === 0}
                  title="Print"
                >
                  <Printer className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
            {showCopy && (
              <div className="mt-3">
                <CopyToolbar timetableId={selectedId!} currentDay={dayTab} onCopied={refresh} />
              </div>
            )}
          </Card>

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
              entries={entries}
              onSaved={refresh}
              searchHighlight={searchHighlights}
            />
          </div>

          <div className="md:hidden space-y-3">
            {DEFAULT_PERIODS.filter((p) => !p.isBreak).map((period) => {
              const entry = entries.find(
                (e) => e.dayOfWeek === dayTab && e.periodNumber === period.number,
              );
              return (
                <div
                  key={period.number}
                  className="flex items-start gap-3 rounded-lg border border-slate-200 p-3"
                >
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
          title="No timetables found"
          description="Create a weekly timetable to get started."
        />
      )}
    </div>
  );
}
