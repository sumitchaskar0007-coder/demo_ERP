export type StaffType = "STUDENT_SECTION" | "FEE_SECTION" | "HOD" | "CLASS_TEACHER" | "SUBJECT_TEACHER" | "GENERAL_STAFF";
export type StaffStatus = "ACTIVE" | "INACTIVE";

export interface CreateStudentSectionStaffRequest {
  collegeId: number; fullName: string; email: string; phone?: string; password: string; joiningDate?: string;
}
export interface StaffResponse {
  id: number; userId: number; collegeId: number; collegeName: string; collegeCode: string; employeeCode: string;
  fullName: string; email: string; phone?: string | null; staffType: StaffType; status: StaffStatus; roles: string[];
  joiningDate?: string | null; createdAt: string; updatedAt: string;
}
