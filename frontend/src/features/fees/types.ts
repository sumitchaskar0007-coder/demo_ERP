export type FeeStructureStatus = "ACTIVE" | "INACTIVE";
export type FeeAccountStatus = "PENDING" | "PARTIALLY_PAID" | "PAID" | "OVERDUE" | "CANCELLED";
export type PaymentStatus = "PENDING" | "VERIFIED" | "REJECTED" | "CANCELLED";
export type PaymentMode = "UPI" | "BANK_TRANSFER" | "CASH" | "CHEQUE" | "OTHER";
export type FeeTransactionType =
  | "PAYMENT_VERIFIED"
  | "PAYMENT_REJECTED"
  | "DISCOUNT_APPLIED"
  | "SCHOLARSHIP_APPROVED"
  | "SCHOLARSHIP_REMOVED"
  | "FEE_CATEGORY_CHANGED"
  | "FEE_ADJUSTMENT"
  | "REFUND";
export interface CreateFeeStructureRequest {
  collegeId: number;
  departmentId: number;
  academicYear: string;
  courseYear?: string;
  studentCategory?: "OPEN" | "OBC" | "SC" | "ST" | "SBC" | "VJNT" | "EWS" | "OTHER";
  customCategoryName?: string;
  gender: "MALE" | "FEMALE";
  title: string;
  description?: string;
  totalFee: number;
  scholarshipAmount: number;
  minimumAmountForAdmission: number;
  admissionFee?: number;
  tuitionFee?: number;
  examFee?: number;
  libraryFee?: number;
  otherFee?: number;
}
export interface UpdateFeeStructureRequest
  extends Omit<CreateFeeStructureRequest, "collegeId" | "departmentId" | "academicYear"> {
  status?: FeeStructureStatus;
}
export interface FeeStructureResponse extends CreateFeeStructureRequest {
  id: number;
  collegeName: string;
  collegeCode: string;
  departmentName: string;
  departmentCode: string;
  status: FeeStructureStatus;
  payableFee: number;
  categoryLabel: string;
  createdAt: string;
  updatedAt: string;
}
export interface StudentFeeAccountResponse {
  id: number;
  studentId: number;
  studentUserId: number;
  admissionId: number;
  admissionReferenceNumber: string;
  admissionNumber: string;
  collegeId: number;
  collegeName: string;
  collegeCode: string;
  departmentId: number;
  departmentName: string;
  departmentCode: string;
  academicYear: string;
  studentCategory: "OPEN" | "OBC" | "SC" | "ST" | "SBC" | "VJNT" | "EWS" | "OTHER";
  customCategoryName?: string | null;
  totalFee: number;
  paidAmount: number;
  remainingAmount: number;
  creditAmount: number;
  discountAmount: number;
  scholarshipAmount: number;
  scholarshipRemoved: boolean;
  scholarshipRemovedAt?: string | null;
  scholarshipRemovalReason?: string | null;
  minimumAmountForAdmission: number;
  status: FeeAccountStatus;
  admissionFeeAccount: boolean;
  collegeQrCodeUrl?: string | null;
  collegeQrAccountName: string;
  paymentInstructions: string;
  createdAt: string;
  updatedAt: string;
}
export interface SubmitPaymentRequest {
  amount: number;
  paymentMode: PaymentMode;
  transactionReference: string;
  paymentDate: string;
  remarks?: string;
}
export interface PaymentResponse {
  id: number;
  feeAccountId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  collegeId: number;
  collegeName: string;
  departmentId: number;
  departmentName: string;
  amount: number;
  paymentMode: PaymentMode;
  transactionReference: string;
  paymentDate: string;
  proofUrl: string;
  remarks?: string | null;
  status: PaymentStatus;
  submittedAt: string;
  verifiedAt?: string | null;
  verifiedByName?: string | null;
  rejectedAt?: string | null;
  rejectedByName?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface FeeReceiptResponse {
  paymentId: number;
  receiptNumber: string;
  receiptTitle: string;
  issuedOn: string;
  collegeId: number;
  collegeCode: string;
  collegeName: string;
  collegeLogoUrl?: string | null;
  collegeAddress?: string | null;
  collegeCity?: string | null;
  collegeState?: string | null;
  collegePincode?: string | null;
  collegeContactEmail?: string | null;
  collegeContactPhone?: string | null;
  studentName: string;
  admissionNumber: string;
  prn?: string | null;
  departmentName: string;
  courseName: string;
  academicYear: string;
  paymentDate: string;
  amount: number;
  paymentMode: PaymentMode;
  transactionReference: string;
  verifiedAt: string;
  verifiedByName?: string | null;
  remark: string;
}
export interface AdmissionFeeSummaryResponse {
  account: StudentFeeAccountResponse | null;
  payments: PaymentResponse[];
}
export interface FeeTransactionResponse {
  id: number;
  feeAccountId: number;
  paymentId?: number | null;
  transactionType: FeeTransactionType;
  amount: number;
  previousPaidAmount?: number | null;
  newPaidAmount?: number | null;
  previousRemainingAmount?: number | null;
  newRemainingAmount?: number | null;
  remarks?: string | null;
  performedByName?: string | null;
  createdAt: string;
}
export interface FeeDashboardResponse {
  totalFeeAccounts: number;
  pendingPayments: number;
  verifiedPayments: number;
  rejectedPayments: number;
  totalCollectedAmount: number;
  totalPendingAmount: number;
  todayCollectedAmount: number;
}
