import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { AdminStudentDetails, StudentProfileResponse, StudentStatus } from "./types";

export async function searchStudents(
  params: {
    keyword?: string;
    collegeId?: number;
    departmentId?: number;
    status?: StudentStatus | "";
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  },
  principal = false,
) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudentProfileResponse>>>(
    `/api/${principal ? "principal" : "super-admin"}/students/search`,
    { params },
  );
  return data.data;
}

export async function getStudentDetails(id: number, principal = false) {
  const { data } = await apiClient.get<ApiResponse<AdminStudentDetails>>(
    `/api/${principal ? "principal" : "super-admin"}/students/${id}/details`,
  );
  return data.data;
}
