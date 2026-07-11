import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { Department, DepartmentFormValues, DepartmentSearchParams } from "./types";

const BASE = "/api/principal/departments";

export async function searchDepartments(params: DepartmentSearchParams) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<Department>>>(`${BASE}/search`, {
    params,
  });
  return data.data;
}
export async function getDepartment(id: number) {
  const { data } = await apiClient.get<ApiResponse<Department>>(`${BASE}/${id}`);
  return data.data;
}
export async function createDepartment(
  values: DepartmentFormValues & { collegeId: number; code: string },
) {
  const { data } = await apiClient.post<ApiResponse<Department>>(BASE, values);
  return data;
}
export async function updateDepartment(
  id: number,
  values: Pick<DepartmentFormValues, "name" | "description">,
) {
  const { data } = await apiClient.put<ApiResponse<Department>>(`${BASE}/${id}`, values);
  return data;
}
export async function setDepartmentStatus(id: number, active: boolean) {
  const { data } = await apiClient.patch<ApiResponse<Department>>(
    `${BASE}/${id}/${active ? "activate" : "deactivate"}`,
  );
  return data;
}
