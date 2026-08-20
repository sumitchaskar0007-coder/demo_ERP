import { describe, expect, it } from "vitest";
import type { AcademicYear } from "@/features/academicSessions/api";
import { calendarState, dateFallsInTerm, localDateIso } from "@/features/academicSessions/calendar";

const year: AcademicYear = {
  id: 1,
  name: "2026-2027",
  startDate: "2026-07-01",
  endDate: "2027-06-30",
  status: "ACTIVE",
  terms: [
    {
      id: 11,
      academicYearId: 1,
      name: "Odd Semester",
      termType: "ODD",
      startDate: "2026-07-01",
      endDate: "2026-12-31",
      status: "CLOSED",
    },
    {
      id: 12,
      academicYearId: 1,
      name: "Even Semester",
      termType: "EVEN",
      startDate: "2027-01-01",
      endDate: "2027-06-30",
      status: "ACTIVE",
    },
  ],
};

describe("academic session calendar safeguards", () => {
  it("detects when the active term conflicts with today's configured term", () => {
    const state = calendarState([year], "2026-08-17");
    expect(state.mismatch).toBe(true);
    expect(state.expectedTerm?.termType).toBe("ODD");
    expect(state.activeTerm?.termType).toBe("EVEN");
  });

  it("treats both start and end dates as inside the semester", () => {
    expect(dateFallsInTerm("2026-07-01", year.terms[0])).toBe(true);
    expect(dateFallsInTerm("2026-12-31", year.terms[0])).toBe(true);
    expect(dateFallsInTerm("2027-01-01", year.terms[0])).toBe(false);
  });

  it("formats a browser-local date without UTC day shifting", () => {
    expect(localDateIso(new Date(2026, 7, 17, 0, 5))).toBe("2026-08-17");
  });
});
