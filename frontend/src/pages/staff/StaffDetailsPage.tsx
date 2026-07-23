import {
  ArrowLeft,
  BookOpen,
  Building2,
  CalendarCheck2,
  CalendarDays,
  CheckCircle2,
  Clock3,
  GraduationCap,
  Mail,
  Phone,
  UserRound,
  UsersRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Badge, StatusBadge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Card } from "@/components/common/Card";
import { EmptyState } from "@/components/common/EmptyState";
import { Loader } from "@/components/common/Loader";
import { getStaffDetails } from "@/features/staff/api";
import type { StaffDetailResponse } from "@/features/staff/types";
import { handleApiError } from "@/lib/handleApiError";
import { initials } from "@/lib/utils";

export function StaffDetailsPage() {
  const { id } = useParams();
  const [details, setDetails] = useState<StaffDetailResponse | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setFailed(false);
    getStaffDetails(Number(id))
      .then(setDetails)
      .catch((error) => {
        setFailed(true);
        toast.error(handleApiError(error).message);
      });
  }, [id]);

  if (!details) {
    return (
      <div className="page-container">
        {failed ? (
          <EmptyState
            title="Staff details unavailable"
            description="The staff member could not be found or is outside your college."
          />
        ) : (
          <Loader label="Loading staff details..." />
        )}
      </div>
    );
  }

  const { staff, attendanceSummary: attendance } = details;
  const attendanceRate = attendance.studentsMarked
    ? Math.round(((attendance.present + attendance.late) * 1000) / attendance.studentsMarked) / 10
    : 0;

  return (
    <div className="page-container space-y-5">
      <Link to="/staff">
        <Button variant="ghost">
          <ArrowLeft className="h-4 w-4" />
          Back to staff
        </Button>
      </Link>

      <Card className="overflow-hidden">
        <div className="bg-gradient-to-r from-slate-900 via-blue-950 to-blue-800 p-6 text-white sm:p-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <div className="grid h-20 w-20 shrink-0 place-items-center rounded-2xl bg-white/15 text-2xl font-bold ring-1 ring-white/20">
              {initials(staff.fullName)}
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-2xl font-bold sm:text-3xl">{staff.fullName}</h1>
                <StatusBadge status={staff.status} />
              </div>
              <p className="mt-1 text-sm text-blue-100">{staff.employeeCode}</p>
              <div className="mt-4 flex flex-wrap gap-2">
                {staff.roles.map((role) => (
                  <Badge key={role} tone="info">
                    {role.replaceAll("_", " ")}
                  </Badge>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-4">
          <Info icon={Mail} label="Email" value={staff.email} />
          <Info icon={Phone} label="Phone" value={staff.phone || "Not provided"} />
          <Info icon={Building2} label="College" value={staff.collegeName} />
          <Info
            icon={GraduationCap}
            label="Department"
            value={staff.departmentNames.join(", ") || staff.departmentName || "Not assigned"}
          />
          <Info icon={CalendarDays} label="Joining date" value={displayDate(staff.joiningDate)} />
          <Info icon={UserRound} label="Staff type" value={staff.staffType.replaceAll("_", " ")} />
        </div>
      </Card>

      <section>
        <div className="mb-3">
          <h2 className="text-lg font-bold text-slate-900">Academic assignments</h2>
          <p className="text-sm text-slate-500">
            Class-teacher responsibility and subjects currently assigned to this staff member.
          </p>
        </div>
        <div className="grid gap-4 xl:grid-cols-2">
          <Card className="p-5">
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-50 text-blue-700">
                <UsersRound className="h-5 w-5" />
              </span>
              <div>
                <h3 className="font-bold text-slate-900">Class teacher</h3>
                <p className="text-xs text-slate-500">
                  {details.classAssignments.length} active class assignment(s)
                </p>
              </div>
            </div>
            {details.classAssignments.length ? (
              <div className="mt-4 space-y-3">
                {details.classAssignments.map((assignment) => (
                  <div key={assignment.sectionId} className="rounded-xl border bg-slate-50 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <p className="font-bold text-slate-900">
                          {assignment.className} - {assignment.sectionName}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          {assignment.departmentName} · {assignment.academicYear}
                        </p>
                      </div>
                      <Badge tone="info">{assignment.sectionCode}</Badge>
                    </div>
                    <p className="mt-3 text-xs font-medium text-slate-600">
                      Capacity: {assignment.capacity} students
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
                This staff member is not currently assigned as a class teacher.
              </p>
            )}
          </Card>

          <Card className="p-5">
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-violet-50 text-violet-700">
                <BookOpen className="h-5 w-5" />
              </span>
              <div>
                <h3 className="font-bold text-slate-900">Teaching subjects</h3>
                <p className="text-xs text-slate-500">
                  {details.subjectAssignments.length} active subject assignment(s)
                </p>
              </div>
            </div>
            {details.subjectAssignments.length ? (
              <div className="mt-4 space-y-3">
                {details.subjectAssignments.map((assignment) => (
                  <div key={assignment.subjectId} className="rounded-xl border bg-slate-50 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <p className="font-bold text-slate-900">{assignment.subjectName}</p>
                        <p className="mt-1 text-xs text-slate-500">
                          {assignment.className} · {assignment.academicYear}
                        </p>
                      </div>
                      <Badge>{assignment.subjectCode}</Badge>
                    </div>
                    <p className="mt-3 text-xs text-slate-600">
                      Divisions: {assignment.divisions.join(", ") || "Not assigned"}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
                No active subject assignment is linked to this staff member.
              </p>
            )}
          </Card>
        </div>
      </section>

      <section>
        <div className="mb-3">
          <h2 className="text-lg font-bold text-slate-900">Attendance activity</h2>
          <p className="text-sm text-slate-500">
            Attendance sessions taken by this teacher and the latest marking records.
          </p>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Metric
            icon={CalendarCheck2}
            label="Sessions taken"
            value={attendance.totalSessions}
            help={`${attendance.submittedSessions} submitted · ${attendance.draftSessions} draft`}
          />
          <Metric
            icon={UsersRound}
            label="Students marked"
            value={attendance.studentsMarked}
            help="Across all attendance sessions"
          />
          <Metric
            icon={CheckCircle2}
            label="Present / late"
            value={attendance.present + attendance.late}
            help={`${attendanceRate}% of marked records`}
          />
          <Metric
            icon={Clock3}
            label="Absent / leave"
            value={attendance.absent + attendance.leave}
            help={`${attendance.absent} absent · ${attendance.leave} leave`}
          />
        </div>

        <Card className="mt-4 overflow-hidden">
          {details.recentAttendance.length ? (
            <div className="erp-table-scroll">
              <table className="erp-table min-w-[900px]">
                <thead>
                  <tr>
                    <th>Date & time</th>
                    <th>Class</th>
                    <th>Subject</th>
                    <th>Status</th>
                    <th>Marked</th>
                    <th>Present</th>
                    <th>Absent</th>
                    <th>Late / leave</th>
                  </tr>
                </thead>
                <tbody>
                  {details.recentAttendance.map((session) => (
                    <tr key={session.sessionId}>
                      <td>
                        <p className="font-semibold text-slate-800">
                          {displayDate(session.attendanceDate)}
                        </p>
                        <p className="mt-1 text-xs text-slate-400">
                          {shortTime(session.startTime)} - {shortTime(session.endTime)}
                        </p>
                      </td>
                      <td>
                        {session.className} - {session.sectionName}
                      </td>
                      <td>{session.subjectName}</td>
                      <td>
                        <StatusBadge status={session.status} />
                      </td>
                      <td>{session.studentsMarked}</td>
                      <td className="font-semibold text-emerald-700">{session.present}</td>
                      <td className="font-semibold text-red-600">{session.absent}</td>
                      <td>
                        {session.late} / {session.leave}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              title="No attendance sessions"
              description="This staff member has not taken attendance yet."
            />
          )}
        </Card>
      </section>
    </div>
  );
}

function Info({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Mail;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">
      <Icon className="h-4 w-4 text-blue-600" />
      <p className="mt-3 text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 break-words text-sm font-semibold text-slate-800">{value}</p>
    </div>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
  help,
}: {
  icon: typeof CalendarCheck2;
  label: string;
  value: number;
  help: string;
}) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-wide text-slate-400">{label}</p>
          <p className="mt-2 text-3xl font-bold text-slate-900">{value}</p>
        </div>
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-50 text-blue-700">
          <Icon className="h-5 w-5" />
        </span>
      </div>
      <p className="mt-3 text-xs text-slate-500">{help}</p>
    </Card>
  );
}

function displayDate(value?: string | null) {
  if (!value) return "Not provided";
  return new Date(`${value.slice(0, 10)}T00:00:00`).toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function shortTime(value: string) {
  return value?.slice(0, 5) || "--:--";
}
