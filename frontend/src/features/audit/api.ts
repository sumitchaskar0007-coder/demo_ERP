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

export interface BusinessActivityRow { id:number;createdAt:string;userId?:number;user:string;role:string;departmentId?:number;department:string;module:string;action:string;title:string;description:string;status:string;affectedRecords:string;remarks?:string;previousValue?:string;newValue?:string }
export interface BusinessActivityDashboard {
  summary:{todayActivities:number;attendanceActivities:number;admissionActivities:number;feeActivities:number;criticalChanges:number;pendingApprovals:number};
  timeline:BusinessActivityRow[]; modules:{module:string;today:number;week:number;month:number}[];
  departments:{departmentId?:number;department:string;activities:number;lastActivity:string;mostActiveUser:string;status:string}[];
  trend:{date:string;activities:number}[]; actionDistribution:{label:string;value:number}[];
  alerts:{key:string;label:string;count:number;module:string;action:string}[]; insights:string[];
  rows:BusinessActivityRow[];totalElements:number;totalPages:number;page:number;size:number;
}
export const getBusinessActivityDashboard = (params?:object) => apiClient
  .get<ApiResponse<BusinessActivityDashboard>>("/api/audit-logs/dashboard",{params})
  .then(r=>r.data.data);
