import { describe, expect, it } from "vitest";
import {
  detailedAdmissionInformationSchema,
  loginSchema,
  publicAdmissionSchema,
} from "./validators";

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

describe("detailedAdmissionInformationSchema", () => {
  const valid = {
    courseYearId: 20,
    fullName: "Test Student",
    email: "student@example.com",
    phone: "9876543210",
    dateOfBirth: "2005-01-01",
    gender: "MALE" as const,
    placeOfBirth: "Pune",
    maritalStatus: "UNMARRIED",
    aadhaarNumber: "123456789012",
    nationality: "Indian",
    religion: "Hindu",
    caste: "Test",
    studentCategory: "OPEN" as const,
    parentName: "Test Guardian",
    parentPhone: "9876500000",
    addressLine1: "Permanent address",
    city: "Pune",
    pincode: "411001",
    state: "Maharashtra",
    correspondenceAddress: "Correspondence address",
    correspondenceCity: "Pune",
    correspondencePincode: "411001",
    correspondenceState: "Maharashtra",
    academicRecords: [
      {
        qualification: "12TH" as const,
        instituteName: "College",
        boardUniversity: "Board",
        yearOfPassing: "2024",
        totalMarks: 500,
        obtainedMarks: 400,
        gradingType: "PERCENTAGE" as const,
        marksPercentage: 80,
      },
    ],
    entranceExams: [{ examName: "", result: "" }],
  };

  it("accepts complete information and an unused blank exam row", () => {
    expect(detailedAdmissionInformationSchema.safeParse(valid).success).toBe(true);
  });

  it("reports exact nested result and entrance-exam fields", () => {
    const result = detailedAdmissionInformationSchema.safeParse({
      ...valid,
      academicRecords: [{ ...valid.academicRecords[0], marksPercentage: 120 }],
      entranceExams: [{ examName: "MH-CET", result: "" }],
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ path: ["academicRecords", 0, "marksPercentage"] }),
          expect.objectContaining({ path: ["entranceExams", 0, "result"] }),
        ]),
      );
    }
  });

  it("accepts CGPA and rejects percentage together with CGPA", () => {
    const cgpa = {
      ...valid,
      academicRecords: [
        {
          ...valid.academicRecords[0],
          gradingType: "CGPA" as const,
          totalMarks: undefined,
          obtainedMarks: undefined,
          marksPercentage: undefined,
          cgpa: 8.4,
        },
      ],
    };
    expect(detailedAdmissionInformationSchema.safeParse(cgpa).success).toBe(true);
    expect(
      detailedAdmissionInformationSchema.safeParse({
        ...cgpa,
        academicRecords: [{ ...cgpa.academicRecords[0], marksPercentage: 84 }],
      }).success,
    ).toBe(false);
  });
});

describe("publicAdmissionSchema", () => {
  const validRegistration = {
    departmentId: 30,
    studentCategory: "OPEN" as const,
    firstName: "Test",
    lastName: "Student",
    email: "student@example.com",
    phone: "9876543210",
    dateOfBirth: "2005-01-01",
    gender: "Male",
  };

  it("accepts a standard student category without a custom category", () => {
    expect(publicAdmissionSchema.safeParse(validRegistration).success).toBe(true);
  });

  it("requires a category when Other is selected", () => {
    const result = publicAdmissionSchema.safeParse({
      ...validRegistration,
      studentCategory: "OTHER",
      customCategoryName: "",
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.flatten().fieldErrors.customCategoryName).toContain(
        "Select an Other category",
      );
    }
  });

  it("accepts Other with a configured category", () => {
    expect(
      publicAdmissionSchema.safeParse({
        ...validRegistration,
        studentCategory: "OTHER",
        customCategoryName: "NT",
      }).success,
    ).toBe(true);
  });
});
