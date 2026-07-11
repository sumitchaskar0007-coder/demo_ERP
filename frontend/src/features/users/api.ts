import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { CreatePrincipalValues, User, UserSearchParams } from "./types";

const BASE = "/api/super-admin/users";

export async function searchUsers(params: UserSearchParams) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<User>>>(`${BASE}/search`, {
    params,
  });
  return data.data;
}
export async function getUser(id: number) {
  const { data } = await apiClient.get<ApiResponse<User>>(`${BASE}/${id}`);
  return data.data;
}
export async function createPrincipal(values: CreatePrincipalValues) {
  const { data } = await apiClient.post<ApiResponse<User>>(`${BASE}/principals`, values);
  return data;
}
export async function setUserStatus(id: number, active: boolean) {
  const { data } = await apiClient.patch<ApiResponse<User>>(
    `${BASE}/${id}/${active ? "activate" : "deactivate"}`,
  );
  return data;
}
