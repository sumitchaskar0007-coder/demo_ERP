import { apiClient } from "@/lib/apiClient";

export type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "LEAVE";
export interface Lecture {
  lectureId: number;
  sessionId?: number;
  sessionStatus?: "DRAFT" | "SUBMITTED";
  date: string;
  period: string;
  lectureNumber: number;
  startTime: string;
  endTime: string;
  subjectId: number;
  subject: string;
  subjectCode: string;
  departmentId: number;
  department: string;
  divisionId: number;
  year: string;
  division: string;
  lectureType: string;
  room?: string;
  active: boolean;
  canMark: boolean;
}
export interface StudentRow {
  recordId?: number;
  studentId: number;
  rollNumber?: string;
  admissionNumber: string;
  studentName: string;
  photoUrl?: string;
  status: AttendanceStatus;
  remarks?: string;
}
export interface Roster {
  lecture: Lecture;
  students: StudentRow[];
  totalStudents: number;
  counts: Record<AttendanceStatus, number>;
  editable: boolean;
}
export interface SessionSummary {
  id: number;
  date: string;
  time: string;
  subjectId: number;
  subject: string;
  departmentId: number;
  department: string;
  divisionId: number;
  year: string;
  division: string;
  teacher: string;
  status: string;
  total: number;
  present: number;
  absent: number;
  late: number;
  leave: number;
  percentage: number;
}
export interface SubjectSummary {
  subjectId: number;
  subject: string;
  total: number;
  attended: number;
  absent: number;
  late: number;
  leave: number;
  percentage: number;
  indicator: string;
}
export interface MonthSummary {
  month: string;
  total: number;
  attended: number;
  percentage: number;
}
export interface TrendPoint {
  date: string;
  total: number;
  attended: number;
  percentage: number;
}
export interface OperationalSummary {
  id: number;
  name: string;
  lectures: number;
  submitted: number;
  pending: number;
}
export interface StudentAnalyticsRow {
  studentId: number;
  admissionNumber: string;
  rollNumber?: string;
  studentName: string;
  gender?: string;
  photoUrl?: string;
  guardianName?: string;
  mobile?: string;
  departmentId: number;
  department: string;
  academicYear: string;
  year: string;
  divisionId: number;
  division: string;
  classTeacher: string;
  total: number;
  present: number;
  absent: number;
  late: number;
  leave: number;
  percentage: number;
  indicator: string;
  subjects: SubjectSummary[];
  monthly: MonthSummary[];
  history: {
    date: string;
    time: string;
    subject: string;
    teacher: string;
    status: AttendanceStatus;
    remarks?: string;
  }[];
}
export interface StudentAttendance {
  studentId: number;
  studentName: string;
  rollNumber?: string;
  overallPercentage: number;
  indicator: string;
  subjects: SubjectSummary[];
  monthly: MonthSummary[];
  history: {
    date: string;
    time: string;
    subject: string;
    teacher: string;
    status: AttendanceStatus;
    remarks?: string;
  }[];
}
export interface AttendanceReport {
  from: string;
  to: string;
  sessions: number;
  totalMarks: number;
  present: number;
  absent: number;
  late: number;
  leave: number;
  percentage: number;
  totalStudents: number;
  uniquePresentToday: number;
  uniqueAbsentToday: number;
  todayLectures: number;
  submittedToday: number;
  pendingToday: number;
  departments: number;
  divisions: number;
  below75: number;
  below50: number;
  trend: TrendPoint[];
  departmentOperations: OperationalSummary[];
  divisionOperations: OperationalSummary[];
  teacherOperations: OperationalSummary[];
  students: StudentAnalyticsRow[];
  rows: SessionSummary[];
  subjects: SubjectSummary[];
  monthly: MonthSummary[];
}

const unwrap = <T>(response: { data: { data: T } }) => response.data.data;
const compact = (params: Record<string, unknown>) =>
  Object.fromEntries(Object.entries(params).filter(([, value]) => value !== "" && value != null));

export const attendanceApi = {
  current: async () =>
    unwrap<Lecture | null>(await apiClient.get("/api/teacher/attendance/current-lecture")),
  roster: async (lectureId: number) =>
    unwrap<Roster>(
      await apiClient.get("/api/teacher/attendance/students", { params: { lectureId } }),
    ),
  create: async (
    lectureId: number,
    records: Pick<StudentRow, "studentId" | "status" | "remarks">[],
    submit: boolean,
  ) =>
    unwrap<Roster>(await apiClient.post("/api/teacher/attendance", { lectureId, records, submit })),
  update: async (
    sessionId: number,
    records: Pick<StudentRow, "studentId" | "status" | "remarks">[],
    submit: boolean,
  ) =>
    unwrap<Roster>(
      await apiClient.put(`/api/teacher/attendance/${sessionId}`, { records, submit }),
    ),
  history: async (params: {
    from?: string;
    to?: string;
    subjectId?: number;
    divisionId?: number;
  }) =>
    unwrap<SessionSummary[]>(
      await apiClient.get("/api/teacher/attendance/history", { params: compact(params) }),
    ),
  student: async () => unwrap<StudentAttendance>(await apiClient.get("/api/student/attendance")),
  report: async (path: string, params: Record<string, unknown>) =>
    unwrap<AttendanceReport>(await apiClient.get(path, { params: compact(params) })),
};
