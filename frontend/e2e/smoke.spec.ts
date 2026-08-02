import { expect, test } from "@playwright/test";

test("login page renders without requiring an API session", async ({ page }) => {
  await page.route("**/api/v1/auth/csrf", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ token: "playwright-smoke-csrf-token" }),
    });
  });
  await page.route("**/api/v1/auth/me", async (route) => {
    await route.fulfill({ status: 401, contentType: "application/json", body: "{}" });
  });

  await page.goto("/login");
  await expect(page.getByRole("heading", { name: /welcome back/i }).first()).toBeVisible();
  await expect(page.getByLabel("Email address")).toBeVisible();
  await expect(page.getByLabel("Password", { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
});
