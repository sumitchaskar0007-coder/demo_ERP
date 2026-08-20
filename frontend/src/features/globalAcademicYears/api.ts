import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";

export type GlobalAcademicYearStatus = "DRAFT" | "ACTIVE" | "CLOSED";

export interface GlobalAcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: GlobalAcademicYearStatus;
  activatedAt: string | null;
  attachedColleges: number;
}

export interface SaveGlobalAcademicYear {
  name: string;
  startDate: string;
  endDate: string;
}

const unwrap = <T>(request: Promise<{ data: ApiResponse<T> }>) => request.then((r) => r.data.data);

export const globalAcademicYearApi = {
  active: () =>
    unwrap(
      apiClient.get<ApiResponse<GlobalAcademicYear | null>>("/api/global-academic-years/active"),
    ),
  list: () =>
    unwrap(apiClient.get<ApiResponse<GlobalAcademicYear[]>>("/api/super-admin/academic-years")),
  create: (body: SaveGlobalAcademicYear) =>
    unwrap(
      apiClient.post<ApiResponse<GlobalAcademicYear>>("/api/super-admin/academic-years", body),
    ),
  update: (id: number, body: SaveGlobalAcademicYear) =>
    unwrap(
      apiClient.put<ApiResponse<GlobalAcademicYear>>(`/api/super-admin/academic-years/${id}`, body),
    ),
  activate: (id: number) =>
    unwrap(
      apiClient.post<ApiResponse<GlobalAcademicYear>>(
        `/api/super-admin/academic-years/${id}/activate`,
      ),
    ),
};
