import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { FeeStructureResponse } from "@/features/fees/types";
import type { TeacherTimetable } from "@/features/teacherTimetable/api";
export type StudentCategory = "OPEN" | "OBC" | "SC" | "ST" | "SBC" | "VJNT" | "EWS" | "OTHER";
export type Row = Record<string, string | number>;
export interface FeeCollectionRow {
  id: number;
  studentName: string;
  collegeName: string;
  departmentName: string;
  courseYear?: string;
  division?: string;
  studentCategory: StudentCategory;
  amount: number;
  paymentDate: string;
  transactionReference: string;
}
export interface PendingFeeRow {
  id: number;
  studentName: string;
  admissionNumber: string;
  collegeName: string;
  departmentName: string;
  courseYear?: string;
  division?: string;
  studentCategory: StudentCategory;
  totalFee: number;
  paidAmount: number;
  remainingAmount: number;
}
export interface FeeReportPageResponse<T> extends PageResponse<T> {
  totalAmount: number;
}
export interface AdminSummary {
  totalColleges: number;
  activeColleges: number;
  totalPrincipals: number;
  totalStaff: number;
  totalStudents: number;
  totalFeeCollection: number;
  pendingFee: number;
}
export interface AdminAnalytics {
  summary: AdminSummary;
  collegeWiseStudents: { label: string; value: number }[];
  collegeWiseFeeCollection: { label: string; value: number }[];
  feeCollectionTrend: { label: string; value: number }[];
  departmentWiseStudents: { label: string; value: number }[];
  admissionStatusDistribution: Record<string, number>;
  pendingFees: Row[];
}
const get = <T>(url: string, params?: object, signal?: AbortSignal) =>
  apiClient.get<ApiResponse<T>>(url, { params, signal }).then((r) => r.data.data);
export const getAdminAnalytics = (params?: object, signal?: AbortSignal) =>
  get<AdminAnalytics>("/api/super-admin/analytics", params, signal);
export const getPrincipalAnalytics = (params?: object, signal?: AbortSignal) =>
  get<AdminAnalytics>("/api/principal/analytics", params, signal);

export interface LectureLoadRow {
  staffId: number;
  employeeCode: string;
  staffName: string;
  collegeId: number;
  collegeName: string;
  departmentId: number;
  departmentName: string;
  weeklyLectures: number;
  weeklyMinutes: number;
  theoryLectures: number;
  practicalLectures: number;
}
export interface AdminCourseYearOption {
  id: number;
  collegeId: number;
  departmentId: number;
  displayName: string;
  yearName: string;
}
export const getLectureLoad = (params?: object, signal?: AbortSignal) =>
  get<LectureLoadRow[]>("/api/super-admin/lecture-load", params, signal);
export const getStaffTimetable = (staffId: number, signal?: AbortSignal) =>
  get<TeacherTimetable>(`/api/super-admin/lecture-load/${staffId}/timetable`, undefined, signal);
export const getCourseYearOptions = (collegeId: number, departmentId: number) =>
  get<AdminCourseYearOption[]>("/api/super-admin/academic-options/course-years", {
    collegeId,
    departmentId,
  });
export const getCollections = (params?: object) =>
  get<FeeReportPageResponse<FeeCollectionRow>>("/api/super-admin/fees/collections", params);
export const getPendingFees = (params?: object) =>
  get<FeeReportPageResponse<PendingFeeRow>>("/api/super-admin/fees/pending", params);
export const getPrincipalCollections = (params?: object) =>
  get<FeeReportPageResponse<FeeCollectionRow>>("/api/principal/fees/collections", params);
export const getPrincipalPendingFees = (params?: object) =>
  get<FeeReportPageResponse<PendingFeeRow>>("/api/principal/fees/pending", params);
export const getCollectionSummary = () =>
  get<Record<string, number>>("/api/super-admin/fees/collection-summary");
export const getPendingSummary = () =>
  get<Record<string, number>>("/api/super-admin/fees/pending-summary");
export const createAdminFeeStructure = (data: object) =>
  apiClient
    .post<ApiResponse<FeeStructureResponse>>("/api/super-admin/fee-structures", data)
    .then((r) => r.data.data);
export const searchAdminFeeStructures = (params?: object) =>
  get<PageResponse<FeeStructureResponse>>("/api/super-admin/fee-structures/search", params);
export const setAdminFeeStatus = (id: number, active: boolean) =>
  apiClient.patch(`/api/super-admin/fee-structures/${id}/${active ? "activate" : "deactivate"}`);
export const updateAdminFeeStructure = (id: number, data: object) =>
  apiClient
    .put<ApiResponse<FeeStructureResponse>>(`/api/super-admin/fee-structures/${id}`, data)
    .then((r) => r.data.data);
export const deleteAdminFeeStructure = (id: number) =>
  apiClient.delete(`/api/super-admin/fee-structures/${id}`);
