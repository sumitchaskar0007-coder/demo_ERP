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
  phone: string;
  departmentId?: number;
  staffType?: StaffType;
  departmentIds?: number[];
  staffTypes?: StaffType[];
  joiningDate?: string;
}
export interface UpdateStaffAssignmentRequest {
  staffTypes: StaffType[];
  departmentIds: number[];
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

export interface StaffClassAssignment {
  sectionId: number;
  departmentName: string;
  className: string;
  sectionName: string;
  sectionCode: string;
  academicYear: string;
  capacity: number;
}

export interface StaffSubjectAssignment {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  className: string;
  academicYear: string;
  divisions: string[];
}

export interface StaffAttendanceSummary {
  totalSessions: number;
  submittedSessions: number;
  draftSessions: number;
  studentsMarked: number;
  present: number;
  absent: number;
  late: number;
  leave: number;
}

export interface StaffAttendanceSession {
  sessionId: number;
  attendanceDate: string;
  startTime: string;
  endTime: string;
  lectureNumber: number;
  subjectName: string;
  className: string;
  sectionName: string;
  status: "DRAFT" | "SUBMITTED";
  studentsMarked: number;
  present: number;
  absent: number;
  late: number;
  leave: number;
}

export interface StaffDetailResponse {
  staff: StaffResponse;
  classAssignments: StaffClassAssignment[];
  subjectAssignments: StaffSubjectAssignment[];
  attendanceSummary: StaffAttendanceSummary;
  recentAttendance: StaffAttendanceSession[];
}
