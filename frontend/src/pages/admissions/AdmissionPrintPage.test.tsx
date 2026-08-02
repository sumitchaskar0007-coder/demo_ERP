import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AdmissionPrintResponse } from "@/features/admissions/types";
import { AdmissionPrintPage } from "./AdmissionPrintPage";

const pdfExportMocks = vi.hoisted(() => ({
  addImage: vi.fn(),
  addPage: vi.fn(),
  save: vi.fn(),
  html2canvas: vi.fn(),
}));

vi.mock("jspdf", () => ({
  default: class MockJsPdf {
    addImage = pdfExportMocks.addImage;
    addPage = pdfExportMocks.addPage;
    save = pdfExportMocks.save;
  },
}));

vi.mock("html2canvas", () => ({
  default: pdfExportMocks.html2canvas,
}));

vi.mock("@/features/admissions/api", () => ({
  getAdmissionPrintData: vi.fn(),
  getStudentSectionAdmission: vi.fn(),
  getAdmissionDocumentRequirements: vi.fn(),
  getDocumentCustody: vi.fn(),
  getAdmissionPhoto: vi.fn(),
  markAdmissionPrinted: vi.fn(),
}));

import * as admissionApi from "@/features/admissions/api";

const printData: AdmissionPrintResponse = {
  admissionId: 42,
  admissionReferenceNumber: "ADM-101-2026-254539",
  admissionNumber: "ADM-42",
  generatedAt: "2026-08-01T10:00:00Z",
  printCount: 0,
  college: {
    collegeName: "Ganesh College",
    collegeCode: "GC",
    address: "Narhe",
    city: "Pune",
    state: "Maharashtra",
    contactEmail: "college@example.com",
    contactPhone: "9876543210",
  },
  student: {
    fullName: "Test Student",
    email: "student@example.com",
    phone: "9876543211",
    dateOfBirth: "2006-02-11",
    gender: "MALE",
    addressLine1: "Test address",
    city: "Pune",
    state: "Maharashtra",
    pincode: "411001",
    hasPhoto: false,
  },
  parent: {
    parentName: "Test Parent",
    parentPhone: "9876543212",
    parentEmail: "parent@example.com",
  },
  academic: {
    academicYear: "2026-2027",
    departmentName: "Computer Science",
    departmentCode: "CS",
    courseYearDisplayName: "First Year",
    academicRecords: [],
    entranceExams: [
      { examName: "MH-CET", result: "92.5 percentile" },
      { examName: "CMAT", result: "Rank 120" },
    ],
    qualifyingEntranceSeatNumber: "CET-42",
    qualifyingEntranceTotalScore: 92,
  },
  verification: {
    status: "STUDENT_SECTION_REVIEW_PENDING",
  },
  declarations: [],
  signatureLabels: [],
};

describe("AdmissionPrintPage PDF layout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(admissionApi.getAdmissionPrintData).mockResolvedValue(printData);
    vi.mocked(admissionApi.getStudentSectionAdmission).mockResolvedValue(null as never);
    vi.mocked(admissionApi.getAdmissionDocumentRequirements).mockResolvedValue([
      {
        id: 1,
        departmentId: 10,
        departmentName: "Computer Science",
        documentKey: "MARKSHEET_10",
        documentName: "10th Marksheet",
        required: true,
        active: true,
        displayOrder: 1,
      },
    ]);
    vi.mocked(admissionApi.getDocumentCustody).mockResolvedValue([]);
    pdfExportMocks.html2canvas.mockResolvedValue({
      width: 1588,
      height: 2246,
      toDataURL: () => "data:image/jpeg;base64,pdf-page",
    });
    Object.defineProperty(document, "fonts", {
      configurable: true,
      value: { ready: Promise.resolve() },
    });
  });

  it("keeps personal and academic details on separate PDF pages without a personal heading rule", async () => {
    const { container } = render(
      <MemoryRouter initialEntries={["/student-section/admissions/42/print"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId/print"
            element={<AdmissionPrintPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Test Student")).toBeInTheDocument();

    const pages = container.querySelectorAll<HTMLElement>("[data-admission-pdf-page]");
    expect(pages).toHaveLength(4);
    pages.forEach((page) => {
      expect(page).toHaveClass("h-[297mm]", "w-[210mm]", "min-w-[210mm]", "max-w-none");
    });
    expect(pages[0]).toHaveTextContent("Personal Information");
    expect(pages[0]).not.toHaveTextContent("Academic Record");
    expect(pages[1]).toHaveTextContent("Academic Record");
    expect(pages[1]).toHaveTextContent("Entrance and Final Details");
    expect(pages[1]).toHaveTextContent("MH-CET");
    expect(pages[1]).toHaveTextContent("92.5 percentile");
    expect(pages[1]).toHaveTextContent("CMAT");
    expect(pages[1]).toHaveTextContent("Rank 120");

    const personalHeading = screen.getByRole("heading", { name: "Personal Information" });
    expect(personalHeading).not.toHaveClass("border-b-2");
    expect(personalHeading.nextElementSibling).toBeNull();

    const academicHeading = screen.getByRole("heading", { name: "Academic Record" });
    expect(academicHeading.nextElementSibling).toHaveAttribute("aria-hidden", "true");
    expect(academicHeading.nextElementSibling).toHaveClass("mt-2", "border-t");

    const tables = container.querySelectorAll(".admission-pdf-table");
    expect(tables).toHaveLength(3);
    expect(tables[0].querySelector("th")).toHaveClass("py-4");
    expect(tables[0].querySelector("td")).toHaveClass("py-4");
    expect(tables[2].querySelector("th")).toHaveClass("py-3.5");
    expect(tables[2].querySelector("td")).toHaveClass("py-3");

    await waitFor(() =>
      expect(admissionApi.getAdmissionDocumentRequirements).toHaveBeenCalledWith(42),
    );
  });

  it("exports every UI page directly to the full A4 PDF canvas without centering", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/student-section/admissions/42/print"]}>
        <Routes>
          <Route
            path="/student-section/admissions/:admissionId/print"
            element={<AdmissionPrintPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole("button", { name: "Download PDF" }));

    await waitFor(() => expect(pdfExportMocks.save).toHaveBeenCalledTimes(1));
    expect(pdfExportMocks.html2canvas).toHaveBeenCalledTimes(4);
    expect(pdfExportMocks.addPage).toHaveBeenCalledTimes(3);
    expect(pdfExportMocks.addImage).toHaveBeenCalledTimes(4);
    pdfExportMocks.addImage.mock.calls.forEach((call) => {
      expect(call.slice(2, 6)).toEqual([0, 0, 210, 297]);
    });
  });
});
