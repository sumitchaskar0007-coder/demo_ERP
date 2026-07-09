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
