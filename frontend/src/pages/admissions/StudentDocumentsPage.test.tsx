import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";
import { StudentDocumentsPage } from "./StudentDocumentsPage";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => ({ isRole: () => false }),
}));

vi.mock("@/features/admissions/api", () => ({
  downloadAdmissionDocument: vi.fn(),
  getAdmissionDocumentRequirements: vi.fn(),
  getStudentSectionAdmission: vi.fn(),
  searchStudentSectionAdmissions: vi.fn(),
}));

import * as admissionApi from "@/features/admissions/api";

const admission = {
  id: 10014,
  admissionReferenceNumber: "ADM-101-2026-044152",
  admissionNumber: "STU-10014",
  fullName: "Sai Dada Dhasgude",
  email: "student@example.com",
  phone: "9876543210",
  departmentCode: "MBA",
  departmentName: "Management",
  courseYearDisplayName: null,
  status: "SUBMITTED",
  uploadedDocuments: ["PROVISIONAL_CERTIFICATE"],
} as StudentSectionAdmissionResponse;

describe("StudentDocumentsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(admissionApi.searchStudentSectionAdmissions).mockResolvedValue({
      content: [admission],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue(admission);
    vi.mocked(admissionApi.getAdmissionDocumentRequirements).mockResolvedValue([
      {
        id: 3,
        departmentId: 30,
        departmentName: "Management",
        documentKey: "PROVISIONAL_CERTIFICATE",
        documentName: "Provisional Certificate",
        required: true,
        active: true,
        displayOrder: 1,
      },
    ]);
  });

  it("shows admission students before division allocation and opens uploaded documents", async () => {
    render(
      <MemoryRouter>
        <StudentDocumentsPage />
      </MemoryRouter>,
    );

    const student = await screen.findByRole("button", { name: /Sai Dada Dhasgude/i });
    expect(admissionApi.searchStudentSectionAdmissions).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: undefined, page: 0 }),
    );

    fireEvent.click(student);

    expect(await screen.findByText("Provisional Certificate")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "View" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Download" })).toBeInTheDocument();
  });
});
