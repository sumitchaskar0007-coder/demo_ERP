import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";
import type { StudentFeeAccountResponse } from "@/features/fees/types";
import { StudentAdmissionPage } from "./StudentAdmissionPage";

vi.mock("@/components/admissions/DetailedAdmissionForm", () => ({
  DetailedAdmissionForm: () => <div>Admission form</div>,
  DetailedAdmissionView: () => <div>Admission view</div>,
}));

vi.mock("@/features/admissions/api", () => ({
  getMyAdmission: vi.fn(),
  getMyAdmissionDocumentRequirements: vi.fn(),
}));

vi.mock("@/features/fees/api", () => ({
  getMyFeeAccount: vi.fn(),
}));

import * as admissionsApi from "@/features/admissions/api";
import * as feesApi from "@/features/fees/api";

const admission = {
  id: 42,
  admissionReferenceNumber: "ADM-REF-42",
  admissionNumber: "ADM-42",
  collegeId: 1,
  collegeName: "Jadhavar College",
  collegeCode: "JCE",
  departmentId: 2,
  departmentName: "Computer Science",
  departmentCode: "CS",
  academicYear: "2026-27",
  studentCategory: "OPEN",
  fullName: "Test Student",
  email: "student@example.com",
  phone: "9876543210",
  dateOfBirth: "2005-01-02",
  gender: "MALE",
  parentName: "Test Parent",
  parentPhone: "9876543211",
  status: "SUBMITTED",
  source: "PUBLIC",
  submittedAt: "2026-07-20T10:00:00Z",
  detailsCompletedAt: "2026-07-20T10:00:00Z",
  createdAt: "2026-07-20T10:00:00Z",
  updatedAt: "2026-07-20T10:00:00Z",
  printCount: 0,
  photoAvailable: false,
  tenthMarksheetAvailable: false,
  twelfthMarksheetAvailable: false,
  graduationPgCertificateAvailable: false,
  leavingCertificateAvailable: false,
  migrationCertificateAvailable: false,
  gapAffidavitAvailable: false,
  casteCertificateAvailable: false,
  incomeProofAvailable: false,
  nameChangeCertificateAvailable: false,
  aadhaarCardAvailable: false,
  photoVerified: false,
  tenthMarksheetVerified: false,
  twelfthMarksheetVerified: false,
  leavingCertificateVerified: false,
  aadhaarCardVerified: false,
  graduationPgCertificateVerified: false,
  migrationCertificateVerified: false,
  gapAffidavitVerified: false,
  casteCertificateVerified: false,
  incomeProofVerified: false,
  nameChangeCertificateVerified: false,
  academicRecords: [],
  uploadedDocuments: [],
} satisfies StudentSectionAdmissionResponse;

const feeAccount = {
  id: 10,
  studentId: 11,
  studentUserId: 12,
  admissionId: 42,
  admissionReferenceNumber: "ADM-REF-42",
  admissionNumber: "ADM-42",
  collegeId: 1,
  collegeName: "Jadhavar College",
  collegeCode: "JCE",
  departmentId: 2,
  departmentName: "Computer Science",
  departmentCode: "CS",
  academicYear: "2026-27",
  studentCategory: "OPEN",
  totalFee: 2200,
  paidAmount: 0,
  remainingAmount: 2200,
  creditAmount: 0,
  discountAmount: 0,
  scholarshipAmount: 0,
  scholarshipRemoved: false,
  minimumAmountForAdmission: 2200,
  status: "PENDING",
  admissionFeeAccount: true,
  collegeQrAccountName: "Jadhavar College",
  paymentInstructions: "Pay and upload proof",
  createdAt: "2026-07-20T10:00:00Z",
  updatedAt: "2026-07-20T10:00:00Z",
} satisfies StudentFeeAccountResponse;

describe("StudentAdmissionPage", () => {
  beforeEach(() => {
    vi.mocked(admissionsApi.getMyAdmission).mockResolvedValue(admission);
    vi.mocked(admissionsApi.getMyAdmissionDocumentRequirements).mockResolvedValue([]);
    vi.mocked(feesApi.getMyFeeAccount).mockResolvedValue(feeAccount);
  });

  it("shows the admission form fee from the student's payable account", async () => {
    render(
      <MemoryRouter>
        <StudentAdmissionPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/Pay the ₹2,200 admission form fee/)).toBeInTheDocument();
    expect(feesApi.getMyFeeAccount).toHaveBeenCalledTimes(1);
    expect(screen.queryByText(/₹1,000 admission form fee/)).not.toBeInTheDocument();
  });
});
