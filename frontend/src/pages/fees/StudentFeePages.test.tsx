import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { PaymentResponse, StudentFeeAccountResponse } from "@/features/fees/types";
import { StudentFeesPage } from "./StudentFeePages";

vi.mock("@/features/fees/api", () => ({
  getMyFeeAccount: vi.fn(),
  getMyPayments: vi.fn(),
  getMyPaymentReceipt: vi.fn(),
}));

vi.mock("@/features/fees/feeReceiptPdf", () => ({
  downloadFeeReceipt: vi.fn(),
}));

import * as feesApi from "@/features/fees/api";

const account: StudentFeeAccountResponse = {
  id: 21,
  studentId: 12,
  studentUserId: 25,
  admissionId: 42,
  admissionReferenceNumber: "ADM-42",
  admissionNumber: "STU-42",
  collegeId: 1,
  collegeName: "Jadhavar College",
  collegeCode: "JCE",
  departmentId: 2,
  departmentName: "MBA",
  departmentCode: "MBA",
  academicYear: "2026-2027",
  studentCategory: "OPEN",
  totalFee: 100000,
  paidAmount: 80000,
  remainingAmount: 0,
  creditAmount: 0,
  discountAmount: 20000,
  scholarshipAmount: 20000,
  scholarshipRemoved: false,
  minimumAmountForAdmission: 5000,
  status: "PAID",
  admissionFeeAccount: false,
  collegeQrAccountName: "Jadhavar College",
  paymentInstructions: "Pay and submit proof.",
  createdAt: "2026-08-01T10:00:00",
  updatedAt: "2026-08-14T10:00:00",
};

const payment = (
  id: number,
  amount: number,
  paymentPurpose: PaymentResponse["paymentPurpose"],
): PaymentResponse => ({
  id,
  feeAccountId: paymentPurpose === "ADMISSION_FORM_FEE" ? 20 : 21,
  paymentPurpose,
  studentId: 12,
  studentName: "Test Student",
  admissionNumber: "STU-42",
  collegeId: 1,
  collegeName: "Jadhavar College",
  departmentId: 2,
  departmentName: "MBA",
  amount,
  paymentMode: "UPI",
  transactionReference: `UTR-${id}`,
  paymentDate: "2026-08-14",
  proofUrl: `proof-${id}.png`,
  status: "VERIFIED",
  submittedAt: "2026-08-14T10:00:00",
  verifiedAt: "2026-08-14T11:00:00",
  verifiedByName: "Fee Officer",
  createdAt: "2026-08-14T10:00:00",
  updatedAt: "2026-08-14T11:00:00",
});

describe("StudentFeesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(feesApi.getMyFeeAccount).mockResolvedValue(account);
    vi.mocked(feesApi.getMyPayments).mockResolvedValue([
      payment(31, 3000, "ADMISSION_FORM_FEE"),
      payment(32, 80000, "COURSE_FEE"),
    ]);
  });

  it("shows verified admission-form and course payments with receipt actions", async () => {
    render(
      <MemoryRouter>
        <StudentFeesPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("Admission Form Fee")).toBeInTheDocument();
    expect(screen.getByText("Course Fee")).toBeInTheDocument();
    expect(screen.getAllByText("VERIFIED")).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: /receipt/i })).toHaveLength(2);
  });
});
