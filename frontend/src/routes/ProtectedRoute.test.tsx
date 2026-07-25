import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: vi.fn(),
}));
vi.mock("@/features/admissions/api", () => ({
  getStudentAdmissionAccess: vi.fn(),
}));

import { useAuth } from "@/features/auth/authStore";
import { getStudentAdmissionAccess } from "@/features/admissions/api";

describe("ProtectedRoute", () => {
  it("redirects unauthenticated users to login", () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false, initializing: false } as never);
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<p>Dashboard</p>} />
          </Route>
          <Route path="/login" element={<p>Login</p>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Login")).toBeInTheDocument();
    expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
  });

  it("renders protected content for an authenticated user", () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true, initializing: false } as never);
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<p>Dashboard</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Dashboard")).toBeInTheDocument();
  });

  it("forces users with a temporary password onto the change-password screen", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { mustChangePassword: true },
    } as never);
    render(
      <MemoryRouter initialEntries={["/student/dashboard"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/student/dashboard" element={<p>Student Dashboard</p>} />
            <Route path="/change-password" element={<p>Mandatory Password Change</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Mandatory Password Change")).toBeInTheDocument();
    expect(screen.queryByText("Student Dashboard")).not.toBeInTheDocument();
  });

  it("keeps a student on the admission form until student-section approval", async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { roles: ["STUDENT"], mustChangePassword: true },
    } as never);
    vi.mocked(getStudentAdmissionAccess).mockResolvedValue({
      admissionId: 1,
      status: "STUDENT_DETAILS_PENDING",
      formCompleted: false,
      editable: true,
      pending: false,
      accessGranted: false,
    });

    render(
      <MemoryRouter initialEntries={["/change-password"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/student/admission" element={<p>Admission Form</p>} />
            <Route path="/change-password" element={<p>Mandatory Password Change</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Admission Form")).toBeInTheDocument();
    expect(screen.queryByText("Mandatory Password Change")).not.toBeInTheDocument();
  });

  it("requires the password change after student-section approval", async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { roles: ["STUDENT"], mustChangePassword: true },
    } as never);
    vi.mocked(getStudentAdmissionAccess).mockResolvedValue({
      admissionId: 1,
      status: "STUDENT_SECTION_APPROVED",
      formCompleted: true,
      editable: false,
      pending: false,
      accessGranted: true,
    });

    render(
      <MemoryRouter initialEntries={["/student/dashboard"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/student/dashboard" element={<p>Student Dashboard</p>} />
            <Route path="/change-password" element={<p>Mandatory Password Change</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Mandatory Password Change")).toBeInTheDocument();
    expect(screen.queryByText("Student Dashboard")).not.toBeInTheDocument();
  });
});
