import { useEffect, useState } from "react";
import { ArrowRight, CalendarCheck2, Clock3 } from "lucide-react";
import { Link } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { attendanceApi, type Lecture } from "./api";
import { ROUTES } from "@/lib/constants";

export function TeacherAttendanceWidget() {
  const [lecture, setLecture] = useState<Lecture | null>();
  useEffect(() => {
    attendanceApi
      .current()
      .then(setLecture)
      .catch(() => setLecture(null));
  }, []);
  return (
    <Card className="mt-6 overflow-hidden">
      <div className="flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
        <div className="flex items-center gap-4">
          <div className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-indigo-600">
            <CalendarCheck2 />
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
              Current active lecture
            </p>
            {lecture === undefined ? (
              <p className="mt-1 text-sm text-slate-500">Checking timetable...</p>
            ) : lecture ? (
              <>
                <h3 className="mt-1 font-bold">
                  {lecture.subject} · {lecture.year} - {lecture.division}
                </h3>
                <p className="mt-1 flex items-center gap-1 text-xs text-slate-500">
                  <Clock3 className="h-3.5 w-3.5" />
                  {lecture.startTime.slice(0, 5)} - {lecture.endTime.slice(0, 5)} · {lecture.period}
                </p>
              </>
            ) : (
              <p className="mt-1 text-sm text-slate-500">No lecture is active right now.</p>
            )}
          </div>
        </div>
        {lecture && (
          <Link
            to={ROUTES.teacherAttendance}
            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-bold text-white hover:bg-indigo-700"
          >
            Start attendance <ArrowRight className="h-4 w-4" />
          </Link>
        )}
      </div>
    </Card>
  );
}
