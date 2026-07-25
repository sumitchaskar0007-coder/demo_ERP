export type NoticeRole =
  | "PRINCIPAL"
  | "HOD"
  | "STUDENT_SECTION"
  | "FEE_SECTION"
  | "CLASS_TEACHER"
  | "SUBJECT_TEACHER"
  | "STUDENT";
export type NoticePriority = "NORMAL" | "HIGH" | "URGENT";

export interface Notice {
  id: number;
  title: string;
  message: string;
  priority: NoticePriority;
  acknowledged: boolean;
  seen: boolean;
  createdByUserId: number;
  createdByName: string;
  collegeIds: number[];
  collegeNames: string[];
  allColleges: boolean;
  departmentId: number | null;
  departmentName: string | null;
  audienceRoles: NoticeRole[];
  actionPath: string | null;
  createdAt: string;
}

export interface CreateNoticeRequest {
  title: string;
  message: string;
  priority: NoticePriority;
  audienceRoles: NoticeRole[];
  collegeIds: number[];
}
