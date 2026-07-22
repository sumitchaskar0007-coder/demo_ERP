import { describe, expect, it } from "vitest";
import { loginSchema } from "./validators";

describe("loginSchema", () => {
  it("rejects empty credentials with field-level messages", () => {
    const result = loginSchema.safeParse({ email: "", password: "" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.flatten().fieldErrors.email).toContain("Email is required");
      expect(result.error.flatten().fieldErrors.password).toContain("Password is required");
    }
  });

  it("accepts a valid email and password", () => {
    expect(
      loginSchema.safeParse({ email: "student@example.com", password: "secret" }).success,
    ).toBe(true);
  });
});
