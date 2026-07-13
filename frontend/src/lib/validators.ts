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
  phone: z.string().min(1, "Phone is required").max(20),
});

export const publicAdmissionSchema = z.object({
  departmentId: z.coerce.number().positive("Department is required"),
  studentCategory: z.enum(["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"]),
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
});

export const createStudentSectionStaffSchema = z.object({
  collegeId: z.coerce.number().positive("College is required"),
  fullName: z.string().trim().min(2).max(150),
  email: z.string().email("Enter a valid email").max(150),
  phone: z.string().min(1, "Phone is required").max(20),
  joiningDate: z.string().optional().default(""),
});

export const approveAdmissionSchema = z.object({
  studentCategory: z.enum(["OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER"]),
  remarks: z.string().max(500).optional().default(""),
});

export const rejectAdmissionSchema = z.object({
  rejectionReason: z.string().trim().min(5).max(500),
});

export const markAdmissionPrintedSchema = z.object({
  remarks: z.string().max(500).optional().default(""),
});
