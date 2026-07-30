export type NoticeRole =
  | "SUPER_ADMIN"
  | "ADMIN"
  | "PRINCIPAL"
  | "HOD"
  | "STUDENT_SECTION"
  | "FEE_SECTION"
  | "CLASS_TEACHER"
  | "SUBJECT_TEACHER"
  | "GENERAL_STAFF"
  | "STUDENT";
export type NoticePriority = "NORMAL" | "HIGH" | "URGENT";
export type NoticeDeliveryMode = "COMMON" | "INDIVIDUAL";

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
  deliveryMode: NoticeDeliveryMode;
  recipientCount: number;
  recipientNames: string[];
  actionPath: string | null;
  createdAt: string;
}

export interface CreateNoticeRequest {
  title: string;
  message: string;
  priority: NoticePriority;
  audienceRoles: NoticeRole[];
  collegeIds: number[];
  departmentId: number | null;
  deliveryMode: NoticeDeliveryMode;
  recipientUserIds: number[];
}

export interface NoticeRecipientOption {
  userId: number;
  fullName: string;
  email: string;
  collegeId: number | null;
  collegeName: string | null;
  departmentId: number | null;
  departmentName: string | null;
  roles: NoticeRole[];
}

export interface NoticeReceipt {
  noticeId: number;
  deliveryMode: NoticeDeliveryMode;
  recipientCount: number;
  seenCount: number;
  recipients: Array<{
    userId: number;
    fullName: string;
    email: string;
    seen: boolean;
    seenAt: string | null;
  }>;
}
