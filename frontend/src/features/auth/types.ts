export interface AuthUser {
  id: number;
  collegeId: number | null;
  collegeName: string | null;
  collegeCode: string | null;
  fullName: string;
  email: string;
  phone: string | null;
  status: "ACTIVE" | "INACTIVE";
  roles: string[];
  mustChangePassword: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: "Bearer";
  expiresInMs: number;
  user: AuthUser;
}
