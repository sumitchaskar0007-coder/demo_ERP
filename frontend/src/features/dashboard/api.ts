import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
export type DashboardData = Record<string, string | number | object[]>;
const get = (role: string) =>
  apiClient.get<ApiResponse<DashboardData>>(`/api/dashboard/${role}`).then((r) => r.data.data);
export const getSuperAdminDashboard = () => get("super-admin");
export const getPrincipalDashboard = () => get("principal");
export const getStudentSectionDashboard = () => get("student-section");
export const getFeeSectionDashboard = () => get("fee-section");
export const getHodDashboard = () => get("hod");
export const getTeacherDashboard = () => get("teacher");
export const getStudentDashboard = () => get("student");
