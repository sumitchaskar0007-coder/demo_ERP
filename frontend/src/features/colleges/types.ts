import type { EntityStatus } from "@/types/common";

export interface College {
  id: number;
  name: string;
  code: string;
  address: string | null;
  city: string | null;
  state: string | null;
  pincode: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  logoUrl: string | null;
  qrCodeUrl: string | null;
  status: EntityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentQrSettings {
  collegeId: number;
  collegeName: string;
  qrCodeUrl: string | null;
}

export interface CollegeFormValues {
  name: string;
  code?: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  contactEmail: string;
  contactPhone: string;
  logoUrl: string;
  qrCodeUrl: string;
}

export interface CollegeSearchParams {
  keyword?: string;
  status?: EntityStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
