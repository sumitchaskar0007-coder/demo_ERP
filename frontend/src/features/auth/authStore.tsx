import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { authToken } from "@/lib/authToken";
import * as authApi from "./api";
import type { AuthUser, LoginRequest, UpdateOwnProfileValues } from "./types";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isRole: (roles: string[]) => boolean;
  login: (request: LoginRequest) => Promise<AuthUser>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
  updateProfile: (request: UpdateOwnProfileValues) => Promise<AuthUser>;
  uploadProfilePhoto: (file: File) => Promise<AuthUser>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const storedUser = authToken.getUser();
    const storedToken = authToken.getToken();
    if (!storedUser || !storedToken) {
      authToken.clear();
      return null;
    }
    return storedUser;
  });

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request);
    authToken.setSession(response.token, response.user);
    setUser(response.user);
    return response.user;
  }, []);

  const logout = useCallback(() => {
    authToken.clear();
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    const profile = await authApi.getProfile();
    const token = authToken.getToken();
    if (token) authToken.setSession(token, profile);
    setUser(profile);
  }, []);

  const updateProfile = useCallback(async (request: UpdateOwnProfileValues) => {
    const profile = await authApi.updateProfile(request);
    const token = authToken.getToken();
    if (token) authToken.setSession(token, profile);
    setUser(profile);
    return profile;
  }, []);

  const uploadProfilePhoto = useCallback(async (file: File) => {
    const profile = await authApi.uploadProfilePhoto(file);
    const token = authToken.getToken();
    if (token) authToken.setSession(token, profile);
    setUser(profile);
    return profile;
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user && authToken.getToken()),
      isRole: (roles) => Boolean(user?.roles.some((role) => roles.includes(role))),
      login,
      logout,
      refreshProfile,
      updateProfile,
      uploadProfilePhoto,
    }),
    [login, logout, refreshProfile, updateProfile, uploadProfilePhoto, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Hooks intentionally share the provider module to keep auth state encapsulated.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
