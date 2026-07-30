import type { EntityStatus } from "@/types/common";

export interface Department {
  id: number;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  name: string;
  code: string;
  description: string | null;
  admissionFormFee: number;
  status: EntityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentFormValues {
  collegeId?: number;
  name: string;
  code?: string;
  description: string;
  admissionFormFee: number;
}

export interface DepartmentSearchParams {
  keyword?: string;
  collegeId?: number;
  status?: EntityStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
