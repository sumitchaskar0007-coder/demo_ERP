import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { College, CollegeFormValues, CollegeSearchParams } from "./types";

const BASE = "/api/super-admin/colleges";

export async function searchColleges(params: CollegeSearchParams) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<College>>>(`${BASE}/search`, {
    params,
  });
  return data.data;
}

export async function getAllColleges() {
  const { data } = await apiClient.get<ApiResponse<College[]>>(BASE);
  return data.data;
}

export async function getActiveColleges() {
  const { data } = await apiClient.get<ApiResponse<College[]>>(`${BASE}/active`);
  return data.data;
}

export async function getCollege(id: number) {
  const { data } = await apiClient.get<ApiResponse<College>>(`${BASE}/${id}`);
  return data.data;
}

export async function createCollege(values: CollegeFormValues) {
  const { data } = await apiClient.post<ApiResponse<College>>(BASE, values);
  return data;
}

export async function updateCollege(id: number, values: Omit<CollegeFormValues, "code">) {
  const { data } = await apiClient.put<ApiResponse<College>>(`${BASE}/${id}`, values);
  return data;
}

export async function setCollegeStatus(id: number, active: boolean) {
  const action = active ? "activate" : "deactivate";
  const { data } = await apiClient.patch<ApiResponse<College>>(`${BASE}/${id}/${action}`);
  return data;
}
