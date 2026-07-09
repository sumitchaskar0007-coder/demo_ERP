import type { AuthUser } from "@/features/auth/types";

const TOKEN_KEY = "jadhavr_erp_token";
const USER_KEY = "jadhavr_erp_user";

export const authToken = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setSession: (token: string, user: AuthUser) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  getUser: (): AuthUser | null => {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};
