export type TimetableStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type LectureType = "THEORY" | "PRACTICAL" | "LAB" | "TUTORIAL" | "BREAK" | "LECTURE";
export type TimetableDay = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY";

export interface PeriodInfo {
  id: number;
  periodNumber: number;
  startTime: string;
  endTime: string;
  type: string;
}

export interface SubjectInfo {
  id: number;
  name: string;
  code: string;
  subjectType?: string;
}

export interface TeacherInfo {
  id: number;
  fullName: string;
  email?: string;
}

export interface RoomInfo {
  id: number;
  code: string;
  name: string;
  capacity: number;
  type: string;
}

export interface TimetableEntry {
  id: number;
  dayOfWeek: TimetableDay;
  periodNumber: number;
  startTime: string;
  endTime: string;
  type: LectureType;
  subjectId?: number;
  subject?: string;
  teacherId?: number;
  teacher?: string;
  roomId?: number;
  room?: string;
  remarks?: string;
  createdBy?: number;
  createdByName?: string;
}

export interface Timetable {
  id: number;
  academicYear: string;
  term: string;
  classId: number;
  className: string;
  sectionId: number;
  section: string;
  weekStart: string;
  status: TimetableStatus;
  entries: TimetableEntry[];
}

export interface SectionInfo {
  sectionId: number;
  classId: number;
  courseYear: string;
  yearName: string;
  division: string;
  divisionCode: string;
  academicYear: string;
  collegeId: number;
  departmentId: number;
  departmentName: string;
  academicYearId?: number;
  academicYearName?: string;
  academicTermId?: number;
  academicTermName?: string;
}
