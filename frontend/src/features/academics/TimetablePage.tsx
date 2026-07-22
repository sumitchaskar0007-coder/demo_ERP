import { useEffect, useMemo, useState } from "react";
import {
  BookOpen,
  CalendarDays,
  Check,
  ClipboardCopy,
  FileDown,
  GripVertical,
  Pencil,
  Plus,
  Printer,
  Redo2,
  Search,
  Settings2,
  Sheet,
  Trash2,
  Undo2,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import {
  weeklyTimetableApi,
  type WeeklyDivision,
  type WeeklyEntry,
  type WeeklyEntryInput,
  type WeeklyPeriod,
  type WeeklyPeriodInput,
  type WeeklyTimetable,
} from "./api";
import { exportWeeklyTimetableExcel, exportWeeklyTimetablePdf } from "./weeklyTimetableExport";
import { getActiveColleges } from "@/features/colleges/api";
import type { College } from "@/features/colleges/types";
import { useAuth } from "@/features/auth/authStore";
import { ROLES } from "@/lib/constants";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const DAY_LABELS: Record<string, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
};
const PALETTE = [
  "bg-gradient-to-br from-blue-50 to-sky-100/70 border-sky-200 border-l-brand-500 text-slate-800 dark:from-sky-950/40 dark:to-blue-950/30 dark:border-sky-800 dark:text-sky-100",
  "bg-gradient-to-br from-teal-50 to-cyan-100/60 border-teal-200 border-l-teal-500 text-slate-800 dark:from-teal-950/40 dark:to-cyan-950/30 dark:border-teal-800 dark:text-teal-100",
  "bg-gradient-to-br from-orange-50 to-amber-100/60 border-orange-200 border-l-orange-400 text-slate-800 dark:from-orange-950/40 dark:to-amber-950/30 dark:border-orange-800 dark:text-orange-100",
  "bg-gradient-to-br from-cyan-50 to-sky-100/60 border-cyan-200 border-l-cyan-500 text-slate-800 dark:from-cyan-950/40 dark:to-sky-950/30 dark:border-cyan-800 dark:text-cyan-100",
  "bg-gradient-to-br from-lime-50 to-emerald-100/50 border-lime-200 border-l-lime-500 text-slate-800 dark:from-lime-950/30 dark:to-emerald-950/30 dark:border-lime-800 dark:text-lime-100",
  "bg-gradient-to-br from-slate-50 to-blue-100/50 border-slate-200 border-l-slate-500 text-slate-800 dark:from-slate-800 dark:to-blue-950/30 dark:border-slate-700 dark:text-slate-100",
];

type Editor = {
  day: string;
  period: WeeklyPeriod;
  entry?: WeeklyEntry;
  subjectId: string;
  teacherId: string;
  room: string;
  lectureType: string;
  remarks: string;
};
type SaveState = "idle" | "saving" | "saved";

const color = (subjectId: number) => PALETTE[subjectId % PALETTE.length];
const academicYearRank = (label: string) => {
  const value = label.toLowerCase();
  if (/\b(first|1st)\b/.test(value)) return 1;
  if (/\b(second|2nd)\b/.test(value)) return 2;
  if (/\b(third|3rd)\b/.test(value)) return 3;
  if (/\b(fourth|4th)\b/.test(value)) return 4;
  if (/\b(fifth|5th)\b/.test(value)) return 5;
  return Number.MAX_SAFE_INTEGER;
};
const displayTime = (time: string) => {
  const [hours, minutes] = time.split(":").map(Number);
  return new Intl.DateTimeFormat("en-IN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(new Date(2000, 0, 1, hours, minutes));
};
const addMinutes = (time: string, minutes: number) => {
  const [hour, minute] = time.slice(0, 5).split(":").map(Number);
  const total = (hour * 60 + minute + minutes) % (24 * 60);
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
};
const toInput = (entry: WeeklyEntry): WeeklyEntryInput => ({
  dayOfWeek: entry.dayOfWeek,
  periodId: entry.periodId,
  subjectId: entry.subjectId,
  teacherId: entry.teacherId,
  room: entry.room,
  lectureType: entry.lectureType,
  remarks: entry.remarks,
});

export function TimetablePage() {
  const { isRole } = useAuth();
  const isSuperAdmin = isRole([ROLES.SUPER_ADMIN]);
  const isPrincipal = isRole([ROLES.PRINCIPAL]);
  const isHod = isRole([ROLES.HOD]);
  const [divisions, setDivisions] = useState<WeeklyDivision[]>([]);
  const [colleges, setColleges] = useState<College[]>([]);
  const [scope, setScope] = useState({ collegeId: "", departmentId: "", courseYearId: "" });
  const [sectionId, setSectionId] = useState("");
  const [table, setTable] = useState<WeeklyTimetable | null>(null);
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState<Editor | null>(null);
  const [timeEditor, setTimeEditor] = useState(false);
  const [times, setTimes] = useState<WeeklyPeriodInput[]>([]);
  const [mobileDay, setMobileDay] = useState("MONDAY");
  const [saving, setSaving] = useState(false);
  const [saveState, setSaveState] = useState<SaveState>("idle");
  const [query, setQuery] = useState("");
  const [sourceDay, setSourceDay] = useState("MONDAY");
  const [targetDay, setTargetDay] = useState("TUESDAY");
  const [sourceSectionId, setSourceSectionId] = useState("");
  const [copyBuffer, setCopyBuffer] = useState<WeeklyEntry | null>(null);
  const [past, setPast] = useState<WeeklyEntry[][]>([]);
  const [future, setFuture] = useState<WeeklyEntry[][]>([]);

  useEffect(() => {
    weeklyTimetableApi
      .divisions()
      .then((rows) => {
        setDivisions(rows);
        if (rows[0] && !isSuperAdmin) {
          setSectionId(String(rows[0].id));
          setScope({
            collegeId: String(rows[0].collegeId),
            departmentId: String(rows[0].departmentId),
            courseYearId: String(rows[0].courseYearId),
          });
        }
      })
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoading(false));
  }, [isSuperAdmin]);
  useEffect(() => {
    getActiveColleges()
      .then(setColleges)
      .catch(() => setColleges([]));
  }, []);

  useEffect(() => {
    if (!sectionId) return;
    setLoading(true);
    weeklyTimetableApi
      .get(Number(sectionId))
      .then((next) => {
        setTable(next);
        setTimes(next.periods);
        setPast([]);
        setFuture([]);
      })
      .catch((error) => {
        setTable(null);
        toast.error(handleApiError(error).message);
      })
      .finally(() => setLoading(false));
  }, [sectionId]);

  const entryMap = useMemo(
    () =>
      new Map(
        (table?.entries ?? []).map((entry) => [`${entry.dayOfWeek}:${entry.periodId}`, entry]),
      ),
    [table],
  );
  const normalizedQuery = query.trim().toLowerCase();
  const matchesSearch = (entry?: WeeklyEntry) =>
    !normalizedQuery ||
    Boolean(
      entry &&
        [entry.subject, entry.teacher, entry.room, entry.lectureType, table?.division]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalizedQuery)),
    );

  const setSavedSoon = () => {
    setSaveState("saved");
    window.setTimeout(() => setSaveState("idle"), 2200);
  };

  const mutate = async (operation: () => Promise<WeeklyTimetable>) => {
    if (!table) return;
    const snapshot = table.entries.map((entry) => ({ ...entry }));
    setSaving(true);
    setSaveState("saving");
    try {
      const next = await operation();
      setPast((items) => [...items.slice(-29), snapshot]);
      setFuture([]);
      setTable(next);
      setTimes(next.periods);
      setSavedSoon();
    } catch (error) {
      setSaveState("idle");
      toast.error(handleApiError(error).message);
      throw error;
    } finally {
      setSaving(false);
    }
  };

  const submitForReview = async () => {
    if (!table) return;
    setSaving(true);
    try {
      setTable(await weeklyTimetableApi.submitReview(table.id));
      toast.success("Timetable submitted to the Principal for approval");
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const reviewTimetable = async (action: "APPROVE" | "REQUEST_CHANGES" | "REJECT") => {
    if (!table) return;
    const comment =
      action === "APPROVE"
        ? undefined
        : window.prompt(
            action === "REJECT" ? "Enter rejection reason" : "Describe the required changes",
          );
    if (action !== "APPROVE" && !comment?.trim()) return;
    setSaving(true);
    try {
      setTable(await weeklyTimetableApi.review(table.id, action, comment?.trim()));
      toast.success(
        action === "APPROVE" ? "Timetable approved" : "Timetable returned to the HOD",
      );
    } catch (error) {
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const openEditor = (day: string, period: WeeklyPeriod, entry?: WeeklyEntry, subjectId = "") => {
    if (!table?.editable || period.kind !== "TEACHING") return;
    const template = entry ?? copyBuffer ?? undefined;
    setEditor({
      day,
      period,
      entry,
      subjectId: subjectId || String(template?.subjectId ?? ""),
      teacherId: String(template?.teacherId ?? ""),
      room: template?.room ?? "",
      lectureType: template?.lectureType ?? "THEORY",
      remarks: template?.remarks ?? "",
    });
  };

  const saveLecture = async () => {
    if (!editor || !table) return;
    if (!editor.subjectId || !editor.teacherId) {
      toast.error("Select a subject and teacher");
      return;
    }
    try {
      await mutate(async () => {
        await weeklyTimetableApi.save(table.id, editor.day, editor.period.id, {
          subjectId: Number(editor.subjectId),
          teacherId: Number(editor.teacherId),
          room: editor.room || null,
          lectureType: editor.lectureType,
          remarks: editor.remarks || null,
        });
        return weeklyTimetableApi.get(table.sectionId);
      });
      setEditor(null);
      setCopyBuffer(null);
      toast.success("Lecture saved");
    } catch {
      // The shared mutation handler displays the API validation message.
    }
  };

  const removeLecture = async () => {
    if (!editor?.entry || !table || !window.confirm("Delete this lecture from the timetable?"))
      return;
    try {
      await mutate(async () => {
        await weeklyTimetableApi.remove(table.id, editor.day, editor.period.id);
        return weeklyTimetableApi.get(table.sectionId);
      });
      setEditor(null);
      toast.success("Lecture removed");
    } catch {
      // Error already shown.
    }
  };

  const drop = async (day: string, period: WeeklyPeriod, raw: string) => {
    if (!table?.editable || period.kind !== "TEACHING" || !raw) return;
    try {
      const payload = JSON.parse(raw) as {
        type: string;
        subjectId?: number;
        day?: string;
        periodId?: number;
      };
      if (payload.type === "subject") {
        openEditor(day, period, undefined, String(payload.subjectId ?? ""));
      } else if (payload.type === "entry" && payload.day && payload.periodId) {
        await mutate(() =>
          weeklyTimetableApi.move(table.id, {
            fromDay: payload.day,
            fromPeriodId: payload.periodId,
            toDay: day,
            toPeriodId: period.id,
          }),
        );
        toast.success("Lecture moved");
      }
    } catch (error) {
      if (error instanceof SyntaxError) toast.error("Unsupported drag data");
    }
  };

  const restore = async (entries: WeeklyEntry[], direction: "undo" | "redo") => {
    if (!table) return;
    const current = table.entries.map((entry) => ({ ...entry }));
    setSaving(true);
    setSaveState("saving");
    try {
      const next = await weeklyTimetableApi.replaceEntries(table.id, entries.map(toInput));
      setTable(next);
      if (direction === "undo") {
        setPast((items) => items.slice(0, -1));
        setFuture((items) => [current, ...items]);
      } else {
        setFuture((items) => items.slice(1));
        setPast((items) => [...items, current]);
      }
      setSavedSoon();
      toast.success(direction === "undo" ? "Last change undone" : "Change restored");
    } catch (error) {
      setSaveState("idle");
      toast.error(handleApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const copyDay = async () => {
    if (!table) return;
    try {
      await mutate(() => weeklyTimetableApi.copyDay(table.id, sourceDay, targetDay, true));
      toast.success(`${DAY_LABELS[sourceDay]} copied to ${DAY_LABELS[targetDay]}`);
    } catch {
      // Error already shown.
    }
  };

  const copyDayToWeek = async () => {
    if (
      !table ||
      !window.confirm(
        `Copy ${DAY_LABELS[sourceDay]} to every other day? Existing lectures will be replaced.`,
      )
    )
      return;
    try {
      await mutate(async () => {
        let next = table;
        for (const day of DAYS.filter((item) => item !== sourceDay)) {
          next = await weeklyTimetableApi.copyDay(table.id, sourceDay, day, true);
        }
        return next;
      });
      toast.success(`${DAY_LABELS[sourceDay]} copied to the entire week`);
    } catch {
      // Error already shown.
    }
  };

  const copyOtherDivision = async () => {
    if (!table || !sourceSectionId) return;
    if (!window.confirm("Replace this timetable with the selected division's timetable?")) return;
    try {
      const source = await weeklyTimetableApi.get(Number(sourceSectionId));
      await mutate(() => weeklyTimetableApi.copyTimetable(table.id, source.id, true));
      toast.success("Division timetable copied");
    } catch {
      // Error already shown by the shared mutation handler.
    }
  };

  const addPeriod = () => {
    const start = times.at(-1)?.endTime?.slice(0, 5) || "08:30";
    setTimes([
      ...times,
      {
        label: `Period ${times.filter((period) => period.kind === "TEACHING").length + 1}`,
        startTime: start,
        endTime: addMinutes(start, 50),
        kind: "TEACHING",
      },
    ]);
  };

  if (loading && !divisions.length)
    return (
      <div className="page-container">
        <Loader label="Loading weekly timetable…" />
      </div>
    );

  return (
    <div className="timetable-print-page page-container min-w-0 space-y-6 print:p-0">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between print:hidden">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-600">
            <CalendarDays className="h-4 w-4" />
            Academic planning
          </div>
          <h1 className="page-title">Weekly timetable</h1>
          <p className="page-subtitle">
            {table?.editable
              ? "Build the fixed weekly schedule for each division."
              : "View the weekly schedule by college, department, year and division."}
          </p>
        </div>
        {table?.editable && (
          <div className="flex flex-wrap items-center gap-2">
            {saveState !== "idle" && (
              <span
                className={`inline-flex items-center gap-1 text-xs font-semibold ${saveState === "saved" ? "text-emerald-600" : "text-slate-500"}`}
              >
                {saveState === "saved" && <Check className="h-3.5 w-3.5" />}
                {saveState === "saving" ? "Saving…" : "Saved successfully"}
              </span>
            )}
            <Button
              variant="secondary"
              disabled={!past.length || saving}
              onClick={() => void restore(past.at(-1) ?? [], "undo")}
            >
              <Undo2 className="h-4 w-4" />
              Undo
            </Button>
            <Button
              variant="secondary"
              disabled={!future.length || saving}
              onClick={() => void restore(future[0], "redo")}
            >
              <Redo2 className="h-4 w-4" />
              Redo
            </Button>
            <Button variant="secondary" onClick={() => setTimeEditor(true)}>
              <Settings2 className="h-4 w-4" />
              Configure times
            </Button>
          </div>
        )}
      </div>

      <Card className="p-4 sm:p-5 print:border-0 print:shadow-none">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4 print:hidden">
          <Select
            label="College"
            value={scope.collegeId}
            onChange={(event) => {
              setScope({ collegeId: event.target.value, departmentId: "", courseYearId: "" });
              setSectionId("");
            }}
            options={[
              { label: "Select college", value: "" },
              ...colleges
                .filter((college) =>
                  divisions.some((division) => division.collegeId === college.id),
                )
                .map((college) => ({ label: college.name, value: college.id })),
            ]}
          />
          <Select
            label="Department"
            disabled={!scope.collegeId}
            value={scope.departmentId}
            onChange={(event) => {
              setScope({ ...scope, departmentId: event.target.value, courseYearId: "" });
              setSectionId("");
            }}
            options={[
              { label: "Select department", value: "" },
              ...Array.from(
                new Map(
                  divisions
                    .filter(
                      (division) =>
                        !scope.collegeId || division.collegeId === Number(scope.collegeId),
                    )
                    .map((division) => [division.departmentId, division.department]),
                ).entries(),
              ).map(([value, label]) => ({ value, label })),
            ]}
          />
          <Select
            label="Year / Class"
            disabled={!scope.collegeId || !scope.departmentId}
            value={scope.courseYearId}
            onChange={(event) => {
              setScope({ ...scope, courseYearId: event.target.value });
              setSectionId("");
            }}
            options={[
              { label: "Select year/class", value: "" },
              ...Array.from(
                new Map(
                  divisions
                    .filter(
                      (division) =>
                        (!scope.collegeId || division.collegeId === Number(scope.collegeId)) &&
                        (!scope.departmentId ||
                          division.departmentId === Number(scope.departmentId)),
                    )
                    .map((division) => [division.courseYearId, division.year]),
                ).entries(),
              )
                .sort(([, firstLabel], [, secondLabel]) => {
                  const rankDifference =
                    academicYearRank(firstLabel) - academicYearRank(secondLabel);
                  return rankDifference || firstLabel.localeCompare(secondLabel);
                })
                .map(([value, label]) => ({ value, label })),
            ]}
          />
          <Select
            label="Division"
            disabled={!scope.collegeId || !scope.departmentId || !scope.courseYearId}
            value={sectionId}
            onChange={(event) => setSectionId(event.target.value)}
            options={[
              { label: "Select division", value: "" },
              ...divisions
                .filter(
                  (division) =>
                    (!scope.collegeId || division.collegeId === Number(scope.collegeId)) &&
                    (!scope.departmentId || division.departmentId === Number(scope.departmentId)) &&
                    (!scope.courseYearId || division.courseYearId === Number(scope.courseYearId)),
                )
                .map((division) => ({ value: String(division.id), label: division.division })),
            ]}
          />
        </div>
        {table && (
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 print:mt-0 print:grid-cols-7 print:gap-1">
            {[
              ["College", table.college],
              ["Department", table.department],
              ["Year", table.year],
              ["Division", table.division],
              ["Class Teacher", table.classTeacher],
              ["Academic Year", table.academicYear],
              ["Status", table.status],
            ].map(([key, value]) => (
              <div
                key={key}
                className="min-w-0 rounded-xl border border-sky-100 bg-sky-50/70 p-3 dark:border-slate-700 dark:bg-slate-800 print:rounded-none print:border print:bg-white print:p-2"
              >
                <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
                  {key}
                </p>
                <p className="mt-1 truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
                  {value}
                </p>
              </div>
            ))}
            {table.reviewComment && (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800 sm:col-span-2 lg:col-span-4 xl:col-span-7 print:hidden">
                <b>Principal review:</b> {table.reviewComment}
              </div>
            )}
          </div>
        )}
      </Card>

      {table && (
        <>
          {table.editable && (
            <Card className="space-y-4 p-4 sm:p-5 print:hidden">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div className="flex items-center gap-2">
                  <BookOpen className="h-4 w-4 text-brand-600" />
                  <h2 className="font-semibold">Subjects</h2>
                  <span className="text-xs text-slate-400">Drag into a teaching period</span>
                </div>
                <div className="w-full lg:w-80">
                  <Input
                    aria-label="Search timetable"
                    placeholder="Search teacher, subject, room…"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    icon={<Search className="h-4 w-4" />}
                  />
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {table.subjects.map((subject) => (
                  <button
                    key={subject.id}
                    draggable={table.editable}
                    onDragStart={(event) =>
                      event.dataTransfer.setData(
                        "application/json",
                        JSON.stringify({ type: "subject", subjectId: subject.id }),
                      )
                    }
                    className={`inline-flex max-w-full items-center gap-1 rounded-xl border px-3 py-2 text-left text-xs font-semibold transition hover:-translate-y-0.5 ${color(subject.id)}`}
                  >
                    <GripVertical className="h-3.5 w-3.5 shrink-0" />
                    <span className="truncate">{subject.label}</span>
                  </button>
                ))}
              </div>
            </Card>
          )}

          {table.editable && (
            <Card className="grid gap-3 p-4 print:hidden xl:grid-cols-[1fr_1fr_auto_auto]">
              <Select
                label="Copy from day"
                value={sourceDay}
                onChange={(event) => setSourceDay(event.target.value)}
                options={DAYS.map((day) => ({ label: DAY_LABELS[day], value: day }))}
              />
              <Select
                label="Copy to day"
                value={targetDay}
                onChange={(event) => setTargetDay(event.target.value)}
                options={DAYS.map((day) => ({ label: DAY_LABELS[day], value: day }))}
              />
              <Button
                className="self-end"
                variant="secondary"
                disabled={saving || sourceDay === targetDay}
                onClick={() => void copyDay()}
              >
                <ClipboardCopy className="h-4 w-4" />
                Copy day
              </Button>
              <Button
                className="self-end"
                variant="secondary"
                disabled={saving}
                onClick={() => void copyDayToWeek()}
              >
                <ClipboardCopy className="h-4 w-4" />
                Copy to week
              </Button>
              <div className="xl:col-span-3">
                <Select
                  label="Copy previous division"
                  value={sourceSectionId}
                  onChange={(event) => setSourceSectionId(event.target.value)}
                  options={[
                    { label: "Select source division", value: "" },
                    ...divisions
                      .filter(
                        (division) =>
                          String(division.id) !== sectionId &&
                          division.department === table.department &&
                          division.year === table.year,
                      )
                      .map((division) => ({
                        label: `${division.department} · ${division.year} · ${division.division}`,
                        value: String(division.id),
                      })),
                  ]}
                />
              </div>
              <Button
                className="self-end"
                variant="secondary"
                disabled={!sourceSectionId || saving}
                onClick={() => void copyOtherDivision()}
              >
                <ClipboardCopy className="h-4 w-4" />
                Copy timetable
              </Button>
            </Card>
          )}

          <div className="flex flex-wrap justify-end gap-2 print:hidden">
            {isHod && table.editable && (
              <Button
                disabled={saving || !table.entries.length}
                onClick={() => void submitForReview()}
              >
                <Check className="h-4 w-4" />
                Submit to Principal
              </Button>
            )}
            {isPrincipal && table.status === "SUBMITTED" && (
              <>
                <Button disabled={saving} onClick={() => void reviewTimetable("APPROVE")}>
                  <Check className="h-4 w-4" />
                  Approve timetable
                </Button>
                <Button
                  variant="secondary"
                  disabled={saving}
                  onClick={() => void reviewTimetable("REQUEST_CHANGES")}
                >
                  Request changes
                </Button>
                <Button
                  variant="secondary"
                  disabled={saving}
                  onClick={() => void reviewTimetable("REJECT")}
                >
                  Reject
                </Button>
              </>
            )}
            <Button variant="secondary" onClick={() => window.print()}>
              <Printer className="h-4 w-4" />
              Print
            </Button>
            <Button variant="secondary" onClick={() => void exportWeeklyTimetablePdf(table)}>
              <FileDown className="h-4 w-4" />
              Download PDF
            </Button>
            <Button variant="secondary" onClick={() => void exportWeeklyTimetableExcel(table)}>
              <Sheet className="h-4 w-4" />
              Download Excel
            </Button>
          </div>

          <DesktopGrid
            table={table}
            entryMap={entryMap}
            matchesSearch={matchesSearch}
            onOpen={openEditor}
            onDrop={drop}
          />
          <MobileGrid
            table={table}
            day={mobileDay}
            setDay={setMobileDay}
            entryMap={entryMap}
            matchesSearch={matchesSearch}
            onOpen={openEditor}
            onDrop={drop}
          />
        </>
      )}

      {isSuperAdmin && divisions.length > 0 && !table && !loading && (
        <Card className="p-10 text-center">
          <h2 className="font-semibold">Select a division to view its timetable</h2>
          <p className="mt-2 text-sm text-slate-500">
            Choose college, department, year/class and division from the filters above.
          </p>
        </Card>
      )}

      {!divisions.length && (
        <Card className="p-10 text-center">
          <h2 className="font-semibold">No assigned divisions</h2>
          <p className="mt-2 text-sm text-slate-500">
            Class teachers only see divisions assigned to their account.
          </p>
        </Card>
      )}

      {editor && table && (
        <Modal
          title={editor.entry ? "Edit lecture" : copyBuffer ? "Duplicate lecture" : "Add lecture"}
          onClose={() => setEditor(null)}
        >
          <div className="mb-4 rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <b>{DAY_LABELS[editor.day]}</b> · {editor.period.label} ·{" "}
            {displayTime(editor.period.startTime)}–{displayTime(editor.period.endTime)}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Select
              label="Subject"
              value={editor.subjectId}
              onChange={(event) =>
                setEditor({ ...editor, subjectId: event.target.value, teacherId: "" })
              }
              options={[
                { label: "Select subject", value: "" },
                ...table.subjects.map((option) => ({
                  label: option.label,
                  value: String(option.id),
                })),
              ]}
            />
            <Select
              label="Teacher"
              value={editor.teacherId}
              onChange={(event) => setEditor({ ...editor, teacherId: event.target.value })}
              options={[
                { label: "Select teacher", value: "" },
                ...(
                  table.subjectTeachers.find((item) => item.subjectId === Number(editor.subjectId))
                    ?.teachers ?? []
                ).map((option) => ({ label: option.label, value: String(option.id) })),
              ]}
            />
            <Input
              label="Room (optional)"
              list="weekly-rooms"
              value={editor.room}
              onChange={(event) => setEditor({ ...editor, room: event.target.value })}
            />
            <datalist id="weekly-rooms">
              {table.rooms.map((room) => (
                <option key={room} value={room} />
              ))}
            </datalist>
            <Select
              label="Lecture type"
              value={editor.lectureType}
              onChange={(event) => setEditor({ ...editor, lectureType: event.target.value })}
              options={["THEORY", "PRACTICAL", "LAB", "TUTORIAL"].map((value) => ({
                label: value[0] + value.slice(1).toLowerCase(),
                value,
              }))}
            />
            <div className="sm:col-span-2">
              <Input
                label="Remarks (optional)"
                value={editor.remarks}
                onChange={(event) => setEditor({ ...editor, remarks: event.target.value })}
              />
            </div>
          </div>
          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-between">
            <div className="flex gap-2">
              {editor.entry && (
                <>
                  <Button variant="danger" disabled={saving} onClick={() => void removeLecture()}>
                    <Trash2 className="h-4 w-4" />
                    Delete
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setCopyBuffer(editor.entry ?? null);
                      setEditor(null);
                      toast.success("Lecture copied. Select an empty cell to duplicate it.");
                    }}
                  >
                    <ClipboardCopy className="h-4 w-4" />
                    Copy
                  </Button>
                </>
              )}
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setEditor(null)}>
                Cancel
              </Button>
              <Button loading={saving} onClick={() => void saveLecture()}>
                Save lecture
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {timeEditor && table && (
        <Modal title="Configure period times" onClose={() => setTimeEditor(false)}>
          <p className="mb-4 text-sm text-slate-500">
            Add, remove, rename, or change timetable rows. Break rows cannot contain lectures.
          </p>
          <div className="max-h-[58vh] space-y-3 overflow-y-auto pr-1">
            {times.map((period, index) => (
              <div key={period.id ?? `new-${index}`} className="rounded-xl bg-slate-50 p-3">
                <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_140px_130px_130px_auto]">
                  <Input
                    aria-label="Period name"
                    value={period.label}
                    onChange={(event) =>
                      setTimes(
                        times.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, label: event.target.value } : item,
                        ),
                      )
                    }
                  />
                  <Select
                    aria-label="Period type"
                    value={period.kind}
                    onChange={(event) =>
                      setTimes(
                        times.map((item, itemIndex) =>
                          itemIndex === index
                            ? { ...item, kind: event.target.value as WeeklyPeriodInput["kind"] }
                            : item,
                        ),
                      )
                    }
                    options={[
                      { label: "Teaching", value: "TEACHING" },
                      { label: "Short break", value: "SHORT_BREAK" },
                      { label: "Lunch break", value: "LUNCH_BREAK" },
                    ]}
                  />
                  <Input
                    aria-label="Start time"
                    type="time"
                    value={period.startTime.slice(0, 5)}
                    onChange={(event) =>
                      setTimes(
                        times.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, startTime: event.target.value } : item,
                        ),
                      )
                    }
                  />
                  <Input
                    aria-label="End time"
                    type="time"
                    value={period.endTime.slice(0, 5)}
                    onChange={(event) =>
                      setTimes(
                        times.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, endTime: event.target.value } : item,
                        ),
                      )
                    }
                  />
                  <Button
                    aria-label={`Remove ${period.label}`}
                    variant="secondary"
                    className="text-rose-600"
                    disabled={times.length === 1}
                    onClick={() => setTimes(times.filter((_, itemIndex) => itemIndex !== index))}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
          <Button variant="secondary" className="mt-4" onClick={addPeriod}>
            <Plus className="h-4 w-4" />
            Add period
          </Button>
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setTimeEditor(false)}>
              Cancel
            </Button>
            <Button
              loading={saving}
              onClick={async () => {
                setSaving(true);
                setSaveState("saving");
                try {
                  const next = await weeklyTimetableApi.updatePeriods(
                    table.id,
                    times.map((period) => ({
                      ...period,
                      startTime: period.startTime.slice(0, 5),
                      endTime: period.endTime.slice(0, 5),
                    })),
                  );
                  setTable(next);
                  setTimes(next.periods);
                  setTimeEditor(false);
                  setSavedSoon();
                  toast.success("Period times updated");
                } catch (error) {
                  setSaveState("idle");
                  toast.error(handleApiError(error).message);
                } finally {
                  setSaving(false);
                }
              }}
            >
              Save times
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}

type GridProps = {
  table: WeeklyTimetable;
  entryMap: Map<string, WeeklyEntry>;
  matchesSearch: (entry?: WeeklyEntry) => boolean;
  onOpen: (day: string, period: WeeklyPeriod, entry?: WeeklyEntry) => void;
  onDrop: (day: string, period: WeeklyPeriod, data: string) => void;
};

function DesktopGrid({ table, entryMap, matchesSearch, onOpen, onDrop }: GridProps) {
  return (
    <div className="timetable-print-sheet hidden overflow-x-auto rounded-2xl border bg-white shadow-sm print:block print:overflow-visible print:rounded-none print:shadow-none xl:block">
      <div className="min-w-[1120px] print:min-w-0">
        <div className="sticky top-0 z-20 grid grid-cols-[150px_repeat(6,minmax(155px,1fr))] border-b border-sky-200 bg-gradient-to-r from-brand-50 via-sky-50 to-cyan-50 text-center text-xs font-bold uppercase tracking-wide text-brand-700 print:static print:grid-cols-[28mm_repeat(6,minmax(0,1fr))] print:text-[8px]">
          <div className="sticky left-0 z-30 bg-gradient-to-r from-brand-50 to-sky-50 p-3 text-left">
            Period
          </div>
          {DAYS.map((day) => (
            <div className="border-l p-3" key={day}>
              {DAY_LABELS[day]}
            </div>
          ))}
        </div>
        {table.periods.map((period) => (
          <div
            key={period.id}
            className={`grid grid-cols-[150px_repeat(6,minmax(155px,1fr))] border-t print:grid-cols-[28mm_repeat(6,minmax(0,1fr))] ${period.kind !== "TEACHING" ? "bg-orange-50/70" : ""}`}
          >
            <div className="sticky left-0 z-10 flex min-w-0 flex-col justify-center bg-white p-3">
              <PeriodLabel period={period} />
            </div>
            {period.kind !== "TEACHING" ? (
              <div className="col-span-6 flex items-center justify-center border-l border-orange-200 p-4 text-xs font-bold tracking-[.2em] text-orange-700">
                {period.kind === "SHORT_BREAK" ? "SHORT BREAK" : "LUNCH BREAK"}
              </div>
            ) : (
              DAYS.map((day) => (
                <Cell
                  key={day}
                  day={day}
                  period={period}
                  entry={entryMap.get(`${day}:${period.id}`)}
                  editable={table.editable}
                  highlighted={matchesSearch(entryMap.get(`${day}:${period.id}`))}
                  onOpen={onOpen}
                  onDrop={onDrop}
                />
              ))
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function MobileGrid({
  table,
  day,
  setDay,
  entryMap,
  matchesSearch,
  onOpen,
  onDrop,
}: GridProps & { day: string; setDay: (day: string) => void }) {
  return (
    <div className="xl:hidden print:hidden">
      <div className="mb-3 grid grid-cols-3 gap-2 sm:grid-cols-6">
        {DAYS.map((item) => (
          <button
            key={item}
            onClick={() => setDay(item)}
            className={`rounded-xl px-2 py-2 text-xs font-semibold transition ${day === item ? "bg-gradient-to-r from-brand-600 to-sky-500 text-white shadow-sm" : "border border-slate-200 bg-white text-slate-600 hover:border-sky-200 hover:bg-sky-50"}`}
          >
            {DAY_LABELS[item].slice(0, 3)}
          </button>
        ))}
      </div>
      <Card className="divide-y overflow-hidden">
        {table.periods.map((period) => (
          <div key={period.id} className={period.kind !== "TEACHING" ? "bg-orange-50" : "p-3"}>
            {period.kind !== "TEACHING" ? (
              <div className="p-4 text-center text-xs font-bold tracking-widest text-orange-700">
                {period.label} · {displayTime(period.startTime)}–{displayTime(period.endTime)}
              </div>
            ) : (
              <div className="grid min-w-0 grid-cols-[100px_1fr] gap-3">
                <PeriodLabel period={period} />
                <Cell
                  day={day}
                  period={period}
                  entry={entryMap.get(`${day}:${period.id}`)}
                  editable={table.editable}
                  highlighted={matchesSearch(entryMap.get(`${day}:${period.id}`))}
                  onOpen={onOpen}
                  onDrop={onDrop}
                />
              </div>
            )}
          </div>
        ))}
      </Card>
    </div>
  );
}

function PeriodLabel({ period }: { period: WeeklyPeriod }) {
  return (
    <>
      <b className="text-sm text-slate-800">{period.label}</b>
      <span className="mt-1 text-[10px] text-slate-400">
        {displayTime(period.startTime)}–{displayTime(period.endTime)}
      </span>
    </>
  );
}

function Cell({
  day,
  period,
  entry,
  editable,
  highlighted,
  onOpen,
  onDrop,
}: {
  day: string;
  period: WeeklyPeriod;
  entry?: WeeklyEntry;
  editable: boolean;
  highlighted: boolean;
  onOpen: GridProps["onOpen"];
  onDrop: GridProps["onDrop"];
}) {
  return (
    <button
      disabled={!editable}
      draggable={Boolean(entry && editable)}
      title={
        entry
          ? `${entry.subject}\n${entry.teacher}\n${entry.room ?? "No room"}\n${entry.lectureType}${entry.remarks ? `\n${entry.remarks}` : ""}`
          : undefined
      }
      onDragStart={(event) =>
        entry &&
        event.dataTransfer.setData(
          "application/json",
          JSON.stringify({ type: "entry", day, periodId: period.id }),
        )
      }
      onDragOver={(event) => {
        if (editable) event.preventDefault();
      }}
      onDrop={(event) => {
        event.preventDefault();
        void onDrop(day, period, event.dataTransfer.getData("application/json"));
      }}
      onClick={() => onOpen(day, period, entry)}
      className={`group min-h-24 min-w-0 rounded-xl border bg-slate-50/60 p-1.5 text-left transition hover:border-brand-200 hover:bg-brand-50/50 disabled:cursor-default xl:rounded-none xl:border-y-0 xl:border-r-0 xl:border-l xl:bg-white xl:p-2 print:min-h-0 print:rounded-none print:border-l print:bg-white print:p-1 ${highlighted ? "opacity-100" : "opacity-25"}`}
    >
      {entry ? (
        <div
          className={`h-full min-w-0 rounded-xl border border-l-4 p-3 shadow-sm transition duration-200 group-hover:-translate-y-0.5 group-hover:shadow-md print:rounded-none print:border-l print:p-1 print:shadow-none ${color(entry.subjectId)}`}
        >
          <div className="flex items-start justify-between gap-1">
            <b className="break-words text-xs leading-5">{entry.subject}</b>
            {editable && <Pencil className="h-3 w-3 shrink-0 opacity-50" />}
          </div>
          <p className="mt-1 break-words text-[11px] leading-4 opacity-75">{entry.teacher}</p>
          <p className="mt-2 text-[10px] font-bold uppercase tracking-wide opacity-80">
            {entry.lectureType}
            {entry.room ? ` · ${entry.room}` : ""}
          </p>
        </div>
      ) : editable ? (
        <div className="grid h-full min-h-20 place-items-center rounded-xl border border-dashed border-slate-200 text-slate-300 transition group-hover:border-brand-300 group-hover:text-brand-500">
          <Plus className="h-5 w-5" />
        </div>
      ) : (
        <span className="text-xs text-slate-300">—</span>
      )}
    </button>
  );
}

function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 p-3 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="max-h-[94vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white shadow-2xl dark:bg-slate-900">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b bg-white px-5 py-4 dark:bg-slate-900">
          <h2 className="font-bold text-slate-900 dark:text-white">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}
