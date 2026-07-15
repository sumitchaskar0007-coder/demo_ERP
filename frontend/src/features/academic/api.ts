import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { AcademicClass, CourseYear, Division, FinalAdmission, Section, Subject, TimetableEntry } from "./types";
const get = <T>(url: string, params?: object) =>
  apiClient.get<ApiResponse<T>>(url, { params }).then((r) => r.data.data);
const post = <T>(url: string, data?: object) =>
  apiClient.post<ApiResponse<T>>(url, data).then((r) => r.data.data);
const patch = <T>(url: string, data?: object) =>
  apiClient.patch<ApiResponse<T>>(url, data).then((r) => r.data.data);
const put = <T>(url: string, data?: object) =>
  apiClient.put<ApiResponse<T>>(url, data).then((r) => r.data.data);
export const createCourseYear = (data: object) => post<CourseYear>("/api/principal/course-years", data);
export const searchCourseYears = (params?: object) =>
  get<PageResponse<CourseYear>>("/api/principal/course-years/search", params);
export const getCourseYear = (id: number) => get<CourseYear>(`/api/principal/course-years/${id}`);
export const updateCourseYear = (id: number, data: object) => put<CourseYear>(`/api/principal/course-years/${id}`, data);
export const setCourseYearStatus = (id: number, active: boolean) => patch<CourseYear>(`/api/principal/course-years/${id}/${active ? "activate" : "deactivate"}`);
export const createDivision = (data: object) => post<Division>("/api/principal/divisions", data);
export const searchDivisions = (params?: object) => get<PageResponse<Division>>("/api/principal/divisions/search", params);
export const getDivision = (id: number) => get<Division>(`/api/principal/divisions/${id}`);
export const updateDivision = (id: number, data: object) => put<Division>(`/api/principal/divisions/${id}`, data);
export const setDivisionStatus = (id: number, active: boolean) => patch<Division>(`/api/principal/divisions/${id}/${active ? "activate" : "deactivate"}`);
export const eligibleClassTeachers = (id: number) => get<import("@/features/staff/types").StaffResponse[]>(`/api/principal/divisions/${id}/eligible-class-teachers`);
export const assignDivisionClassTeacher = (id: number, staffProfileId: number) => patch<Division>(`/api/principal/divisions/${id}/assign-class-teacher`, { staffProfileId });
export const removeDivisionClassTeacher = (id: number) => patch<Division>(`/api/principal/divisions/${id}/remove-class-teacher`);
export const getFinalAdmissionQueue = (params?: object) =>
  get<FinalAdmission[]>("/api/principal/final-admissions/review-ready", params);
export const getFinalAdmissionDetail = (id: number) =>
  get<FinalAdmission>(`/api/principal/final-admissions/${id}`);
export const approveFinalAdmission = (id: number, data: object) =>
  patch<FinalAdmission>(`/api/principal/final-admissions/${id}/approve`, data);
export const rejectFinalAdmission = (id: number, data: object) =>
  patch<FinalAdmission>(`/api/principal/final-admissions/${id}/reject`, data);
export const createAcademicClass = (data: object) =>
  post<AcademicClass>("/api/academic/classes", data);
export const searchAcademicClasses = (params?: object) =>
  get<AcademicClass[]>("/api/academic/classes/search", params);
export const activateAcademicClass = (id: number) => patch(`/api/academic/classes/${id}/activate`);
export const deactivateAcademicClass = (id: number) =>
  patch(`/api/academic/classes/${id}/deactivate`);
export const createSection = (data: object) => post<Section>("/api/academic/sections", data);
export const searchSections = (params?: object) =>
  get<Section[]>("/api/academic/sections/search", params);
export const assignClassTeacher = (id: number, data: object) =>
  patch(`/api/academic/sections/${id}/assign-class-teacher`, data);
export const assignStudentToSection = (id: number, data: object) =>
  post(`/api/academic/sections/${id}/students`, data);
export const createSubject = (data: object) => post<Subject>("/api/academic/subjects", data);
export const updateSubject = (id: number, data: object) => put<Subject>(`/api/academic/subjects/${id}`, data);
export const deleteSubject = (id: number) => apiClient.delete(`/api/academic/subjects/${id}`);
export const searchSubjects = (params?: object) =>
  get<Subject[]>("/api/academic/subjects/search", params);
export const assignSubjectTeacher = (id: number, data: object) =>
  post(`/api/academic/subjects/${id}/assign-teacher`, data);
export const createTimetableEntry = (data: object) =>
  post<TimetableEntry>("/api/academic/timetable", data);
export const searchTimetable = (sectionId: number) =>
  get<TimetableEntry[]>("/api/academic/timetable", { sectionId });
export const createAttendanceSession = (data: object) =>
  post("/api/academic/attendance/sessions", data);
export const markAttendance = (id: number, data: object) =>
  patch(`/api/academic/attendance/sessions/${id}/mark`, data);
export const submitAttendance = (id: number) =>
  patch(`/api/academic/attendance/sessions/${id}/submit`);
export const getMyStudentTimetable = () => get<TimetableEntry[]>("/api/student/academic/timetable");
export const getMyStudentAttendanceSummary = () =>
  get<Record<string, number>>("/api/student/academic/attendance/summary");
export type StudentRosterItem = { studentProfileId: number; admissionNumber: string; rollNumber?: string | null; fullName: string; email: string; phone: string };
export const eligibleStudentsForClass = (id: number) => get<StudentRosterItem[]>(`/api/academic/classes/${id}/eligible-students`);
export const sectionStudents = (id: number) => get<StudentRosterItem[]>(`/api/academic/sections/${id}/students`);
export const getMyClassRoster = () => get<Record<string, unknown>>("/api/academic/class-teacher/my-class");
export const getMyStudentClass = () => get<Record<string, unknown>>("/api/student/academic/class");
// Class teacher timetable
export const getClassTeacherSection = () => get<Record<string, unknown>>("/api/class-teacher/timetable/my-section");
export const getClassTeacherSubjects = () => get<Record<string, unknown>[]>("/api/class-teacher/timetable/subjects");
export const getClassTeacherTeachers = (subjectId: number) => get<Record<string, unknown>[]>("/api/class-teacher/timetable/teachers", { subjectId });
export const getClassTeacherPeriods = () => get<Record<string, unknown>[]>("/api/class-teacher/timetable/periods");
