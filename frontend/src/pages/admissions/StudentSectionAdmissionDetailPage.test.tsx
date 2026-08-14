import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";
import { StudentSectionAdmissionDetailPage } from "./StudentSectionAdmissionDetailPage";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => ({ isRole: () => true }),
}));

vi.mock("@/features/admissions/api", () => ({
  getStudentSectionAdmission: vi.fn(),
  getPublicAdmissionCategories: vi.fn(),
  getStudentSectionAdmissionFees: vi.fn(),
  getAdmissionHistory: vi.fn(),
  getAdmissionCourseYears: vi.fn(),
  getAdmissionDocumentRequirements: vi.fn(),
  getDocumentCustody: vi.fn(),
  getAdmissionDocument: vi.fn(),
  getAdmissionPhoto: vi.fn(),
  startAdmissionReview: vi.fn(),
  updateAdmissionDetails: vi.fn(),
  uploadAdmissionPhoto: vi.fn(),
  uploadAdmissionDocument: vi.fn(),
  approveAdmission: vi.fn(),
}));

import * as admissionApi from "@/features/admissions/api";

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
  status: "STUDENT_SECTION_REVIEW_PENDING",
  source: "PUBLIC",
  submittedAt: "2026-07-20T10:00:00Z",
  createdAt: "2026-07-20T10:00:00Z",
  updatedAt: "2026-07-20T10:00:00Z",
  printCount: 0,
  photoAvailable: false,
  tenthMarksheetAvailable: true,
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
  uploadedDocuments: ["TENTH_MARKSHEET"],
  detailsCompletedAt: "2026-07-20T10:00:00Z",
} satisfies StudentSectionAdmissionResponse;

const verifiedAdmissionFeeAccount = {
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
  paidAmount: 2200,
  remainingAmount: 0,
  creditAmount: 0,
  discountAmount: 0,
  scholarshipAmount: 0,
  scholarshipRemoved: false,
  minimumAmountForAdmission: 2200,
  status: "PAID",
  admissionFeeAccount: true,
  collegeQrAccountName: "Jadhavar College",
  paymentInstructions: "Pay and upload proof",
  createdAt: "2026-07-20T10:00:00Z",
  updatedAt: "2026-07-20T10:00:00Z",
} as const;

describe("StudentSectionAdmissionDetailPage", () => {
  beforeEach(() => {
    vi.mocked(admissionApi.getPublicAdmissionCategories).mockResolvedValue([
      { category: "OPEN", label: "Open" },
    ]);
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue(admission);
    vi.mocked(admissionApi.getStudentSectionAdmissionFees).mockResolvedValue({
      account: verifiedAdmissionFeeAccount,
      payments: [],
    });
    vi.mocked(admissionApi.getAdmissionHistory).mockResolvedValue([]);
    vi.mocked(admissionApi.getAdmissionCourseYears).mockResolvedValue([]);
    vi.mocked(admissionApi.getAdmissionDocumentRequirements).mockResolvedValue([]);
    vi.mocked(admissionApi.getDocumentCustody).mockResolvedValue([]);
    vi.mocked(admissionApi.getAdmissionPhoto).mockResolvedValue("blob:test-photo");
  });

  it("starts read-only, shows uploaded documents, and enables editing on request", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId"
            element={<StudentSectionAdmissionDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Test Student")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "10th marksheet" })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Save detailed admission form" }),
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Change Information" }));

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "Save detailed admission form" }),
      ).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Cancel Changes" })).toBeInTheDocument();
  });

  it("shows the available category names after Student Section selects Other", async () => {
    const user = userEvent.setup();
    vi.mocked(admissionApi.getPublicAdmissionCategories).mockResolvedValue([
      { category: "OPEN", label: "OPEN" },
      { category: "OTHER", customCategoryName: "NT-C", label: "NT-C" },
      { category: "OTHER", customCategoryName: "MINORITY", label: "MINORITY" },
    ]);
    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId"
            element={<StudentSectionAdmissionDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: "Change Information" }));
    await user.selectOptions(screen.getByLabelText("Student category"), "OTHER");

    const customCategory = await screen.findByLabelText("Other category");
    expect(customCategory).toHaveValue("");
    expect(screen.getByRole("option", { name: "NT-C" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "MINORITY" })).toBeInTheDocument();
    await user.selectOptions(customCategory, "NT-C");
    expect(customCategory).toHaveValue("NT-C");
  });

  it("preserves the saved Other category when category loading fails", async () => {
    const user = userEvent.setup();
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue({
      ...admission,
      studentCategory: "OTHER",
      customCategoryName: "NT",
      gender: "Male",
    });
    vi.mocked(admissionApi.getPublicAdmissionCategories).mockRejectedValue(
      new Error("Category service unavailable"),
    );
    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId"
            element={<StudentSectionAdmissionDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: "Change Information" }));

    expect(await screen.findByLabelText("Other category")).toHaveValue("NT");
    expect(screen.getByRole("option", { name: "NT" })).toBeInTheDocument();
    expect(screen.getByLabelText("Gender")).toHaveValue("MALE");
    expect(
      screen.queryByText("No Other category is available for this department."),
    ).not.toBeInTheDocument();
  });

  it("approves when all available required documents are verified", async () => {
    const user = userEvent.setup();
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue({
      ...admission,
      photoAvailable: true,
      twelfthMarksheetAvailable: true,
      leavingCertificateAvailable: true,
      aadhaarCardAvailable: true,
      uploadedDocuments: [
        "TENTH_MARKSHEET",
        "TWELFTH_MARKSHEET",
        "PROVISIONAL_CERTIFICATE",
        "TRANSFER_CERTIFICATE",
        "NATIONALITY_CERTIFICATE",
        "DOMICILE_CERTIFICATE",
        "AADHAAR_CARD",
      ],
    });
    vi.mocked(admissionApi.approveAdmission).mockResolvedValue(admission);

    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId"
            element={<StudentSectionAdmissionDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: "Approve" }));
    const checkboxes = screen.getAllByRole("checkbox");
    for (const checkbox of checkboxes) {
      if (!checkbox.hasAttribute("disabled")) await user.click(checkbox);
    }
    await user.click(screen.getAllByRole("button", { name: "Approve" }).at(-1)!);

    await waitFor(() => expect(admissionApi.approveAdmission).toHaveBeenCalledTimes(1));
  });

  it("locks approval for a legacy pending admission without a verified form fee", async () => {
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue({
      ...admission,
      photoAvailable: true,
    });
    vi.mocked(admissionApi.getStudentSectionAdmissionFees).mockResolvedValue({
      account: {
        ...verifiedAdmissionFeeAccount,
        paidAmount: 0,
        remainingAmount: 2200,
        status: "PENDING",
      },
      payments: [],
    });

    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId"
            element={<StudentSectionAdmissionDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(
      await screen.findByText(
        "Approval is locked until Fee Section verifies the ₹2,200 admission form fee.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
  });
});
