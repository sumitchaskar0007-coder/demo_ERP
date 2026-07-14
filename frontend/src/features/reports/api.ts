import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";
export type ReportRow = Record<string, string | number>;
export const getReport = (type: string, params?: object) =>
  apiClient
    .get<ApiResponse<ReportRow[]>>(`/api/reports/${type}`, { params })
    .then((r) => r.data.data);
export async function exportReport(type: string, params?: object) {
  const r = await apiClient.get(`/api/reports/${type}/export`, { params, responseType: "blob" });
  const url = URL.createObjectURL(r.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${type}-report.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
