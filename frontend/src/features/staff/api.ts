import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  CreateFeeSectionStaffRequest,
  CreateStudentSectionStaffRequest,
  CreateStaffRequest,
  StaffResponse,
  StaffDetailResponse,
  StaffStatus,
  StaffType,
  UpdateStaffAssignmentRequest,
} from "./types";

export async function createStaff(values: CreateStaffRequest) {
  const { data } = await apiClient.post<ApiResponse<StaffResponse>>("/api/principal/staff", values);
  return data.data;
}

export async function createStudentSectionStaff(values: CreateStudentSectionStaffRequest) {
  const { data } = await apiClient.post<ApiResponse<StaffResponse>>(
    "/api/principal/staff/student-section",
    values,
  );
  return data.data;
}
export async function createFeeSectionStaff(values: CreateFeeSectionStaffRequest) {
  const { data } = await apiClient.post<ApiResponse<StaffResponse>>(
    "/api/principal/staff/fee-section",
    values,
  );
  return data.data;
}
export async function searchStaff(params: {
  keyword?: string;
  collegeId?: number;
  departmentId?: number;
  staffType?: StaffType | "";
  status?: StaffStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StaffResponse>>>(
    "/api/principal/staff/search",
    { params },
  );
  return data.data;
}
export async function searchAdminStaff(params: {
  keyword?: string;
  collegeId?: number;
  staffType?: StaffType | "";
  status?: StaffStatus | "";
  page?: number;
  size?: number;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StaffResponse>>>(
    "/api/super-admin/staff/search",
    { params },
  );
  return data.data;
}
export async function getStaffById(id: number) {
  const { data } = await apiClient.get<ApiResponse<StaffResponse>>(`/api/principal/staff/${id}`);
  return data.data;
}
export async function getStaffDetails(id: number) {
  const { data } = await apiClient.get<ApiResponse<StaffDetailResponse>>(
    `/api/principal/staff/${id}/details`,
  );
  return data.data;
}
export async function updateStaffAssignment(id: number, values: UpdateStaffAssignmentRequest) {
  const { data } = await apiClient.put<ApiResponse<StaffResponse>>(
    `/api/principal/staff/${id}/assignment`,
    values,
  );
  return data.data;
}
export async function activateStaff(id: number) {
  const { data } = await apiClient.patch<ApiResponse<StaffResponse>>(
    `/api/principal/staff/${id}/activate`,
  );
  return data.data;
}
export async function deactivateStaff(id: number) {
  const { data } = await apiClient.patch<ApiResponse<StaffResponse>>(
    `/api/principal/staff/${id}/deactivate`,
  );
  return data.data;
}
