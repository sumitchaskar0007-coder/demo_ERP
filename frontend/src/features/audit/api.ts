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

export interface BusinessActivityRow {
  id: number;
  createdAt: string;
  userId?: number;
  user: string;
  role: string;
  departmentId?: number;
  department: string;
  module: string;
  action: string;
  title: string;
  description: string;
  status: string;
  affectedRecords: string;
  remarks?: string;
  previousValue?: string;
  newValue?: string;
}
export interface TeacherEngagementSummary {
  totalTeachers: number;
  loggedInToday: number;
  notLoggedInToday: number;
  scheduledLecturesToday: number;
  attendanceCompletedToday: number;
  attendanceRemainingToday: number;
  lowUsageTeachers: number;
}
export interface TeacherEngagementRow {
  staffId: number;
  userId: number;
  teacher: string;
  employeeCode: string;
  department: string;
  role: string;
  lastLoginAt?: string | null;
  loginDaysLast7: number;
  loggedInToday: boolean;
  scheduledLectures: number;
  attendanceSubmitted: number;
  attendanceRemaining: number;
  attendanceDraft: number;
  usageStatus: "REGULAR" | "ACTIVE_TODAY" | "LOW_USAGE" | "INACTIVE_7_DAYS";
  attendanceStatus: "COMPLETED" | "PARTIAL" | "PENDING" | "NO_LECTURES";
}
export interface BusinessActivityDashboard {
  summary: {
    todayActivities: number;
    attendanceActivities: number;
    admissionActivities: number;
    feeActivities: number;
    criticalChanges: number;
    pendingApprovals: number;
  };
  timeline: BusinessActivityRow[];
  modules: { module: string; today: number; week: number; month: number }[];
  departments: {
    departmentId?: number;
    department: string;
    activities: number;
    lastActivity: string;
    mostActiveUser: string;
    status: string;
  }[];
  trend: { date: string; activities: number }[];
  actionDistribution: { label: string; value: number }[];
  alerts: { key: string; label: string; count: number; module: string; action: string }[];
  insights: string[];
  teacherSummary: TeacherEngagementSummary;
  teachers: TeacherEngagementRow[];
  rows: BusinessActivityRow[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
export const getBusinessActivityDashboard = (params?: object) =>
  apiClient
    .get<ApiResponse<BusinessActivityDashboard>>("/api/audit-logs/dashboard", { params })
    .then((r) => r.data.data);
