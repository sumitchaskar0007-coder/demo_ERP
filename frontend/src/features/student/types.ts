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

export interface AdminStudentDetails {
  profile: StudentProfileResponse;
  admission?: {
    id: number;
    referenceNumber: string;
    academicYear: string;
    status: string;
    submittedAt?: string;
  };
  academic?: { courseYear: string; division: string; academicYear: string; rollNumber: string };
  attendance: {
    totalLectures: number;
    present: number;
    absent: number;
    late: number;
    leave: number;
    percentage: number;
  };
}
