import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
import type { AuthUser, LoginRequest, LoginResponse } from "./types";

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>("/api/auth/login", request);
  return data.data;
}

export async function getProfile(): Promise<AuthUser> {
  const { data } = await apiClient.get<ApiResponse<AuthUser>>("/api/auth/profile");
  return data.data;
}

export async function changePassword(request: { currentPassword: string; newPassword: string }): Promise<AuthUser> {
  const { data } = await apiClient.post<ApiResponse<AuthUser>>("/api/auth/change-password", request);
  return data.data;
}
