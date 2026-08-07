import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminStudentDetails,
  FeeCategoryAssessmentOption,
  ScholarshipResponse,
  StudentProfileResponse,
  StudentStatus,
} from "./types";

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

export async function approveScholarship(
  studentId: number,
  values: { amount: number; remarks?: string },
) {
  const { data } = await apiClient.post<ApiResponse<ScholarshipResponse>>(
    `/api/principal/students/${studentId}/scholarship/approve`,
    values,
  );
  return data.data;
}

export async function getFeeCategoryOptions(studentId: number) {
  const { data } = await apiClient.get<ApiResponse<FeeCategoryAssessmentOption[]>>(
    `/api/principal/students/${studentId}/fee-adjustments/category-options`,
  );
  return data.data;
}

export async function removeScholarship(studentId: number, reason: string) {
  const { data } = await apiClient.post<ApiResponse<ScholarshipResponse>>(
    `/api/principal/students/${studentId}/fee-adjustments/scholarship/remove`,
    { reason },
  );
  return data.data;
}

export async function changeStudentCategory(
  studentId: number,
  values: {
    studentCategory: import("@/features/admissions/types").StudentCategory;
    customCategoryName?: string;
    caste: string;
    reason: string;
  },
) {
  const { data } = await apiClient.patch<ApiResponse<unknown>>(
    `/api/principal/students/${studentId}/fee-adjustments/category`,
    values,
  );
  return data.data;
}

export async function getAvailableOtherCategories(collegeCode: string, departmentId: number) {
  const { data } = await apiClient.get<
    ApiResponse<Array<{ category: string; customCategoryName?: string | null; label: string }>>
  >(`/api/public/admissions/college/${collegeCode}/departments/${departmentId}/categories`);
  return data.data.filter((option) => option.category === "OTHER" && option.customCategoryName);
}
