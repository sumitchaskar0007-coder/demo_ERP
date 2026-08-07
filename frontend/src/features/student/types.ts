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
  customCategoryName?: string | null;
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

export interface AdminStudentDetails {
  profile: StudentProfileResponse;
  admission?: {
    id: number;
    referenceNumber: string;
    academicYear: string;
    status: string;
    caste?: string | null;
    submittedAt?: string;
  };
  academic?: { courseYear: string; division: string; academicYear: string; rollNumber: string };
  fees?: {
    feeAccountId: number;
    totalFee: number;
    paidAmount: number;
    scholarshipAmount: number;
    remainingAmount: number;
    creditAmount: number;
    minimumAmountForAdmission: number;
    status: string;
    scholarshipRemoved: boolean;
    scholarshipRemovedAt?: string | null;
    scholarshipRemovalReason?: string | null;
  };
  attendance: {
    totalLectures: number;
    present: number;
    absent: number;
    late: number;
    leave: number;
    percentage: number;
  };
}

export interface ScholarshipResponse {
  feeAccountId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  totalFee: number;
  paidAmount: number;
  scholarshipAmount: number;
  remainingAmount: number;
  creditAmount: number;
  scholarshipRemoved: boolean;
  scholarshipRemovedAt?: string | null;
  scholarshipRemovalReason?: string | null;
  status: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
}

export interface FeeCategoryAssessmentOption {
  feeStructureId: number;
  studentCategory: import("@/features/admissions/types").StudentCategory;
  customCategoryName?: string | null;
  label: string;
  gender: "MALE" | "FEMALE";
  totalFee: number;
  scholarshipAmount: number;
  payableFee: number;
}
