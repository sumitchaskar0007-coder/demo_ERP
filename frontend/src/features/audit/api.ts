import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
export interface AuditLog {
  id: number;
  actorName: string;
  actorEmail?: string;
  actorRoles: string;
  module: string;
  action: string;
  entityType?: string;
  entityId?: number;
  description: string;
  createdAt: string;
}
export const searchAuditLogs = (params?: object) =>
  apiClient
    .get<ApiResponse<PageResponse<AuditLog>>>("/api/audit-logs", { params })
    .then((r) => r.data.data);
