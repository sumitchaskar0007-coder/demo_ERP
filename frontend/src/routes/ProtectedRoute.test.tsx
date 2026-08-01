import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from "@/features/auth/authStore";

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

  it("forces every account with a one-time password to change it", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { roles: ["STUDENT"], mustChangePassword: true },
    } as never);
    render(
      <MemoryRouter initialEntries={["/student/admission"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/student/admission" element={<p>Admission Form</p>} />
            <Route path="/change-password" element={<p>Mandatory Password Change</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Mandatory Password Change")).toBeInTheDocument();
    expect(screen.queryByText("Admission Form")).not.toBeInTheDocument();
  });
});
