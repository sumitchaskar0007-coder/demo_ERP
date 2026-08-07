import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
import type { AuthUser, LoginRequest, UpdateOwnProfileValues } from "./types";

export async function login(request: LoginRequest): Promise<AuthUser> {
  const { data } = await apiClient.post<ApiResponse<AuthUser>>("/api/v1/auth/login", request);
  return data.data;
}

export async function getProfile(): Promise<AuthUser> {
  const { data } = await apiClient.get<ApiResponse<AuthUser>>("/api/v1/auth/me");
  return data.data;
}

export async function logout(): Promise<void> {
  await apiClient.post("/api/v1/auth/logout");
}

export async function bootstrapSession(): Promise<AuthUser> {
  return getProfile();
}

export async function updateProfile(request: UpdateOwnProfileValues): Promise<AuthUser> {
  const { data } = await apiClient.put<ApiResponse<AuthUser>>("/api/auth/profile", request);
  return data.data;
}

export async function uploadProfilePhoto(file: File): Promise<AuthUser> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post<ApiResponse<AuthUser>>("/api/auth/profile/photo", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data.data;
}
export async function changePassword(request: {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}): Promise<void> {
  await apiClient.patch("/api/account/change-password", request);
}
export async function forgotPassword(email: string) {
  const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/password/forgot", { email });
  return data;
}
export async function resetPassword(token: string, newPassword: string) {
  const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/password/reset", {
    token,
    newPassword,
  });
  return data;
}
export async function confirmEmailVerification(token: string) {
  const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/email-verification/confirm", {
    token,
  });
  return data;
}
export async function requestEmailVerification() {
  const { data } = await apiClient.post<ApiResponse<null>>("/api/auth/email-verification/request");
  return data;
}
