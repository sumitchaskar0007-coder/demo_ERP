import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PublicAdmissionPage } from "./PublicAdmissionPage";

vi.mock("@/features/admissions/api", () => ({
  getPublicAdmissionInfo: vi.fn(),
  getPublicAdmissionCategories: vi.fn(),
  submitAdmission: vi.fn(),
}));

import * as admissionApi from "@/features/admissions/api";

describe("PublicAdmissionPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(admissionApi.getPublicAdmissionInfo).mockResolvedValue({
      collegeId: 1,
      collegeName: "Jadhavar College",
      collegeCode: "101",
      academicYear: "2026-27",
      departments: [{ id: 30, name: "Computer Science", code: "CS" }],
    });
    vi.mocked(admissionApi.getPublicAdmissionCategories).mockResolvedValue([
      { category: "OPEN", label: "OPEN" },
      { category: "OTHER", customCategoryName: "NT", label: "NT" },
      { category: "OTHER", customCategoryName: null, label: "Other (not listed)" },
    ]);
  });

  it("shows only available category names after Other is selected", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/admissions/101"]}>
        <Routes>
          <Route path="/admissions/:collegeCode" element={<PublicAdmissionPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText("Jadhavar College");
    await user.selectOptions(screen.getByLabelText("Department"), "30");
    await waitFor(() =>
      expect(admissionApi.getPublicAdmissionCategories).toHaveBeenCalledWith(
        "101",
        30,
        expect.objectContaining({ academicYear: "2026-27" }),
      ),
    );

    await user.selectOptions(screen.getByLabelText("Student category"), "OTHER");
    const customCategory = await screen.findByLabelText("Other category");

    expect(screen.getByRole("option", { name: "NT" })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: "Other (not listed)" })).not.toBeInTheDocument();
    await user.selectOptions(customCategory, "NT");
    expect(customCategory).toHaveValue("NT");
  });

  it("shows missing information beside each required field", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/admissions/101"]}>
        <Routes>
          <Route path="/admissions/:collegeCode" element={<PublicAdmissionPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText("Jadhavar College");
    await user.click(screen.getByRole("button", { name: /create student login/i }));

    expect(await screen.findByText("Department is required")).toBeInTheDocument();
    expect(screen.getByText("First name is required")).toBeInTheDocument();
    expect(screen.getByText("Last name is required")).toBeInTheDocument();
    expect(screen.getByText("Email is required")).toBeInTheDocument();
    expect(screen.getByText("Phone is required")).toBeInTheDocument();
    expect(screen.getByText("Date of birth is required")).toBeInTheDocument();
    expect(screen.getByText("Gender is required")).toBeInTheDocument();
    expect(admissionApi.submitAdmission).not.toHaveBeenCalled();
  });

  it("shows a duplicate-email API error beside the email field", async () => {
    const user = userEvent.setup();
    vi.mocked(admissionApi.submitAdmission).mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          message: "Admission information validation failed",
          errors: {
            email: "This email is already registered. Use another email or sign in.",
          },
        },
      },
    });

    render(
      <MemoryRouter initialEntries={["/admissions/101"]}>
        <Routes>
          <Route path="/admissions/:collegeCode" element={<PublicAdmissionPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText("Jadhavar College");
    await user.selectOptions(screen.getByLabelText("Department"), "30");
    await waitFor(() => expect(admissionApi.getPublicAdmissionCategories).toHaveBeenCalled());
    await user.type(screen.getByLabelText("First name"), "Aarav");
    await user.type(screen.getByLabelText("Last name"), "Patil");
    const email = screen.getByLabelText("Email address");
    await user.type(email, "aarav.patil@example.com");
    await user.type(screen.getByLabelText("Mobile number"), "9876543210");
    await user.type(screen.getByLabelText("Date of birth"), "2000-01-01");
    await user.selectOptions(screen.getByLabelText("Gender"), "Male");
    await user.click(screen.getByRole("button", { name: /create student login/i }));

    expect(
      await screen.findByText("This email is already registered. Use another email or sign in."),
    ).toBeInTheDocument();
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Please correct the highlighted field below.",
    );
  });
});
