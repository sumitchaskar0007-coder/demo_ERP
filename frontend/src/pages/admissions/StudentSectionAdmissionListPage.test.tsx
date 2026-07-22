import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { StudentSectionAdmissionResponse } from "@/features/admissions/types";
import { StudentSectionAdmissionListPage } from "./StudentSectionAdmissionListPage";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => ({ isRole: (roles: string[]) => roles.includes("STUDENT_SECTION") }),
}));

vi.mock("@/features/admissions/api", () => ({
  searchStudentSectionAdmissions: vi.fn(),
}));

import * as admissionApi from "@/features/admissions/api";

const admission = {
  id: 6,
  admissionReferenceNumber: "ADM-6",
  admissionNumber: "STU-6",
  fullName: "Test Student",
  email: "student@example.com",
  phone: "9876543210",
  departmentCode: "BCA",
  submittedAt: "2026-07-22T10:00:00Z",
  printCount: 0,
} as StudentSectionAdmissionResponse;

function showAdmission(status: StudentSectionAdmissionResponse["status"]) {
  vi.mocked(admissionApi.searchStudentSectionAdmissions).mockResolvedValue({
    content: [{ ...admission, status }],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    last: true,
  });
  render(
    <MemoryRouter>
      <StudentSectionAdmissionListPage />
    </MemoryRouter>,
  );
}

describe("student section admission actions", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows only Review for a submitted admission", async () => {
    showAdmission("SUBMITTED");

    expect(await screen.findByRole("button", { name: "Review" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "View" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Print" })).not.toBeInTheDocument();
  });

  it("shows View and Print after student section approval", async () => {
    showAdmission("STUDENT_SECTION_APPROVED");

    expect(await screen.findByRole("button", { name: "View" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Print" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Review" })).not.toBeInTheDocument();
  });

  it("shows View and Print for every later state", async () => {
    showAdmission("PRINCIPAL_APPROVED");

    expect(await screen.findByRole("button", { name: "View" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Print" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Review" })).not.toBeInTheDocument();
  });
});
