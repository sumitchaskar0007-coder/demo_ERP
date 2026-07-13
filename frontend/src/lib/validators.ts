import { z } from "zod";

const code = z
  .string()
  .min(2, "Code must contain at least 2 characters")
  .max(30, "Code cannot exceed 30 characters")
  .regex(/^[A-Za-z0-9_-]+$/, "Use only letters, numbers, underscore, or hyphen");

const optionalEmail = z
  .string()
  .max(150, "Email cannot exceed 150 characters")
  .refine((value) => !value || z.string().email().safeParse(value).success, "Enter a valid email");

export const loginSchema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

export const createCollegeSchema = z.object({
  name: z.string().trim().min(2, "Name must contain at least 2 characters").max(150),
  code,
  address: z.string().max(500).optional().default(""),
  city: z.string().max(100).optional().default(""),
  state: z.string().max(100).optional().default(""),
  pincode: z.string().max(10).optional().default(""),
  contactEmail: optionalEmail.optional().default(""),
  contactPhone: z.string().max(20).optional().default(""),
  logoUrl: z.string().max(500).optional().default(""),
  qrCodeUrl: z.string().max(500).optional().default(""),
});

export const updateCollegeSchema = createCollegeSchema.omit({ code: true });

export const createDepartmentSchema = z.object({
  collegeId: z.coerce.number().positive("College is required"),
  name: z.string().trim().min(2, "Name must contain at least 2 characters").max(150),
  code,
  description: z.string().max(500).optional().default(""),
});

export const updateDepartmentSchema = createDepartmentSchema.omit({
  collegeId: true,
  code: true,
});

export const createPrincipalSchema = z.object({
  collegeId: z.coerce.number().positive("College is required"),
  fullName: z.string().trim().min(2).max(150),
  email: z.string().email("Enter a valid email").max(150),
  phone: z.string().max(20).optional().default(""),
  password: z
    .string()
    .min(8)
    .max(100)
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/,
      "Include uppercase, lowercase, number, and special character",
    ),
});

export const updatePrincipalSchema = z.object({
  phone: z.string().max(20).optional().default(""),
  password: z.union([
    z.literal(""),
    z.string().min(8).max(100).regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/,
      "Include uppercase, lowercase, number, and special character",
    ),
  ]),
});

export const updateOwnProfileSchema = z.object({
  phone: z.string().max(20, "Phone cannot exceed 20 characters").optional().default(""),
  address: z.string().max(500, "Address cannot exceed 500 characters").optional().default(""),
  bio: z.string().max(500, "Bio cannot exceed 500 characters").optional().default(""),
});

export const publicAdmissionSchema = z.object({
  departmentId: z.coerce.number().positive("Department is required"),
  firstName: z.string().trim().min(2).max(80),
  middleName: z.string().max(80).optional().default(""),
  lastName: z.string().trim().min(2).max(80),
  email: z.string().email("Enter a valid email").max(150),
  phone: z.string().min(1, "Phone is required").max(20),
  dateOfBirth: z.string().min(1, "Date of birth is required"),
  gender: z.string().min(1, "Gender is required").max(30),
  addressLine1: z.string().max(250).optional().default(""),
  addressLine2: z.string().max(250).optional().default(""),
  city: z.string().max(100).optional().default(""),
  state: z.string().max(100).optional().default(""),
  pincode: z.string().max(10).optional().default(""),
  parentName: z.string().trim().min(2).max(150),
  parentPhone: z.string().min(1, "Parent phone is required").max(20),
  parentEmail: optionalEmail.optional().default(""),
  previousSchoolName: z.string().max(200).optional().default(""),
  previousClassName: z.string().max(100).optional().default(""),
  previousPercentage: z.union([z.coerce.number().min(0).max(100), z.literal("")]).optional(),
});

export const createStudentSectionStaffSchema = z.object({
  collegeId: z.coerce.number().positive("College is required"),
  fullName: z.string().trim().min(2).max(150),
  email: z.string().email("Enter a valid email").max(150),
  phone: z.string().max(20).optional().default(""),
  password: z
    .string()
    .min(8)
    .max(100)
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/,
      "Include uppercase, lowercase, number, and special character",
    ),
  joiningDate: z.string().optional().default(""),
});

export const approveAdmissionSchema = z.object({
  remarks: z.string().max(500).optional().default(""),
});

export const rejectAdmissionSchema = z.object({
  rejectionReason: z.string().trim().min(5).max(500),
});

export const markAdmissionPrintedSchema = z.object({
  remarks: z.string().max(500).optional().default(""),
});
