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

export async function changePassword(request: { currentPassword: string; newPassword: string; confirmPassword: string }): Promise<void> {
  await apiClient.patch("/api/account/change-password", request);
}
export async function forgotPassword(email: string) { const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/password/forgot", { email }); return data; }
export async function resetPassword(token: string, newPassword: string) { const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/password/reset", { token, newPassword }); return data; }
export async function confirmEmailVerification(token: string) { const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/email-verification/confirm", { token }); return data; }
export async function requestEmailVerification() { const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/email-verification/request"); return data; }
