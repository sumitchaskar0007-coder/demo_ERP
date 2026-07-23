import axios from "axios";
import { ROUTES } from "@/lib/constants";

// Keep development requests on the same origin so phones and tablets connected
// over Wi-Fi can use Vite's API proxy without separate CORS configuration.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

const authClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  withCredentials: true,
  withXSRFToken: true,
});
let refreshPromise: Promise<void> | null = null;
let csrfPromise: Promise<void> | null = null;

const unsafeMethods = new Set(["post", "put", "patch", "delete"]);

function csrfTokenFromCookie() {
  const entry = document.cookie.split("; ").find((cookie) => cookie.startsWith("XSRF-TOKEN="));
  return entry ? decodeURIComponent(entry.slice("XSRF-TOKEN=".length)) : null;
}

async function ensureCsrfToken(force = false) {
  if (!force && csrfTokenFromCookie()) return;
  if (!csrfPromise) {
    csrfPromise = authClient
      .get("/api/v1/auth/csrf")
      .then(() => undefined)
      .finally(() => {
        csrfPromise = null;
      });
  }
  await csrfPromise;
}

apiClient.interceptors.request.use(async (config) => {
  if (config.data instanceof FormData && config.headers.get("Content-Type") === "application/json") {
    config.headers.set("Content-Type", "multipart/form-data");
  }
  if (unsafeMethods.has(config.method?.toLowerCase() ?? "")) {
    await ensureCsrfToken();
    const token = csrfTokenFromCookie();
    if (token) config.headers.set("X-XSRF-TOKEN", token);
  }
  return config;
});

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = authClient
      .get("/api/v1/auth/csrf")
      .then(() => authClient.post("/api/v1/auth/refresh"))
      .then(() => undefined)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const path = window.location.pathname;
      const original = error.config as
        | (typeof error.config & { _retried?: boolean; _csrfRetried?: boolean })
        | undefined;
      const authRequest = original?.url?.startsWith("/api/v1/auth/");
      if (status === 401 && original && !original._retried && !authRequest) {
        original._retried = true;
        try {
          await refreshSession();
          return apiClient.request(original);
        } catch {
          window.dispatchEvent(new Event("auth:unauthorized"));
          if (path !== ROUTES.login) window.location.assign(ROUTES.login);
        }
      } else if (status === 401 && !authRequest) {
        window.dispatchEvent(new Event("auth:unauthorized"));
      } else if (
        status === 403 &&
        original &&
        !original._csrfRetried &&
        !authRequest &&
        unsafeMethods.has(original.method?.toLowerCase() ?? "")
      ) {
        original._csrfRetried = true;
        await ensureCsrfToken(true);
        const token = csrfTokenFromCookie();
        if (token) original.headers.set("X-XSRF-TOKEN", token);
        return apiClient.request(original);
      }
    }
    return Promise.reject(error);
  },
);

export async function initializeCsrf() {
  await ensureCsrfToken(true);
}
