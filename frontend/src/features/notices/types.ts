export type NoticeRole = "PRINCIPAL" | "HOD" | "STUDENT_SECTION" | "FEE_SECTION" | "CLASS_TEACHER" | "SUBJECT_TEACHER" | "STUDENT";

export interface Notice {
  id: number;
  title: string;
  message: string;
  createdByUserId: number;
  createdByName: string;
  collegeIds: number[];
  collegeNames: string[];
  departmentId: number | null;
  departmentName: string | null;
  audienceRoles: NoticeRole[];
  createdAt: string;
}

export interface CreateNoticeRequest {
  title: string;
  message: string;
  audienceRoles: NoticeRole[];
  collegeIds: number[];
}
