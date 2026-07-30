import { API_BASE_URL, apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
import type { CreateNoticeRequest, Notice } from "./types";

export async function getNoticeInbox() {
  const { data } = await apiClient.get<ApiResponse<Notice[]>>("/api/notices/inbox");
  return data.data;
}
export async function getUnreadNoticeCount() {
  const { data } = await apiClient.get<ApiResponse<number>>("/api/notices/unread-count");
  return data.data;
}

export function subscribeToNoticeChanges(onRefresh: () => void) {
  let stopped = false;
  let source: EventSource | null = null;
  let retryMs = 1_000;
  let reconnectTimer: number | undefined;

  const connect = () => {
    if (stopped || document.visibilityState === "hidden") return;
    source = new EventSource(`${API_BASE_URL}/api/notices/stream`, { withCredentials: true });
    source.addEventListener("unread-count", onRefresh);
    source.addEventListener("refresh", onRefresh);
    source.onopen = () => {
      retryMs = 1_000;
    };
    source.onerror = () => {
      source?.close();
      source = null;
      if (stopped) return;
      reconnectTimer = window.setTimeout(connect, retryMs);
      retryMs = Math.min(retryMs * 2, 30_000);
    };
  };
  const visibility = () => {
    if (document.visibilityState === "hidden") {
      source?.close();
      source = null;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
    } else {
      retryMs = 1_000;
      connect();
      onRefresh();
    }
  };
  document.addEventListener("visibilitychange", visibility);
  connect();
  return () => {
    stopped = true;
    source?.close();
    if (reconnectTimer) window.clearTimeout(reconnectTimer);
    document.removeEventListener("visibilitychange", visibility);
  };
}
export async function markNoticeInboxSeen() {
  await apiClient.post("/api/notices/inbox/seen");
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
