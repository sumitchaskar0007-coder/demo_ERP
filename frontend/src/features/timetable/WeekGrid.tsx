import { useState } from "react";
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import { LectureCard } from "./LectureCard";
import { PeriodPopup } from "./PeriodPopup";
import { DAYS, DAY_LABELS, DEFAULT_PERIODS, EMPTY_CELL_STYLE } from "./constants";
import type { TimetableEntry } from "./types";

interface Props {
  timetableId: number;
  entries: TimetableEntry[];
  onSaved: () => void;
  searchHighlight?: string;
}

export function WeekGrid({ timetableId, entries, onSaved, searchHighlight }: Props) {
  const [popup, setPopup] = useState<{
    day: string;
    period: number;
    entry?: TimetableEntry | null;
  } | null>(null);
  const [activeEntry, setActiveEntry] = useState<TimetableEntry | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const getEntry = (day: string, period: number) =>
    entries.find((e) => e.dayOfWeek === day && e.periodNumber === period);

  const handleDragStart = (event: {
    active: { data: { current?: { entry?: TimetableEntry } } };
  }) => {
    const entry = event.active.data.current?.entry;
    if (entry) setActiveEntry(entry);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveEntry(null);
    const { active, over } = event;
    if (!over) return;

    const entry = active.data.current?.entry as TimetableEntry | undefined;
    const cellId = over.id as string;
    if (!entry || !cellId.startsWith("cell-")) return;

    const [, targetDay, targetPeriod] = cellId.split("-");
    if (entry.dayOfWeek === targetDay && entry.periodNumber === Number(targetPeriod)) return;

    onSaved();
  };

  const highlightLower = searchHighlight?.toLowerCase() ?? "";

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[800px] border-collapse text-xs">
          <thead>
            <tr>
              <th className="w-24 border border-sky-200 bg-gradient-to-r from-brand-50 to-sky-50 px-2 py-2 text-left text-xs font-bold text-brand-700">
                Time
              </th>
              {DAYS.map((day) => (
                <th
                  key={day}
                  className="border border-sky-200 bg-gradient-to-r from-sky-50 to-cyan-50 px-2 py-2 text-center text-xs font-bold text-brand-700"
                >
                  {DAY_LABELS[day]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {DEFAULT_PERIODS.map((period) => (
              <tr key={period.number}>
                <td className="whitespace-nowrap border border-slate-200 bg-slate-50/70 px-2 py-1">
                  <p className="font-medium text-slate-700">{period.label}</p>
                  <p className="text-[10px] text-slate-400">
                    {period.start} – {period.end}
                  </p>
                </td>
                {DAYS.map((day) => {
                  if (period.isBreak) {
                    return (
                      <td
                        key={`${day}-${period.number}`}
                        className="border border-orange-200 bg-orange-50 px-1 py-1 text-center text-xs font-semibold text-orange-700"
                      >
                        {period.label}
                      </td>
                    );
                  }

                  const entry = getEntry(day, period.number);
                  const cellId = `cell-${day}-${period.number}`;
                  const isEmpty = !entry;

                  const isHighlighted =
                    highlightLower &&
                    entry &&
                    (entry.subject?.toLowerCase().includes(highlightLower) ||
                      entry.teacher?.toLowerCase().includes(highlightLower) ||
                      entry.room?.toLowerCase().includes(highlightLower));

                  return (
                    <td
                      key={`${day}-${period.number}`}
                      id={cellId}
                      className={cn(
                        "border border-slate-200 px-1 py-1 transition-colors",
                        isEmpty && "cursor-pointer hover:bg-blue-50",
                        EMPTY_CELL_STYLE,
                      )}
                      onClick={() => {
                        if (isEmpty) setPopup({ day, period: period.number });
                      }}
                    >
                      <div
                        className={cn(
                          "min-h-[56px]",
                          isHighlighted && "ring-2 ring-yellow-400 rounded-lg",
                        )}
                        data-cell-id={cellId}
                      >
                        {entry ? (
                          <LectureCard
                            entry={entry}
                            isDroppable={false}
                            onEdit={(e) => setPopup({ day, period: period.number, entry: e })}
                            onDelete={(e) => {
                              if (confirm(`Delete ${e.subject || e.type}?`)) {
                                import("./api").then(({ timetableApi }) =>
                                  timetableApi.deleteEntry(timetableId, e.id).then(onSaved),
                                );
                              }
                            }}
                          />
                        ) : (
                          <div className="flex h-full min-h-[56px] items-center justify-center rounded-lg opacity-0 transition-opacity hover:opacity-100">
                            <Plus className="h-4 w-4 text-slate-300" />
                          </div>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <DragOverlay>
        {activeEntry && (
          <div className="w-48 opacity-90">
            <LectureCard
              entry={activeEntry}
              isDroppable={false}
              onEdit={() => {}}
              onDelete={() => {}}
            />
          </div>
        )}
      </DragOverlay>

      {popup && (
        <PeriodPopup
          open={true}
          onClose={() => setPopup(null)}
          timetableId={timetableId}
          dayOfWeek={popup.day}
          periodNumber={popup.period}
          entry={popup.entry}
          onSaved={onSaved}
        />
      )}
    </DndContext>
  );
}
