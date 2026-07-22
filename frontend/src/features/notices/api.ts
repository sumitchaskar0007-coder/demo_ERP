import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
import type { CreateNoticeRequest, Notice } from "./types";

export async function getNoticeInbox() {
  const { data } = await apiClient.get<ApiResponse<Notice[]>>("/api/notices/inbox");
  return data.data;
}
export async function getSentNotices() {
  const { data } = await apiClient.get<ApiResponse<Notice[]>>("/api/notices/sent");
  return data.data;
}
export async function sendNotice(request: CreateNoticeRequest) {
  const { data } = await apiClient.post<ApiResponse<Notice>>("/api/notices", request);
  return data.data;
}
export async function deleteNotice(id: number) {
  await apiClient.delete(`/api/notices/${id}`);
}
export async function acknowledgeNotice(id: number) {
  await apiClient.post(`/api/notices/${id}/acknowledge`);
}
