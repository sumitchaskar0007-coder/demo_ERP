import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Sidebar } from "./Sidebar";

const accessState = vi.hoisted(() => ({ divisionAllocated: false }));

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => ({ isRole: (roles: string[]) => roles.includes("STUDENT") }),
}));

vi.mock("@/features/academics/StudentAcademicAccessContext", () => ({
  useStudentAcademicAccess: () => accessState,
}));

describe("student sidebar", () => {
  beforeEach(() => {
    accessState.divisionAllocated = false;
  });

  it("hides division features and Account before allocation", () => {
    render(
      <MemoryRouter>
        <Sidebar collapsed={false} onToggle={() => undefined} />
      </MemoryRouter>,
    );

    expect(screen.queryByRole("link", { name: "My Timetable" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "My Attendance" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "My Class" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Notices" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Account" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "My Payments" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Profile" })).toBeInTheDocument();
    expect(screen.queryByText("Future modules")).not.toBeInTheDocument();
    expect(screen.queryByText("Attendance")).not.toBeInTheDocument();
    expect(screen.queryByText("Timetable")).not.toBeInTheDocument();
    expect(screen.queryByText("Fee Payment")).not.toBeInTheDocument();
    expect(screen.queryByText("Results")).not.toBeInTheDocument();
  });

  it("shows division features after allocation without restoring Account", () => {
    accessState.divisionAllocated = true;
    render(
      <MemoryRouter>
        <Sidebar collapsed={false} onToggle={() => undefined} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "My Timetable" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Attendance" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Class" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Notices" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Account" })).not.toBeInTheDocument();
  });

  it("keeps the full student navigation visible on My Admission", () => {
    render(
      <MemoryRouter initialEntries={["/student/admission"]}>
        <Sidebar collapsed={false} onToggle={() => undefined} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Admission" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Fees" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "My Profile" })).toBeInTheDocument();
  });
});
