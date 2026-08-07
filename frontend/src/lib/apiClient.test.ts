import { beforeEach, describe, expect, it } from "vitest";
import { csrfTokenFromCookie, requiresCsrfProtection } from "./apiClient";

describe("CSRF request policy", () => {
  beforeEach(() => {
    document.cookie = "XSRF-TOKEN=; Path=/; Max-Age=0";
  });

  it("does not delay endpoints that the backend exempts from CSRF", () => {
    expect(requiresCsrfProtection("post", "/api/v1/auth/login")).toBe(false);
    expect(requiresCsrfProtection("POST", "/api/public/admissions/college/101/submit")).toBe(false);
  });

  it("requires CSRF for authenticated writes but not reads", () => {
    expect(requiresCsrfProtection("patch", "/api/student/admissions/me/details")).toBe(true);
    expect(requiresCsrfProtection("get", "/api/student/admissions/me/details")).toBe(false);
  });

  it("removes a malformed token so the next request can obtain a clean cookie", () => {
    document.cookie = "XSRF-TOKEN=invalid%ZZ; Path=/";

    expect(csrfTokenFromCookie()).toBeNull();
    expect(document.cookie).not.toContain("XSRF-TOKEN=");
  });
});
