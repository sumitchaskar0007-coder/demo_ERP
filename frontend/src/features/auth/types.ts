export interface AuthUser {
  id: number;
  collegeId: number | null;
  collegeName: string | null;
  collegeCode: string | null;
  fullName: string;
  email: string;
  phone: string | null;
  profileImageUrl: string | null;
  address: string | null;
  bio: string | null;
  status: "ACTIVE" | "INACTIVE";
  roles: string[];
  mustChangePassword: boolean;
  emailVerified: boolean;
}

export interface UpdateOwnProfileValues {
  phone: string;
  address: string;
  bio: string;
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
