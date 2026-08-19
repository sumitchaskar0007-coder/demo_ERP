import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StudentAttendancePage } from "./StudentAttendancePage";
import { attendanceApi } from "./api";

vi.mock("./api", () => ({
  attendanceApi: {
    student: vi.fn(),
  },
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn() },
}));

const studentAttendance = vi.mocked(attendanceApi.student);

describe("StudentAttendancePage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("stops loading after a failed request and allows a retry", async () => {
    studentAttendance
      .mockRejectedValueOnce({ response: { status: 500 } })
      .mockResolvedValueOnce({
        studentId: 7,
        studentName: "Student",
        rollNumber: "12",
        overallPercentage: 0,
        indicator: "CRITICAL",
        subjects: [],
        monthly: [],
        history: [],
      });

    render(<StudentAttendancePage />);

    expect(await screen.findByText("Attendance could not be loaded")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));

    await waitFor(() => expect(studentAttendance).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("My Attendance")).toBeInTheDocument();
  });
});
