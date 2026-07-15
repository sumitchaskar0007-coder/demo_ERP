import axios from "axios";
import { ROUTES } from "@/lib/constants";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  || `${window.location.protocol}//${window.location.hostname}:8081`;
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

const authClient = axios.create({ baseURL: API_BASE_URL, timeout: 15_000, withCredentials: true, withXSRFToken: true });
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
      }
    }
    return Promise.reject(error);
  },
);

export async function initializeCsrf() { await authClient.get("/api/v1/auth/csrf"); }
