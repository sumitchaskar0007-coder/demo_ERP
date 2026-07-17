import { apiClient } from "@/lib/apiClient";
export interface Master {
  id: number;
  type: string;
  name: string;
  code?: string;
  details: string;
}
export interface Entry {
  id: number;
  dayOfWeek: string;
  periodNumber: number;
  startTime: string;
  endTime: string;
  type: string;
  subjectId?: number;
  subject?: string;
  teacherId?: number;
  teacher?: string;
  roomId?: number;
  room?: string;
}
export interface Timetable {
  id: number;
  academicYear: string;
  term: string;
  classId: number;
  className: string;
  sectionId: number;
  section: string;
  weekStart: string;
  status: string;
  entries: Entry[];
}
export interface AttendanceItem {
  id?: number;
  studentId: number;
  admissionNumber: string;
  studentName: string;
  status?: string;
  remarks?: string;
}
export interface Session {
  id: number;
  date: string;
  type: string;
  status: string;
  lockAt: string;
  teacherId: number;
  teacher: string;
  className: string;
  section: string;
  subject: string;
  attendance: AttendanceItem[];
}
export const academicApi = {
  masters: async (type: string) =>
    (await apiClient.get<Master[]>("/api/academic/masters", { params: { type } })).data,
  create: async (body: Record<string, unknown>) =>
    (await apiClient.post<Master>("/api/academic/masters", body)).data,
  people: async (kind = "TEACHERS") =>
    (await apiClient.get<Master[]>("/api/academic/people", { params: { kind } })).data,
  assign: async (body: Record<string, unknown>) =>
    (await apiClient.post("/api/academic/assignments", body)).data,
  enroll: async (body: Record<string, unknown>) =>
    (await apiClient.post("/api/academic/enrollments", body)).data,
};
export const timetableApi = {
  list: async () => (await apiClient.get<Timetable[]>("/api/timetables")).data,
  create: async (body: Record<string, unknown>) =>
    (await apiClient.post<Timetable>("/api/timetables", body)).data,
  add: async (id: number, body: Record<string, unknown>) =>
    (await apiClient.post(`/api/timetables/${id}/entries`, body)).data,
  publish: async (id: number) => (await apiClient.post(`/api/timetables/${id}/publish`)).data,
};
export interface WeeklyDivision {
  id: number;
  collegeId: number;
  departmentId: number;
  courseYearId: number;
  department: string;
  year: string;
  division: string;
  classTeacher: string;
  academicYear: string;
  editable: boolean;
}
export interface WeeklyOption {
  id: number;
  label: string;
}
export interface WeeklySubjectTeacherOption {
  subjectId: number;
  teachers: WeeklyOption[];
}
export interface WeeklyPeriod {
  id: number;
  position: number;
  label: string;
  startTime: string;
  endTime: string;
  kind: "TEACHING" | "SHORT_BREAK" | "LUNCH_BREAK";
}
export interface WeeklyEntry {
  id: number;
  dayOfWeek: string;
  periodId: number;
  subjectId: number;
  subject: string;
  teacherId: number;
  teacher: string;
  room?: string;
  lectureType: string;
  remarks?: string;
}
export interface WeeklyTimetable {
  id: number;
  sectionId: number;
  department: string;
  year: string;
  division: string;
  classTeacher: string;
  academicYear: string;
  status: string;
  editable: boolean;
  periods: WeeklyPeriod[];
  entries: WeeklyEntry[];
  subjects: WeeklyOption[];
  teachers: WeeklyOption[];
  subjectTeachers: WeeklySubjectTeacherOption[];
  rooms: string[];
}
const unwrap = <T>(response: { data: { data: T } }) => response.data.data;
export type WeeklyPeriodInput = {
  id?: number;
  label: string;
  startTime: string;
  endTime: string;
  kind: "TEACHING" | "SHORT_BREAK" | "LUNCH_BREAK";
};
export type WeeklyEntryInput = {
  dayOfWeek: string;
  periodId: number;
  subjectId: number;
  teacherId: number;
  room?: string;
  lectureType: string;
  remarks?: string;
};
export const weeklyTimetableApi = {
  divisions: async () =>
    unwrap<WeeklyDivision[]>(await apiClient.get("/api/weekly-timetables/divisions")),
  get: async (sectionId: number) =>
    unwrap<WeeklyTimetable>(await apiClient.get(`/api/weekly-timetables/sections/${sectionId}`)),
  save: async (id: number, day: string, periodId: number, body: Record<string, unknown>) =>
    unwrap<WeeklyEntry>(
      await apiClient.put(`/api/weekly-timetables/${id}/entries/${day}/${periodId}`, body),
    ),
  remove: async (id: number, day: string, periodId: number) =>
    apiClient.delete(`/api/weekly-timetables/${id}/entries/${day}/${periodId}`),
  move: async (id: number, body: Record<string, unknown>) =>
    unwrap<WeeklyTimetable>(
      await apiClient.patch(`/api/weekly-timetables/${id}/entries/move`, body),
    ),
  copyDay: async (id: number, sourceDay: string, targetDay: string, overwrite = true) =>
    unwrap<WeeklyTimetable>(
      await apiClient.post(`/api/weekly-timetables/${id}/copy-day`, {
        sourceDay,
        targetDay,
        overwrite,
      }),
    ),
  copyTimetable: async (id: number, sourceTimetableId: number, overwrite = true) =>
    unwrap<WeeklyTimetable>(
      await apiClient.post(`/api/weekly-timetables/${id}/copy-timetable`, {
        sourceTimetableId,
        overwrite,
      }),
    ),
  replaceEntries: async (id: number, entries: WeeklyEntryInput[]) =>
    unwrap<WeeklyTimetable>(
      await apiClient.put(`/api/weekly-timetables/${id}/entries`, { entries }),
    ),
  updatePeriods: async (id: number, periods: WeeklyPeriodInput[]) =>
    unwrap<WeeklyTimetable>(
      await apiClient.put(`/api/weekly-timetables/${id}/periods`, { periods }),
    ),
};
export const attendanceApi = {
  sessions: async (from: string, to: string) =>
    (await apiClient.get<Session[]>("/api/attendance/sessions", { params: { from, to } })).data,
  submit: async (
    id: number,
    attendance: { studentId: number; status: string; remarks?: string }[],
  ) => (await apiClient.post(`/api/attendance/sessions/${id}/submit`, { attendance })).data,
  correct: async (id: number, status: string, reason: string) =>
    (await apiClient.post(`/api/attendance/records/${id}/correct`, { status, reason })).data,
  report: async (studentId: number, from: string, to: string) =>
    (await apiClient.get(`/api/attendance/reports/students/${studentId}`, { params: { from, to } }))
      .data,
};
