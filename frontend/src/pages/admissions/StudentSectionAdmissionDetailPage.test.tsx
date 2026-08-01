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

describe("StudentSectionAdmissionDetailPage", () => {
  beforeEach(() => {
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue(admission);
    vi.mocked(admissionApi.getStudentSectionAdmissionFees).mockResolvedValue({
      account: null,
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
});
