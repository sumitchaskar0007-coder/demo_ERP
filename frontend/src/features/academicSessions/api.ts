import { apiClient } from "@/lib/apiClient";
import type { ApiResponse } from "@/types/api";

export type TermType = "ODD" | "EVEN";
export type TermStatus = "PLANNED" | "ACTIVE" | "CLOSED";

export interface AcademicTerm {
  id: number;
  academicYearId: number;
  name: string;
  termType: TermType;
  startDate: string;
  endDate: string;
  status: TermStatus;
}

export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: "DRAFT" | "ACTIVE" | "CLOSED";
  terms: AcademicTerm[];
}

export interface AcademicContext {
  academicYearId: number | null;
  academicYear: string | null;
  academicTermId: number | null;
  termName: string | null;
  termType: TermType | null;
  termStartDate: string | null;
  termEndDate: string | null;
  transitionDue: boolean;
}

export interface RolloverPreview {
  sourceTermId: number;
  targetTermId: number;
  totalStudents: number;
  promotableStudents: number;
  graduatingStudents: number;
  blockedStudents: number;
  targetDivisionMappingRequired: boolean;
  students: Array<{
    enrollmentId: number;
    studentId: number;
    studentName: string;
    sourceDivision: string;
    sourceSemester: number;
    targetSemester: number | null;
    decision: "PROMOTE" | "GRADUATE" | "BLOCKED";
    reason: string | null;
  }>;
}

const unwrap = <T>(request: Promise<{ data: ApiResponse<T> }>) => request.then((r) => r.data.data);

export const academicSessionApi = {
  context: () => unwrap(apiClient.get<ApiResponse<AcademicContext>>("/api/academic-sessions/context")),
  years: () =>
    unwrap(apiClient.get<ApiResponse<AcademicYear[]>>("/api/principal/academic-sessions/years")),
  createYear: (body: {
    name: string;
    startDate: string;
    endDate: string;
    oddTerm: { startDate: string; endDate: string };
    evenTerm: { startDate: string; endDate: string };
  }) => unwrap(apiClient.post<ApiResponse<AcademicYear>>("/api/principal/academic-sessions/years", body)),
  activateYear: (id: number) =>
    unwrap(apiClient.post<ApiResponse<AcademicYear>>(`/api/principal/academic-sessions/years/${id}/activate`)),
  updateTerm: (term: AcademicTerm) =>
    unwrap(apiClient.put<ApiResponse<AcademicTerm>>(`/api/principal/academic-sessions/terms/${term.id}`, {
      name: term.name,
      startDate: term.startDate,
      endDate: term.endDate,
    })),
  activateTerm: (id: number, overrideDate = false, reason?: string) =>
    unwrap(apiClient.post<ApiResponse<AcademicTerm>>(`/api/principal/academic-sessions/terms/${id}/activate`, {
      overrideDate,
      reason: reason || null,
    })),
  configureSemesters: (departmentId: number, durationYears: number) =>
    unwrap(apiClient.post<ApiResponse<unknown[]>>("/api/principal/academic-sessions/semesters/configure", {
      departmentId,
      durationYears,
    })),
  preview: (sourceTermId: number, targetTermId: number) =>
    unwrap(apiClient.get<ApiResponse<RolloverPreview>>("/api/principal/academic-sessions/rollover/preview", {
      params: { sourceTermId, targetTermId },
    })),
  rollover: (sourceTermId: number, targetTermId: number, holdStudentIds: number[] = []) =>
    unwrap(apiClient.post<ApiResponse<unknown>>("/api/principal/academic-sessions/rollover", {
      sourceTermId,
      targetTermId,
      holdStudentIds,
      targetSectionBySourceSection: {},
      confirmation: "PROMOTE",
    })),
};
