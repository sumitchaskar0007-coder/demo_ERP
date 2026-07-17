import { useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Clock3 } from "lucide-react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Card } from "@/components/common/Card";
import { Loader } from "@/components/common/Loader";
import { handleApiError } from "@/lib/handleApiError";
import { ROUTES } from "@/lib/constants";
import { teacherTimetableApi, type NextLecture, type TeacherDay } from "./api";

const time = (value: string) => value.slice(0, 5);
const duration = (minutes?: number | null) => {
  if (minutes == null) return "No upcoming lecture";
  if (minutes < 60) return `Starts in ${minutes} minute${minutes === 1 ? "" : "s"}`;
  if (minutes < 24 * 60) return `Starts in ${Math.floor(minutes / 60)}h ${minutes % 60}m`;
  return `Starts in ${Math.floor(minutes / (24 * 60))} day${minutes < 48 * 60 ? "" : "s"}`;
};

export function TeacherDashboardTimetable() {
  const [today, setToday] = useState<TeacherDay | null>(null);
  const [next, setNext] = useState<NextLecture | null>(null);
  useEffect(() => {
    Promise.all([teacherTimetableApi.today(), teacherTimetableApi.next()])
      .then(([todayResponse, nextResponse]) => {
        setToday(todayResponse);
        setNext(nextResponse);
      })
      .catch((error) => toast.error(handleApiError(error).message));
  }, []);
  if (!today || !next) return <Loader label="Loading today's lectures…" />;
  const dayLabel = today.day[0] + today.day.slice(1).toLowerCase();
  return (
    <div className="mt-6 grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b px-6 py-5">
          <div>
            <h2 className="font-bold">Today's Lectures ({dayLabel})</h2>
            <p className="mt-1 text-xs text-slate-500">Your teaching schedule for today</p>
          </div>
          <Link
            to={ROUTES.teacherTimetable}
            className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600"
          >
            Full timetable <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <div className="divide-y">
          {today.lectures.length === 0 ? (
            <p className="px-6 py-10 text-center text-sm text-slate-500">
              No lectures scheduled for today.
            </p>
          ) : (
            today.periods.map((period) => {
              const lecture = today.lectures.find((item) => item.periodKey === period.key);
              if (period.kind !== "TEACHING")
                return (
                  <div
                    key={period.key}
                    className="bg-amber-50 px-6 py-3 text-center text-xs font-bold tracking-widest text-amber-700"
                  >
                    {period.label.toUpperCase()}
                  </div>
                );
              if (!lecture) return null;
              return (
                <div key={period.key} className="flex gap-4 px-6 py-4">
                  <div className="w-28 shrink-0 text-xs font-semibold text-slate-500">
                    {time(period.startTime)} – {time(period.endTime)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-900">{lecture.subject}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {lecture.year} - {lecture.division} · {lecture.lectureType}
                    </p>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </Card>
      <Card className="relative overflow-hidden p-6">
        <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-brand-100/60" />
        <div className="relative">
          <div className="flex items-center gap-2 text-brand-600">
            <CalendarDays className="h-5 w-5" />
            <h2 className="font-bold">Next Lecture</h2>
          </div>
          {next.lecture ? (
            <>
              <p className="mt-7 text-xl font-bold text-slate-900">{next.lecture.subject}</p>
              <p className="mt-2 text-sm text-slate-500">
                {next.lecture.year} - {next.lecture.division}
              </p>
              <p className="mt-5 inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700">
                <Clock3 className="h-4 w-4" />
                {duration(next.startsInMinutes)}
              </p>
            </>
          ) : (
            <p className="mt-8 text-sm text-slate-500">No upcoming lecture scheduled.</p>
          )}
        </div>
      </Card>
    </div>
  );
}
