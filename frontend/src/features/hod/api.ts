import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";

export type Summary = {
  totalStudents: number;
  totalTeachers: number;
  totalDivisions: number;
  totalSubjects: number;
  classesRunningToday: number;
  averageAttendance: number;
  pendingTasks: number;
};
export type Division = {
  id: number;
  courseYearId: number;
  courseYear: string;
  academicYear: string;
  name: string;
  code: string;
  capacity: number;
  allocated: number;
  classTeacher?: string | null;
};
export type Student = {
  id: number;
  name: string;
  prn?: string | null;
  admissionNumber: string;
  gender: string;
  admissionStatus: string;
  enrollmentId?: number | null;
  courseYearId?: number | null;
  courseYear?: string | null;
  divisionId?: number | null;
  division?: string | null;
  rollNumber?: string | null;
  allocationStatus: string;
};
export type Teacher = {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  staffType: string;
  subjects: number;
  divisions: number;
  weeklyLectures: number;
  remainingCapacity: number;
  loadStatus: "GREEN" | "YELLOW" | "RED";
};
export type Subject = {
  id: number;
  code: string;
  name: string;
  courseYearId: number;
  courseYear: string;
  academicYear: string;
  weeklyLectures: number;
  teacherIds: number[];
  teacherNames: string[];
  divisionIds: number[];
};
export type Timetable = {
  id: number;
  divisionId: number;
  division: string;
  courseYear: string;
  academicYear: string;
  reviewStatus: string;
  comment?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
  lectures: number;
};
export type Activity = { type: string; message: string; occurredAt: string };
export type Workspace = {
  departmentId: number;
  department: string;
  summary: Summary;
  divisions: Division[];
  students: Student[];
  totalStudents: number;
  page: number;
  totalPages: number;
  teachers: Teacher[];
  subjects: Subject[];
  timetables: Timetable[];
  recentActivity: Activity[];
};
export type RollPreview = {
  studentId: number;
  studentName: string;
  admissionNumber: string;
  currentRollNumber?: string | null;
  proposedRollNumber: string;
};

const data = <T>(request: Promise<{ data: ApiResponse<T> }>) => request.then((r) => r.data.data);
export const workspace = (params?: object) =>
  data<Workspace>(apiClient.get("/api/hod/workspace", { params }));
export const bulkAllocate = (sectionId: number, studentIds: number[]) =>
  data(apiClient.post("/api/hod/students/allocate", { sectionId, studentIds }));
export const autoAllocate = (courseYearId: number, studentIds: number[]) =>
  data(apiClient.post("/api/hod/students/auto-allocate", { courseYearId, studentIds }));
export const transfer = (targetSectionId: number, studentIds: number[]) =>
  data(apiClient.post("/api/hod/students/transfer", { targetSectionId, studentIds }));
export const previewRolls = (sectionId: number, strategy: string) =>
  data<RollPreview[]>(apiClient.post("/api/hod/roll-numbers/preview", { sectionId, strategy }));
export const confirmRolls = (
  sectionId: number,
  assignments: { studentId: number; rollNumber: string }[],
  overwrite = false,
) => data(apiClient.post("/api/hod/roll-numbers/confirm", { sectionId, assignments, overwrite }));
export const allocateSubject = (subjectId: number, teacherId: number, divisionIds: number[]) =>
  data(apiClient.put(`/api/hod/subjects/${subjectId}/allocation`, { teacherId, divisionIds }));
export const assignClassTeacher = (sectionId: number, teacherId: number) =>
  data(apiClient.put(`/api/hod/divisions/${sectionId}/class-teacher`, { teacherId }));
