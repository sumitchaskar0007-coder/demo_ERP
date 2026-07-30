export type AcademicStatus = "ACTIVE" | "INACTIVE";
export type SubjectType = "THEORY" | "PRACTICAL" | "OTHER";
export type CourseYearName =
  | "FIRST_YEAR"
  | "SECOND_YEAR"
  | "THIRD_YEAR"
  | "FOURTH_YEAR"
  | "FIFTH_YEAR";
export interface CourseYear {
  id: number;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId: number;
  departmentName: string;
  departmentCode: string;
  academicYear: string;
  yearName: CourseYearName;
  displayName: string;
  code: string;
  status: AcademicStatus;
  divisionCount: number;
  createdAt: string;
  updatedAt: string;
}
export interface Division {
  id: number;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId: number;
  departmentName: string;
  departmentCode: string;
  courseYearId: number;
  courseYearName: CourseYearName;
  courseYearDisplayName: string;
  academicYear: string;
  name: string;
  code: string;
  capacity: number;
  classTeacherId?: number | null;
  classTeacherName?: string | null;
  classTeacherEmail?: string | null;
  status: AcademicStatus;
  createdAt: string;
  updatedAt: string;
}
export type TimetableDay =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";
export type AttendanceStatus = "PRESENT" | "ABSENT";
export interface AcademicClass {
  id: number;
  college: { id: number; name: string };
  department: { id: number; name: string };
  academicYear: string;
  yearName: CourseYearName;
  name: string;
  code: string;
  description?: string;
  status: AcademicStatus;
}
export interface Section {
  id: number;
  academicClass: AcademicClass;
  academicYear: string;
  name: string;
  code: string;
  capacity: number;
  status: AcademicStatus;
  classTeacher?: { id: number; fullName: string };
}
export interface Subject {
  id: number;
  academicClass: AcademicClass;
  academicYear: string;
  name: string;
  code: string;
  description?: string;
  credits?: number;
  subjectType?: SubjectType;
  status: AcademicStatus;
}
export interface TimetableEntry {
  id: number;
  section: Section;
  subject: Subject;
  teacher: { id: number; fullName: string };
  dayOfWeek: TimetableDay;
  startTime: string;
  endTime: string;
  roomNumber?: string;
  status: AcademicStatus;
}
export interface SubjectTeacherAssignment {
  id: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  teacherId: number;
  teacherName: string;
  academicYear: string;
}
