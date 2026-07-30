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

export type NoticeStreamChange = {
  type: "NOTICE_CREATED" | "NOTICE_DELETED" | "UNREAD_COUNT" | "NOTICE_ACKNOWLEDGED";
  noticeId?: number;
  unreadCount?: number;
};

type NoticeStreamHandlers = {
  onUnreadCount: (count: number) => void;
  onChange: (change: NoticeStreamChange) => void;
  onReconnect: () => void;
};

export function subscribeToNoticeChanges(handlers: NoticeStreamHandlers) {
  let stopped = false;
  let source: EventSource | null = null;
  let reconnectTimer: number | undefined;
  let retryMs = 1_000;

  const scheduleReconnect = () => {
    if (stopped || document.visibilityState === "hidden" || reconnectTimer !== undefined) return;
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = undefined;
      connect();
    }, retryMs);
    retryMs = Math.min(retryMs * 2, 30_000);
  };

  const connect = () => {
    if (stopped || source || document.visibilityState === "hidden") return;
    source = new EventSource(`${API_BASE_URL}/api/notices/stream`, { withCredentials: true });
    source.addEventListener("unread-count", (event) => {
      const count = Number((event as MessageEvent<string>).data);
      if (Number.isSafeInteger(count) && count >= 0) handlers.onUnreadCount(count);
    });
    source.addEventListener("notice-change", (event) => {
      try {
        handlers.onChange(JSON.parse((event as MessageEvent<string>).data) as NoticeStreamChange);
      } catch {
        // A malformed event is ignored; the count-only fallback remains authoritative.
      }
    });
    source.onopen = () => {
      retryMs = 1_000;
    };
    source.onerror = () => {
      source?.close();
      source = null;
      scheduleReconnect();
    };
  };

  const onVisibilityChange = () => {
    if (document.visibilityState === "hidden") {
      source?.close();
      source = null;
      if (reconnectTimer !== undefined) window.clearTimeout(reconnectTimer);
      reconnectTimer = undefined;
      return;
    }
    retryMs = 1_000;
    connect();
    handlers.onReconnect();
  };

  document.addEventListener("visibilitychange", onVisibilityChange);
  connect();

  return () => {
    stopped = true;
    source?.close();
    if (reconnectTimer !== undefined) window.clearTimeout(reconnectTimer);
    document.removeEventListener("visibilitychange", onVisibilityChange);
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
