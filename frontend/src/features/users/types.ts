import type { EntityStatus } from "@/types/common";

export interface User {
  id: number;
  collegeId: number | null;
  collegeName: string | null;
  collegeCode: string | null;
  fullName: string;
  email: string;
  phone: string | null;
  status: EntityStatus;
  roles: string[];
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePrincipalValues {
  collegeId: number;
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface UserSearchParams {
  keyword?: string;
  collegeId?: number;
  role?: string;
  status?: EntityStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
