export const APP_NAME = "Jadhavr ERP";

export const ROUTES = {
  login: "/login",
  dashboard: "/dashboard",
  profile: "/profile",
  colleges: "/colleges",
  departments: "/departments",
  users: "/users",
  createPrincipal: "/users/principals/create",
  forbidden: "/forbidden",
  serverError: "/server-error",
} as const;

export const ROLES = {
  SUPER_ADMIN: "SUPER_ADMIN",
  PRINCIPAL: "PRINCIPAL",
} as const;

export type AppRole = (typeof ROLES)[keyof typeof ROLES];

export const PAGE_SIZE = 10;

export const STATUS_OPTIONS = [
  { label: "All statuses", value: "" },
  { label: "Active", value: "ACTIVE" },
  { label: "Inactive", value: "INACTIVE" },
];
