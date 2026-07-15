import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import { timetableApi } from "./api";
import type { TimetableDay } from "./types";
import { DAYS, DAY_LABELS } from "./constants";

interface Props {
  timetableId: number;
  currentDay: TimetableDay;
  onCopied: () => void;
}

export function CopyToolbar({ timetableId, currentDay, onCopied }: Props) {
  const [copyDayTarget, setCopyDayTarget] = useState("");
  const [copyWeekTarget, setCopyWeekTarget] = useState("");
  const [timetables, setTimetables] = useState<{ id: number; weekStart: string; section: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [showWeekCopy, setShowWeekCopy] = useState(false);

  const copyDay = async () => {
    if (!copyDayTarget) return;
    try {
      setLoading(true);
      await timetableApi.copyDay(timetableId, {
        sourceDay: currentDay,
        targetDay: copyDayTarget,
      });
      toast.success(`Copied ${DAY_LABELS[currentDay]} to ${DAY_LABELS[copyDayTarget as TimetableDay]}`);
      setCopyDayTarget("");
      onCopied();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };

  const copyToAllDays = async () => {
    try {
      setLoading(true);
      for (const day of DAYS) {
        if (day === currentDay) continue;
        await timetableApi.copyDay(timetableId, {
          sourceDay: currentDay,
          targetDay: day,
        });
      }
      toast.success(`Copied ${DAY_LABELS[currentDay]} to all days`);
      onCopied();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };

  const loadTimetables = async () => {
    try {
      const tables = await timetableApi.list();
      setTimetables(
        tables
          .filter((t) => t.id !== timetableId && t.status === "DRAFT")
          .map((t) => ({ id: t.id, weekStart: t.weekStart, section: t.section }))
      );
    } catch (e) {
      toast.error(handleApiError(e).message);
    }
  };

  const copyWeek = async () => {
    if (!copyWeekTarget) return;
    try {
      setLoading(true);
      await timetableApi.copyWeek(timetableId, {
        sourceTimetableId: timetableId,
        targetTimetableId: Number(copyWeekTarget),
      });
      toast.success("Copied entire week to target timetable");
      setCopyWeekTarget("");
      setShowWeekCopy(false);
      onCopied();
    } catch (e) {
      toast.error(handleApiError(e).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
      <span className="text-sm font-medium text-slate-600">Copy:</span>
      <Select
        label=""
        value={copyDayTarget}
        onChange={(e) => setCopyDayTarget(e.target.value)}
        options={[
          { label: "Copy day to...", value: "" },
          ...DAYS.filter((d) => d !== currentDay).map((d) => ({
            label: DAY_LABELS[d],
            value: d,
          })),
        ]}
      />
      <Button className="h-8 px-3 text-xs" onClick={copyDay} disabled={!copyDayTarget || loading}>
        Copy Day
      </Button>
      <Button className="h-8 px-3 text-xs" variant="secondary" onClick={copyToAllDays} disabled={loading}>
        Copy to All Days
      </Button>
      {!showWeekCopy ? (
        <Button
          className="h-8 px-3 text-xs"
          variant="ghost"
          onClick={() => {
            setShowWeekCopy(true);
            loadTimetables();
          }}
        >
          Copy Week
        </Button>
      ) : (
        <>
          <Select
            label=""
            value={copyWeekTarget}
            onChange={(e) => setCopyWeekTarget(e.target.value)}
            options={[
              { label: "Select target timetable", value: "" },
              ...timetables.map((t) => ({
                label: `${t.weekStart} — ${t.section}`,
                value: String(t.id),
              })),
            ]}
          />
          <Button className="h-8 px-3 text-xs" onClick={copyWeek} disabled={!copyWeekTarget || loading}>
            Copy Week
          </Button>
          <Button className="h-8 px-3 text-xs" variant="ghost" onClick={() => setShowWeekCopy(false)}>
            Cancel
          </Button>
        </>
      )}
    </div>
  );
}
