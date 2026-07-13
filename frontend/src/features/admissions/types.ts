export type AdmissionStatus =
  | "SUBMITTED"
  | "STUDENT_SECTION_REVIEW_PENDING"
  | "STUDENT_SECTION_APPROVED"
  | "STUDENT_SECTION_REJECTED"
  | "PRINCIPAL_REVIEW_PENDING"
  | "PRINCIPAL_APPROVED"
  | "PRINCIPAL_REJECTED"
  | "CANCELLED";

export type AdmissionAction =
  | "SUBMITTED"
  | "STUDENT_SECTION_REVIEW_STARTED"
  | "STUDENT_SECTION_APPROVED"
  | "STUDENT_SECTION_REJECTED"
  | "ADMISSION_FORM_PRINTED"
  | "STATUS_UPDATED";

export type StudentCategory = "OPEN" | "OBC" | "SC" | "ST" | "SBC" | "VJNT" | "EWS" | "OTHER";

export interface AdmissionDepartmentOptionResponse {
  id: number;
  name: string;
  code: string;
}
export interface PublicAdmissionInfoResponse {
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  logoUrl?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  academicYear: string;
  departments: AdmissionDepartmentOptionResponse[];
}
export interface SubmitAdmissionRequest {
  departmentId: number;
  studentCategory: StudentCategory;
  firstName: string;
  middleName?: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
  parentName?: string;
  parentPhone?: string;
  parentEmail?: string;
  previousSchoolName?: string;
  previousClassName?: string;
  previousPercentage?: number | "";
}
export interface SubmitAdmissionResponse {
  admissionReferenceNumber: string;
  admissionNumber: string;
  status: AdmissionStatus;
  studentUserId: number;
  studentProfileId: number;
  collegeName: string;
  collegeCode: string;
  departmentName: string;
  departmentCode: string;
  studentName: string;
  email: string;
  temporaryPassword: string;
  loginUrl: string;
  message: string;
}
export interface AdmissionResponse {
  id: number;
  admissionReferenceNumber: string;
  admissionNumber: string;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId: number;
  departmentName: string;
  departmentCode: string;
  academicYear: string;
  studentCategory: StudentCategory;
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  pincode?: string | null;
  parentName: string;
  parentPhone: string;
  parentEmail?: string | null;
  previousSchoolName?: string | null;
  previousClassName?: string | null;
  previousPercentage?: number | null;
  status: AdmissionStatus;
  source: string;
  submittedAt: string;
  createdAt: string;
  updatedAt: string;
  rejectionReason?: string | null;
}
export interface StudentSectionAdmissionResponse extends AdmissionResponse {
  studentSectionVerifiedAt?: string | null;
  studentSectionVerifiedByName?: string | null;
  studentSectionRemarks?: string | null;
  studentSectionRejectedAt?: string | null;
  studentSectionRejectedByName?: string | null;
  lastPrintedAt?: string | null;
  lastPrintedByName?: string | null;
  printCount: number;
}
export interface AdmissionStatusHistoryResponse {
  id: number;
  admissionId: number;
  admissionReferenceNumber: string;
  oldStatus?: AdmissionStatus | null;
  newStatus: AdmissionStatus;
  action: AdmissionAction;
  remarks?: string | null;
  changedByUserId?: number | null;
  changedByName?: string | null;
  createdAt: string;
}
export interface AdmissionPrintCollegeSection {
  collegeName: string;
  collegeCode: string;
  logoUrl?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
}
export interface AdmissionPrintStudentSection {
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  pincode?: string | null;
}
export interface AdmissionPrintParentSection {
  parentName: string;
  parentPhone: string;
  parentEmail?: string | null;
}
export interface AdmissionPrintAcademicSection {
  academicYear: string;
  departmentName: string;
  departmentCode: string;
  previousSchoolName?: string | null;
  previousClassName?: string | null;
  previousPercentage?: number | null;
}
export interface AdmissionPrintResponse {
  admissionId: number;
  admissionReferenceNumber: string;
  admissionNumber: string;
  generatedAt: string;
  printCount: number;
  college: AdmissionPrintCollegeSection;
  student: AdmissionPrintStudentSection;
  parent: AdmissionPrintParentSection;
  academic: AdmissionPrintAcademicSection;
  verification: {
    status: AdmissionStatus;
    verifiedAt?: string | null;
    verifiedByName?: string | null;
    remarks?: string | null;
  };
  declarations: string[];
  signatureLabels: string[];
}
