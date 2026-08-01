import { describe, expect, it } from "vitest";
import { handleApiError } from "@/lib/handleApiError";

function axiosFailure(status: number, data: unknown) {
  return {
    isAxiosError: true,
    response: { status, data },
  };
}

describe("handleApiError", () => {
  it("keeps an intentional API validation message", () => {
    expect(
      handleApiError(
        axiosFailure(400, {
          message: "Validation failed",
          errors: { email: "Email must be valid" },
        }),
      ),
    ).toEqual({
      message: "Validation failed",
      status: 400,
      fieldErrors: { email: "Email must be valid" },
    });
  });

  it.each([
    "java.sql.SQLException: relation users does not exist",
    "C:\\app\\secrets\\application.properties:42",
    "/opt/jadhavr/app/config.yml",
    "IllegalStateException\n\tat com.jadhavr.Service.run(Service.java:17)",
  ])("replaces sensitive server details: %s", (message) => {
    const result = handleApiError(axiosFailure(500, { message }));
    expect(result.message).toBe("The request could not be completed. Please try again.");
  });

  it("never renders a local Error message", () => {
    const result = handleApiError(new Error("ENOENT: C:\\private\\credentials.txt"));
    expect(result.message).toBe("The request could not be completed. Please try again.");
  });

  it("filters malformed field-error data", () => {
    const result = handleApiError(
      axiosFailure(400, {
        message: "Validation failed",
        errors: {
          email: "at internal.Validator.validate(Validator.java:12)",
          "../../path": "secret",
          phone: { raw: "database error" },
        },
      }),
    );
    expect(result.fieldErrors).toEqual({
      email: "This value is invalid.",
    });
  });
});
