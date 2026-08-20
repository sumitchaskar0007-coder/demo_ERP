import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { NotFoundPage } from "./NotFoundPage";

vi.mock("@/features/auth/authStore", () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from "@/features/auth/authStore";

function renderUnknownRoute() {
  render(
    <MemoryRouter initialEntries={["/old-page"]}>
      <Routes>
        <Route path="/login" element={<p>Login page</p>} />
        <Route path="/dashboard" element={<p>Dashboard page</p>} />
        <Route path="/change-password" element={<p>Change password page</p>} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NotFoundPage", () => {
  it("redirects a signed-out user to login", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      initializing: false,
      user: null,
    } as never);

    renderUnknownRoute();

    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("redirects a signed-in user to their dashboard", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { roles: ["SUPER_ADMIN"], mustChangePassword: false },
    } as never);

    renderUnknownRoute();

    expect(screen.getByText("Dashboard page")).toBeInTheDocument();
  });

  it("keeps one-time-password users on the required password step", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      initializing: false,
      user: { roles: ["STUDENT"], mustChangePassword: true },
    } as never);

    renderUnknownRoute();

    expect(screen.getByText("Change password page")).toBeInTheDocument();
  });
});
