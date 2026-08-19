import { describe, expect, it } from "vitest";
import { workspaceHome, workspaceModeForPath } from "./workspaceMode";

describe("leadership workspace routing", () => {
  it("recognizes teaching and leadership destinations", () => {
    expect(workspaceModeForPath("/teacher/workspace")).toBe("teaching");
    expect(workspaceModeForPath("/academic/class-teacher/my-class")).toBe("teaching");
    expect(workspaceModeForPath("/principal/academics")).toBe("leadership");
    expect(workspaceModeForPath("/hod")).toBe("leadership");
    expect(workspaceModeForPath("/notices")).toBeNull();
  });

  it("returns the correct home for each workspace", () => {
    expect(workspaceHome("teaching", ["PRINCIPAL"])).toBe("/teacher/workspace");
    expect(workspaceHome("leadership", ["PRINCIPAL"])).toBe("/dashboard");
    expect(workspaceHome("leadership", ["HOD"])).toBe("/hod");
  });
});
