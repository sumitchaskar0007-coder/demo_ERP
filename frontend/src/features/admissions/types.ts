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
export interface AcademicRecord {
  qualification: "10TH" | "12TH" | "DIPLOMA" | "GRADUATION";
  instituteName?: string | null;
  boardUniversity?: string | null;
  yearOfPassing?: string | null;
  marksPercentage?: number | null;
}

export interface DetailedAdmissionRequest {
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  placeOfBirth: string;
  maritalStatus: string;
  aadhaarNumber: string;
  apaarId?: string;
  nationality: string;
  religion: string;
  caste: string;
  studentCategory: StudentCategory;
  parentName: string;
  parentPhone: string;
  parentEmail?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  pincode: string;
  state: string;
  permanentPhone?: string;
  permanentEmail?: string;
  correspondenceAddress: string;
  correspondenceCity: string;
  correspondencePincode: string;
  correspondenceState: string;
  correspondencePhone?: string;
  correspondenceMobile?: string;
  correspondenceEmail?: string;
  academicRecords: AcademicRecord[];
  qualifyingEntranceSeatNumber?: string;
  qualifyingEntranceTotalScore?: number;
  lastGraduationCollegeName?: string;
  lastGraduationCollegeAddress?: string;
}

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
  photoAvailable: boolean;
  placeOfBirth?: string | null;
  maritalStatus?: string | null;
  aadhaarNumber?: string | null;
  apaarId?: string | null;
  nationality?: string | null;
  religion?: string | null;
  caste?: string | null;
  permanentPhone?: string | null;
  permanentEmail?: string | null;
  correspondenceAddress?: string | null;
  correspondenceCity?: string | null;
  correspondencePincode?: string | null;
  correspondenceState?: string | null;
  correspondencePhone?: string | null;
  correspondenceMobile?: string | null;
  correspondenceEmail?: string | null;
  academicRecords: AcademicRecord[];
  qualifyingEntranceSeatNumber?: string | null;
  qualifyingEntranceTotalScore?: number | null;
  lastGraduationCollegeName?: string | null;
  lastGraduationCollegeAddress?: string | null;
  detailsCompletedAt?: string | null;
  principalApprovedAt?: string | null;
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
  hasPhoto: boolean;
  placeOfBirth?: string | null;
  maritalStatus?: string | null;
  aadhaarNumber?: string | null;
  apaarId?: string | null;
  nationality?: string | null;
  religion?: string | null;
  caste?: string | null;
  permanentPhone?: string | null;
  permanentEmail?: string | null;
  correspondenceAddress?: string | null;
  correspondenceCity?: string | null;
  correspondencePincode?: string | null;
  correspondenceState?: string | null;
  correspondencePhone?: string | null;
  correspondenceMobile?: string | null;
  correspondenceEmail?: string | null;
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
  academicRecords: AcademicRecord[];
  qualifyingEntranceSeatNumber?: string | null;
  qualifyingEntranceTotalScore?: number | null;
  lastGraduationCollegeName?: string | null;
  lastGraduationCollegeAddress?: string | null;
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
