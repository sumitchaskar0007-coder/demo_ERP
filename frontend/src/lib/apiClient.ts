import axios from "axios";
import { authToken } from "@/lib/authToken";
import { ROUTES } from "@/lib/constants";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8081",
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config) => {
  const token = authToken.getToken();
  const isLoginRequest = config.url?.includes("/api/auth/login");
  // Login must never carry an old session token. An expired token can cause
  // the security filter to reject valid credentials before login is handled.
  if (token && !isLoginRequest) config.headers.Authorization = `Bearer ${token}`;
  if (isLoginRequest) delete config.headers.Authorization;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const path = window.location.pathname;
      if (status === 401) {
        authToken.clear();
        if (path !== ROUTES.login) window.location.assign(ROUTES.login);
      } else if (status === 403 && path !== ROUTES.forbidden) {
        window.location.assign(ROUTES.forbidden);
      }
    }
    return Promise.reject(error);
  },
);
