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
    "/opt/college-erp/app/config.yml",
    "IllegalStateException\n\tat com.collegeerp.Service.run(Service.java:17)",
  ])("replaces sensitive server details: %s", (message) => {
    const result = handleApiError(axiosFailure(500, { message }));
    expect(result.message).toBe("The request could not be completed. Please try again.");
  });

  it("never renders a local Error message", () => {
    const result = handleApiError(new Error("ENOENT: C:\\private\\credentials.txt"));
    expect(result.message).toBe("The request could not be completed. Please try again.");
  });

  it("explains a mobile upload timeout without exposing client details", () => {
    expect(
      handleApiError({ isAxiosError: true, code: "ECONNABORTED", message: "timeout of 15000ms" }),
    ).toEqual({
      message: "The request took too long on this connection. Check your network and try again.",
      fieldErrors: {},
    });
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

  it("keeps safe nested admission field paths", () => {
    const result = handleApiError(
      axiosFailure(400, {
        message: "Admission information validation failed",
        errors: {
          "academicRecords[0].obtainedMarks": "Obtained marks must not exceed total marks",
        },
      }),
    );
    expect(result.fieldErrors).toEqual({
      "academicRecords[0].obtainedMarks": "Obtained marks must not exceed total marks",
    });
  });

  it("does not expose correlation metadata as a form field", () => {
    const result = handleApiError(
      axiosFailure(500, {
        message: "The request could not be completed. Please try again.",
        errors: { correlationId: "ba425cb4-a9d5-40a5-a019-86649a6954a5" },
      }),
    );

    expect(result.fieldErrors).toEqual({});
    expect(result.message).toBe("The request could not be completed. Please try again.");
  });
});
