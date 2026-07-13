import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
import type { AuthUser, LoginRequest, LoginResponse, UpdateOwnProfileValues } from "./types";

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>("/api/auth/login", request);
  return data.data;
}

export async function getProfile(): Promise<AuthUser> {
  const { data } = await apiClient.get<ApiResponse<AuthUser>>("/api/auth/profile");
  return data.data;
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
