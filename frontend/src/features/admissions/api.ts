import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdmissionPrintResponse,
  AdmissionResponse,
  AdmissionStatus,
  AdmissionStatusHistoryResponse,
  PublicAdmissionInfoResponse,
  StudentSectionAdmissionResponse,
  SubmitAdmissionRequest,
  SubmitAdmissionResponse,
} from "./types";

export async function getPublicAdmissionInfo(collegeCode: string) {
  const { data } = await apiClient.get<ApiResponse<PublicAdmissionInfoResponse>>(
    `/api/public/admissions/college/${collegeCode}/info`,
  );
  return data.data;
}
export async function submitAdmission(collegeCode: string, values: SubmitAdmissionRequest) {
  const payload = {
    ...values,
    previousPercentage: values.previousPercentage === "" ? undefined : values.previousPercentage,
  };
  const { data } = await apiClient.post<ApiResponse<SubmitAdmissionResponse>>(
    `/api/public/admissions/college/${collegeCode}/submit`,
    payload,
  );
  return data.data;
}
export async function getMyAdmission() {
  const { data } = await apiClient.get<ApiResponse<AdmissionResponse>>(
    "/api/student/admissions/me",
  );
  return data.data;
}
export async function getStudentProfile() {
  const { data } =
    await apiClient.get<ApiResponse<import("@/features/student/types").StudentProfileResponse>>(
      "/api/student/profile/me",
    );
  return data.data;
}
export async function searchStudentSectionAdmissions(params: {
  keyword?: string;
  departmentId?: number;
  status?: AdmissionStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudentSectionAdmissionResponse>>>(
    "/api/student-section/admissions",
    { params },
  );
  return data.data;
}
export async function getStudentSectionAdmission(id: number) {
  const { data } = await apiClient.get<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}`,
  );
  return data.data;
}
export async function startAdmissionReview(id: number) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/start-review`,
  );
  return data.data;
}
export async function approveAdmission(
  id: number,
  values: { studentCategory: import("./types").StudentCategory; remarks?: string },
) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/approve`,
    values,
  );
  return data.data;
}
export async function rejectAdmission(id: number, values: { rejectionReason: string }) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/reject`,
    values,
  );
  return data.data;
}
export async function getAdmissionHistory(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionStatusHistoryResponse[]>>(
    `/api/student-section/admissions/${id}/history`,
  );
  return data.data;
}
export async function getAdmissionPrintData(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionPrintResponse>>(
    `/api/student-section/admissions/${id}/print-data`,
  );
  return data.data;
}
export async function markAdmissionPrinted(id: number, values: { remarks?: string }) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/mark-printed`,
    values,
  );
  return data.data;
}
export async function getPrincipalReviewReadyAdmissions(params: {
  keyword?: string;
  departmentId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudentSectionAdmissionResponse>>>(
    "/api/principal/admissions/review-ready",
    { params },
  );
  return data.data;
}
export async function getPrincipalAdmission(id: number) {
  const { data } = await apiClient.get<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/principal/admissions/${id}`,
  );
  return data.data;
}
export async function getPrincipalAdmissionHistory(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionStatusHistoryResponse[]>>(
    `/api/principal/admissions/${id}/history`,
  );
  return data.data;
}
