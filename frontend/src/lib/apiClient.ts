import axios from "axios";
import { ROUTES } from "@/lib/constants";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";
export const apiClient = axios.create({
  baseURL,
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

const authClient = axios.create({ baseURL, timeout: 15_000, withCredentials: true, withXSRFToken: true });
let refreshPromise: Promise<void> | null = null;

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = authClient.get("/api/v1/auth/csrf")
      .then(() => authClient.post("/api/v1/auth/refresh"))
      .then(() => undefined)
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const path = window.location.pathname;
      const original = error.config as (typeof error.config & { _retried?: boolean }) | undefined;
      const authRequest = original?.url?.startsWith("/api/v1/auth/");
      if (status === 401 && original && !original._retried && !authRequest) {
        original._retried = true;
        try { await refreshSession(); return apiClient.request(original); }
        catch { window.dispatchEvent(new Event("auth:unauthorized")); if (path !== ROUTES.login) window.location.assign(ROUTES.login); }
      } else if (status === 401) {
        window.dispatchEvent(new Event("auth:unauthorized"));
      } else if (status === 403 && path !== ROUTES.forbidden) {
        window.location.assign(ROUTES.forbidden);
      }
    }
    return Promise.reject(error);
  },
);

export async function initializeCsrf() { await authClient.get("/api/v1/auth/csrf"); }
