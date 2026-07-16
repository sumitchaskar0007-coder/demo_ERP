export type StaffType =
  | "STUDENT_SECTION"
  | "FEE_SECTION"
  | "HOD"
  | "TEACHER"
  | "CLASS_TEACHER"
  | "SUBJECT_TEACHER"
  | "GENERAL_STAFF";
export type StaffStatus = "ACTIVE" | "INACTIVE";

export interface CreateStudentSectionStaffRequest {
  collegeId: number;
  fullName: string;
  email: string;
  phone: string;
  joiningDate?: string;
}
export type CreateFeeSectionStaffRequest = CreateStudentSectionStaffRequest;
export interface CreateStaffRequest {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
  departmentId?: number;
  staffType?: StaffType;
  departmentIds?: number[];
  staffTypes?: StaffType[];
  joiningDate?: string;
}
export interface StaffResponse {
  id: number;
  userId: number;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId?: number | null;
  departmentName?: string | null;
  departmentCode?: string | null;
  departmentIds: number[];
  departmentNames: string[];
  employeeCode: string;
  fullName: string;
  email: string;
  phone?: string | null;
  staffType: StaffType;
  staffTypes: StaffType[];
  status: StaffStatus;
  roles: string[];
  joiningDate?: string | null;
  assignedClassTeacherDivisions: string[];
  createdAt: string;
  updatedAt: string;
}
