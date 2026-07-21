import { describe, expect, it } from "vitest";
import { localDateString } from "./date";

describe("localDateString", () => {
  it("formats the local calendar date without UTC shifting", () => {
    expect(localDateString(new Date(2026, 0, 5, 23, 59))).toBe("2026-01-05");
  });

  it("pads single-digit months and days", () => {
    expect(localDateString(new Date(2026, 8, 9))).toBe("2026-09-09");
  });
});
