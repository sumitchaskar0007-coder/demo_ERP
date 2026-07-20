import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
export type ReportRow = Record<string, string | number>;
export interface AdmissionTimelineItem { stage:string; status:string; date?:string; responsibleUser?:string; remarks?:string }
export interface AdmissionAnalyticsRow { id:number; admissionReferenceNumber:string; admissionNumber:string; studentName:string; gender:string; dateOfBirth:string; email:string; phone:string; guardianName:string; guardianPhone:string; address:string; collegeId:number; college:string; departmentId:number; department:string; departmentCode:string; academicYear:string; year:string; divisionId?:number; division:string; classTeacher:string; status:string; submittedAt:string; approvedAt?:string; documentsUploaded:boolean; feePaid:boolean; feePending:boolean; remarks?:string; processingDays:number; timeline:AdmissionTimelineItem[] }
export interface AdmissionGroup { key:string; label:string; parentKey:string; applications:number; approved:number; pending:number; rejected:number; admissionRate:number; classTeacher:string }
export interface AdmissionAnalytics { summary:{totalApplications:number;approvedAdmissions:number;pendingReviews:number;rejectedApplications:number;todayApplications:number;successRate:number;documentsPending:number;feePending:number;averageProcessingDays:number}; funnel:{key:string;label:string;count:number}[]; trend:{date:string;applications:number;approved:number;rejected:number}[]; departments:AdmissionGroup[]; years:AdmissionGroup[]; divisions:AdmissionGroup[]; rows:AdmissionAnalyticsRow[]; totalElements:number;totalPages:number;page:number;size:number }
export const getReport = (type: string, params?: object) =>
  apiClient
    .get<ApiResponse<ReportRow[]>>(`/api/reports/${type}`, { params })
    .then((r) => r.data.data);
export const getAdmissionAnalytics = (params?: object) => apiClient
  .get<ApiResponse<AdmissionAnalytics>>("/api/reports/admissions/analytics", { params })
  .then(r => r.data.data);
export async function exportReport(type: string, params?: object) {
  const r = await apiClient.get(`/api/reports/${type}/export`, { params, responseType: "blob" });
  const url = URL.createObjectURL(r.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${type}-report.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
