import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Sidebar } from "./Sidebar";

const accessState = vi.hoisted(() => ({ divisionAllocated: false }));
const authState = vi.hoisted(() => ({ roles: ["STUDENT"] }));

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => ({
    isRole: (roles: string[]) => roles.some((role) => authState.roles.includes(role)),
  }),
}));

vi.mock("@/features/academics/StudentAcademicAccessContext", () => ({
  useStudentAcademicAccess: () => accessState,
}));

describe("student sidebar", () => {
  beforeEach(() => {
    accessState.divisionAllocated = false;
    authState.roles = ["STUDENT"];
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

describe("teacher sidebar", () => {
  beforeEach(() => {
    authState.roles = ["CLASS_TEACHER", "SUBJECT_TEACHER"];
  });

  it("groups detailed teacher tools into themed navigation areas", () => {
    render(
      <MemoryRouter initialEntries={["/teacher/workspace?tab=workload"]}>
        <Sidebar collapsed={false} onToggle={() => undefined} />
      </MemoryRouter>,
    );

    for (const label of ["Dashboard", "Students", "Teaching", "Attendance", "Updates", "Profile"]) {
      expect(screen.getByRole("link", { name: label })).toBeInTheDocument();
    }
    for (const detail of [
      "PRN & Roll Numbers",
      "Student Directory",
      "Attendance Analytics",
      "Needs Attention",
      "Workload",
      "Today's Schedule",
      "Notifications",
      "My Timetable",
      "Take Attendance",
      "Attendance Reports",
    ]) {
      expect(screen.queryByRole("link", { name: detail })).not.toBeInTheDocument();
    }
  });

  it("keeps the Teaching group active on a detailed teaching screen", () => {
    render(
      <MemoryRouter initialEntries={["/teacher/workspace?tab=workload"]}>
        <Sidebar collapsed={false} onToggle={() => undefined} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "Teaching" })).toHaveClass("bg-brand-50");
  });
});

describe("leadership teaching navigation", () => {
  it.each(["PRINCIPAL", "HOD"])("keeps teaching tasks out of the %s leadership menu", (role) => {
    authState.roles = [role];
    render(
      <MemoryRouter>
        <Sidebar collapsed={false} onToggle={() => undefined} workspaceMode="leadership" />
      </MemoryRouter>,
    );

    expect(screen.queryByRole("link", { name: "Teaching Home" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "My Class" })).not.toBeInTheDocument();
  });

  it.each(["PRINCIPAL", "HOD"])(
    "shows only teaching tools in the assigned %s teaching workspace",
    (leadershipRole) => {
      authState.roles = [leadershipRole, "CLASS_TEACHER"];
      render(
        <MemoryRouter>
          <Sidebar collapsed={false} onToggle={() => undefined} workspaceMode="teaching" />
        </MemoryRouter>,
      );

      expect(screen.getByRole("link", { name: "Teaching Home" })).toHaveAttribute(
        "href",
        "/teacher/workspace",
      );
      expect(screen.getByRole("link", { name: "My Class" })).toHaveAttribute(
        "href",
        "/academic/class-teacher/my-class",
      );
      for (const label of ["Students", "Teaching", "Attendance", "Updates", "Profile"]) {
        expect(screen.getByRole("link", { name: label })).toBeInTheDocument();
      }
      expect(screen.queryByRole("link", { name: "Staff" })).not.toBeInTheDocument();
      expect(screen.queryByRole("link", { name: "Teaching Assignments" })).not.toBeInTheDocument();
    },
  );
});
