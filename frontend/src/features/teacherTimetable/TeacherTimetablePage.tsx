import { useEffect, useMemo, useState } from "react";
import { CalendarDays, Clock3, Grid3X3, List, Search } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/common/Input";
import { Loader } from "@/components/common/Loader";
import { Select } from "@/components/common/Select";
import { handleApiError } from "@/lib/handleApiError";
import {
  teacherTimetableApi,
  type TeacherDay,
  type TeacherLecture,
  type TeacherTimetable,
} from "./api";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const LABELS: Record<string, string> = Object.fromEntries(
  DAYS.map((day) => [day, day[0] + day.slice(1).toLowerCase()]),
);
const COLORS = [
  "border-blue-200 bg-blue-50 text-blue-950 dark:border-blue-800 dark:bg-blue-950/40 dark:text-blue-100",
  "border-emerald-200 bg-emerald-50 text-emerald-950 dark:border-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-100",
  "border-orange-200 bg-orange-50 text-orange-950 dark:border-orange-800 dark:bg-orange-950/40 dark:text-orange-100",
  "border-violet-200 bg-violet-50 text-violet-950 dark:border-violet-800 dark:bg-violet-950/40 dark:text-violet-100",
  "border-cyan-200 bg-cyan-50 text-cyan-950 dark:border-cyan-800 dark:bg-cyan-950/40 dark:text-cyan-100",
  "border-rose-200 bg-rose-50 text-rose-950 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-100",
];

const minutes = (value: string) => {
  const [hours, mins] = value.slice(0, 5).split(":").map(Number);
  return hours * 60 + mins;
};
const time = (value: string) => {
  const [hours, mins] = value.split(":").map(Number);
  return new Intl.DateTimeFormat("en-IN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(new Date(2000, 0, 1, hours, mins));
};

export function TeacherTimetablePage() {
  const [table, setTable] = useState<TeacherTimetable | null>(null);
  const [dayData, setDayData] = useState<TeacherDay | null>(null);
  const [loading, setLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(false);
  const [view, setView] = useState<"weekly" | "daily">("weekly");
  const [selectedDay, setSelectedDay] = useState("");
  const [query, setQuery] = useState("");

  useEffect(() => {
    teacherTimetableApi
      .get()
      .then((response) => {
        setTable(response);
        setSelectedDay(DAYS.includes(response.currentDay) ? response.currentDay : "MONDAY");
      })
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (view !== "daily" || !selectedDay) return;
    setDayLoading(true);
    teacherTimetableApi
      .day(selectedDay)
      .then(setDayData)
      .catch((error) => toast.error(handleApiError(error).message))
      .finally(() => setDayLoading(false));
  }, [selectedDay, view]);

  const normalized = query.trim().toLowerCase();
  const matches = (lecture?: TeacherLecture) =>
    !normalized ||
    Boolean(
      lecture &&
        [lecture.subject, lecture.department, lecture.year, lecture.division].some((value) =>
          value.toLowerCase().includes(normalized),
        ),
    );
  const lectureMap = useMemo(
    () =>
      new Map(
        (table?.lectures ?? []).map((lecture) => [
          `${lecture.dayOfWeek}:${lecture.periodKey}`,
          lecture,
        ]),
      ),
    [table],
  );

  if (loading)
    return (
      <div className="page-container">
        <Loader label="Loading your timetable…" />
      </div>
    );
  if (!table) return null;

  return (
    <div className="page-container min-w-0 space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-brand-600">
            <CalendarDays className="h-4 w-4" />
            Teacher workspace
          </div>
          <h1 className="page-title">My Timetable</h1>
          <p className="page-subtitle">Your read-only weekly teaching schedule.</p>
        </div>
        <div className="flex rounded-xl border bg-white p-1 shadow-sm dark:bg-slate-900">
          <Button
            variant={view === "weekly" ? "primary" : "secondary"}
            onClick={() => setView("weekly")}
          >
            <Grid3X3 className="h-4 w-4" />
            Weekly Grid
          </Button>
          <Button
            variant={view === "daily" ? "primary" : "secondary"}
            onClick={() => setView("daily")}
          >
            <List className="h-4 w-4" />
            Daily View
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Teacher Name" value={table.teacherName} />
        <Stat label="Employee ID" value={table.employeeId} />
        <Stat label="Total Weekly Lectures" value={String(table.totalWeeklyLectures)} />
        <Stat label="Today's Lectures" value={String(table.todayLectureCount)} />
      </div>

      <Card className="p-4">
        <Input
          placeholder="Search subject, department, year, or division…"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          icon={<Search className="h-4 w-4" />}
        />
      </Card>

      {table.lectures.length === 0 ? (
        <Card>
          <EmptyState
            title="No timetable assigned"
            description="No timetable has been assigned yet."
          />
        </Card>
      ) : view === "weekly" ? (
        <WeeklyGrid table={table} lectureMap={lectureMap} matches={matches} />
      ) : (
        <div className="space-y-4">
          <Card className="p-4">
            <Select
              label="Day"
              value={selectedDay}
              onChange={(event) => setSelectedDay(event.target.value)}
              options={DAYS.map((day) => ({ label: LABELS[day], value: day }))}
            />
          </Card>
          {dayLoading || !dayData ? (
            <Loader label="Loading day schedule…" />
          ) : (
            <DayView data={dayData} matches={matches} currentDay={table.currentDay} />
          )}
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Card className="p-5">
      <p className="text-xs font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-2 truncate text-xl font-bold text-slate-900 dark:text-white">{value}</p>
    </Card>
  );
}

function WeeklyGrid({
  table,
  lectureMap,
  matches,
}: {
  table: TeacherTimetable;
  lectureMap: Map<string, TeacherLecture>;
  matches: (lecture?: TeacherLecture) => boolean;
}) {
  return (
    <>
      <div className="hidden overflow-x-auto rounded-2xl border bg-white shadow-sm dark:bg-slate-900 md:block">
        <div className="min-w-[1100px]">
          <div className="sticky top-0 z-20 grid grid-cols-[145px_repeat(6,minmax(150px,1fr))] bg-slate-100 text-center text-xs font-bold uppercase tracking-wide text-slate-500 dark:bg-slate-800">
            <div className="sticky left-0 z-30 bg-slate-100 p-3 text-left dark:bg-slate-800">
              Period
            </div>
            {DAYS.map((day) => (
              <div
                key={day}
                className={`border-l p-3 ${day === table.currentDay ? "bg-brand-100 text-brand-700 dark:bg-brand-950" : ""}`}
              >
                {LABELS[day]}
                {day === table.currentDay && <span className="ml-1 text-[9px]">TODAY</span>}
              </div>
            ))}
          </div>
          {table.periods.map((period) => (
            <div
              key={period.key}
              className={`grid grid-cols-[145px_repeat(6,minmax(150px,1fr))] border-t ${period.kind !== "TEACHING" ? "bg-amber-50/70 dark:bg-amber-950/20" : ""}`}
            >
              <div className="sticky left-0 z-10 flex flex-col justify-center bg-white p-3 dark:bg-slate-900">
                <b className="text-sm">{period.label}</b>
                <span className="mt-1 text-[10px] text-slate-400">
                  {time(period.startTime)}–{time(period.endTime)}
                </span>
              </div>
              {period.kind !== "TEACHING" ? (
                <div className="col-span-6 border-l p-4 text-center text-xs font-bold tracking-[.18em] text-amber-700">
                  {period.label.toUpperCase()}
                </div>
              ) : (
                DAYS.map((day) => {
                  const lecture = lectureMap.get(`${day}:${period.key}`);
                  return (
                    <LectureCell
                      key={day}
                      lecture={lecture}
                      highlighted={matches(lecture)}
                      currentDay={table.currentDay}
                    />
                  );
                })
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="md:hidden">
        <DayView
          data={{
            day: table.currentDay,
            periods: table.periods,
            lectures: table.lectures.filter((lecture) => lecture.dayOfWeek === table.currentDay),
          }}
          matches={matches}
          currentDay={table.currentDay}
        />
      </div>
    </>
  );
}

function DayView({
  data,
  matches,
  currentDay,
}: {
  data: TeacherDay;
  matches: (lecture?: TeacherLecture) => boolean;
  currentDay: string;
}) {
  const lectureMap = new Map(data.lectures.map((lecture) => [lecture.periodKey, lecture]));
  return (
    <Card className="divide-y overflow-hidden">
      <div className="bg-slate-50 px-5 py-4 dark:bg-slate-800">
        <h2 className="font-bold">{LABELS[data.day]}</h2>
        <p className="text-xs text-slate-400">
          {data.lectures.length} scheduled lecture{data.lectures.length === 1 ? "" : "s"}
        </p>
      </div>
      {data.lectures.length === 0 ? (
        <EmptyState title="No lectures" description="No lectures scheduled for this day." />
      ) : (
        data.periods.map((period) =>
          period.kind !== "TEACHING" ? (
            <div
              key={period.key}
              className="bg-amber-50 px-5 py-4 text-center text-xs font-bold tracking-widest text-amber-700 dark:bg-amber-950/20"
            >
              {period.label.toUpperCase()} · {time(period.startTime)}–{time(period.endTime)}
            </div>
          ) : (
            <div key={period.key} className="grid grid-cols-[110px_1fr] gap-4 p-4">
              <div className="flex flex-col justify-center">
                <b className="text-sm">{period.label}</b>
                <span className="text-[10px] text-slate-400">
                  {time(period.startTime)}–{time(period.endTime)}
                </span>
              </div>
              <LectureCard
                lecture={lectureMap.get(period.key)}
                highlighted={matches(lectureMap.get(period.key))}
                currentDay={currentDay}
              />
            </div>
          ),
        )
      )}
    </Card>
  );
}

function LectureCell({
  lecture,
  highlighted,
  currentDay,
}: {
  lecture?: TeacherLecture;
  highlighted: boolean;
  currentDay: string;
}) {
  return (
    <div
      className={`min-h-24 border-l p-2 ${lecture?.dayOfWeek === currentDay ? "bg-brand-50/30" : ""}`}
    >
      <LectureCard lecture={lecture} highlighted={highlighted} currentDay={currentDay} />
    </div>
  );
}

function LectureCard({
  lecture,
  highlighted,
  currentDay,
}: {
  lecture?: TeacherLecture;
  highlighted: boolean;
  currentDay: string;
}) {
  if (!lecture)
    return <div className="grid h-full min-h-20 place-items-center text-sm text-slate-300">—</div>;
  const now = new Date();
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const isToday = lecture.dayOfWeek === currentDay;
  const current =
    isToday && nowMinutes >= minutes(lecture.startTime) && nowMinutes < minutes(lecture.endTime);
  const past = isToday && nowMinutes >= minutes(lecture.endTime);
  const title = [
    `Subject: ${lecture.subject}`,
    `Department: ${lecture.department}`,
    `Year: ${lecture.year}`,
    `Division: ${lecture.division}`,
    `Lecture type: ${lecture.lectureType}`,
    `Start: ${time(lecture.startTime)}`,
    `End: ${time(lecture.endTime)}`,
    lecture.remarks && `Remarks: ${lecture.remarks}`,
  ]
    .filter(Boolean)
    .join("\n");
  return (
    <div
      title={title}
      className={`h-full min-h-20 rounded-xl border p-3 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:shadow-md ${COLORS[lecture.subjectId % COLORS.length]} ${past ? "opacity-55" : ""} ${highlighted ? "" : "opacity-20"} ${current ? "ring-2 ring-brand-500 ring-offset-2" : ""}`}
    >
      <div className="flex items-start justify-between gap-2">
        <b className="break-words text-xs leading-5">{lecture.subject}</b>
        {current && (
          <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[8px] font-bold text-white">
            NOW
          </span>
        )}
      </div>
      <p className="mt-1 text-[10px] opacity-75">
        {lecture.year} - {lecture.division}
      </p>
      <p className="mt-1 text-[10px] opacity-75">{lecture.department}</p>
      <p className="mt-2 flex items-center gap-1 text-[10px] font-semibold">
        <Clock3 className="h-3 w-3" />
        {time(lecture.startTime)}–{time(lecture.endTime)} · {lecture.lectureType}
      </p>
    </div>
  );
}

export {
  Stat as TeacherTimetableStat,
  WeeklyGrid as TeacherWeeklyGrid,
  DayView as TeacherDayView,
};
