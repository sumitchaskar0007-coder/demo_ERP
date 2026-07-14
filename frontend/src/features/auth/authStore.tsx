import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  useEffect,
  type PropsWithChildren,
} from "react";
import * as authApi from "./api";
import type { AuthUser, LoginRequest, UpdateOwnProfileValues } from "./types";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  initializing: boolean;
  isRole: (roles: string[]) => boolean;
  login: (request: LoginRequest) => Promise<AuthUser>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  updateProfile: (request: UpdateOwnProfileValues) => Promise<AuthUser>;
  uploadProfilePhoto: (file: File) => Promise<AuthUser>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    authApi.bootstrapSession().then(setUser).catch(() => setUser(null)).finally(() => setInitializing(false));
    const clear = () => setUser(null);
    window.addEventListener("auth:unauthorized", clear);
    return () => window.removeEventListener("auth:unauthorized", clear);
  }, []);

  const login = useCallback(async (request: LoginRequest) => {
    const authenticatedUser = await authApi.login(request);
    setUser(authenticatedUser);
    return authenticatedUser;
  }, []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } finally { setUser(null); }
  }, []);

  const refreshProfile = useCallback(async () => {
    const profile = await authApi.getProfile();
    setUser(profile);
  }, []);

  const updateProfile = useCallback(async (request: UpdateOwnProfileValues) => {
    const profile = await authApi.updateProfile(request);
    setUser(profile);
    return profile;
  }, []);

  const uploadProfilePhoto = useCallback(async (file: File) => {
    const profile = await authApi.uploadProfilePhoto(file);
    setUser(profile);
    return profile;
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      initializing,
      isRole: (roles) => Boolean(user?.roles.some((role) => roles.includes(role))),
      login,
      logout,
      refreshProfile,
      updateProfile,
      uploadProfilePhoto,
    }),
    [initializing, login, logout, refreshProfile, updateProfile, uploadProfilePhoto, user],
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
