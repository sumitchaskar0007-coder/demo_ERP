import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ProfilePage } from "./ProfilePage";

const authState = vi.hoisted(() => ({
  refreshProfile: vi.fn(async () => undefined),
  updateProfile: vi.fn(),
  uploadProfilePhoto: vi.fn(),
  user: {
    id: 42,
    collegeId: 1,
    institutionId: 1,
    collegeName: "Jadhavar College",
    collegeCode: "JDC",
    fullName: "Asha Jadhav",
    email: "asha@example.com",
    phone: "9876543210",
    profileImageUrl: null,
    address: "Pune",
    bio: "Faculty member",
    status: "ACTIVE",
    roles: [
      "SUPER_ADMIN",
      "PRINCIPAL",
      "HOD",
      "CLASS_TEACHER",
      "SUBJECT_TEACHER",
      "STUDENT_SECTION",
      "FEE_SECTION",
      "STUDENT",
    ],
    mustChangePassword: false,
    emailVerified: true,
  },
}));

vi.mock("@/features/auth/authStore", () => ({
  useAuth: () => authState,
}));

describe("ProfilePage", () => {
  beforeEach(() => {
    authState.refreshProfile.mockClear();
  });

  it("shows personal information without exposing internal account fields for any role", async () => {
    render(<ProfilePage />);

    expect(await screen.findAllByText("Asha Jadhav")).not.toHaveLength(0);
    expect(screen.getByText("Profile Overview")).toBeInTheDocument();
    expect(screen.getAllByText("Jadhavar College")).not.toHaveLength(0);

    expect(screen.queryByText("User ID")).not.toBeInTheDocument();
    expect(screen.queryByText("Primary Role")).not.toBeInTheDocument();
    expect(screen.queryByText("Account Status")).not.toBeInTheDocument();
    expect(screen.queryByText("SUPER ADMIN")).not.toBeInTheDocument();
    expect(screen.queryByText("ACTIVE")).not.toBeInTheDocument();
  });
});
