export type StudentStatus =
  | "ADMISSION_SUBMITTED"
  | "UNDER_REVIEW"
  | "ADMISSION_APPROVED"
  | "ADMISSION_REJECTED"
  | "ACTIVE"
  | "INACTIVE";

export interface StudentProfileResponse {
  id: number;
  userId: number;
  admissionNumber: string;
  studentCategory: import("@/features/admissions/types").StudentCategory;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId: number;
  departmentName: string;
  departmentCode: string;
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  parentName: string;
  parentPhone: string;
  status: StudentStatus;
  createdAt: string;
  updatedAt: string;
}
