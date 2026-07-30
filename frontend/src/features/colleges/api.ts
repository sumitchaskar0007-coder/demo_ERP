import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { College, CollegeFormValues, CollegeSearchParams, PaymentQrSettings } from "./types";

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

export async function uploadCollegeImage(file: File, kind: "logo" | "qr-code") {
  const body = new FormData();
  body.append("file", file);
  const { data } = await apiClient.post<ApiResponse<{ url: string }>>(
    `${BASE}/images/${kind}`,
    body,
    {
      headers: { "Content-Type": "multipart/form-data" },
    },
  );
  return data.data.url;
}

export async function getPaymentQrSettings(collegeId?: number) {
  const { data } = await apiClient.get<ApiResponse<PaymentQrSettings>>(
    "/api/college-settings/payment-qr",
    { params: collegeId ? { collegeId } : undefined },
  );
  return data.data;
}

export async function updatePaymentQr(file: File, accountName: string, collegeId?: number) {
  const body = new FormData();
  body.append("file", file);
  body.append("accountName", accountName);
  const { data } = await apiClient.post<ApiResponse<PaymentQrSettings>>(
    "/api/college-settings/payment-qr",
    body,
    {
      params: collegeId ? { collegeId } : undefined,
      headers: { "Content-Type": "multipart/form-data" },
    },
  );
  return data.data;
}
