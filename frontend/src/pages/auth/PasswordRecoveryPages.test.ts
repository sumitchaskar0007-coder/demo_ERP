import { describe, expect, it } from "vitest";
import { passwordRequirements } from "@/features/auth/passwordRules";

describe("passwordRequirements", () => {
  it("accepts a password that matches every backend requirement", () => {
    expect(passwordRequirements("College@2026").every(({ met }) => met)).toBe(true);
  });

  it("shows which requirements are missing while the password is being typed", () => {
    const requirements = passwordRequirements("college");

    expect(requirements.find(({ label }) => label.includes("uppercase"))?.met).toBe(false);
    expect(requirements.find(({ label }) => label.includes("number"))?.met).toBe(false);
    expect(requirements.find(({ label }) => label.includes("special character:"))?.met).toBe(false);
  });

  it("rejects spaces and unsupported symbols", () => {
    const requirements = passwordRequirements("College 2026+");

    expect(requirements.at(-1)?.met).toBe(false);
  });
});
