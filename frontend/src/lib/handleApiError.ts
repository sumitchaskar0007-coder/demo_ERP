import axios from "axios";
import type { ApiErrorResponse } from "@/types/api";

export interface NormalizedApiError {
  message: string;
  status?: number;
  fieldErrors: Record<string, string>;
}

const GENERIC_MESSAGE = "The request could not be completed. Please try again.";
const MAX_CLIENT_MESSAGE_LENGTH = 300;
const NON_FIELD_ERROR_KEYS = new Set(["correlationId", "requestId", "traceId", "path"]);
const SENSITIVE_DETAIL_PATTERN = new RegExp(
  [
    "(?:[A-Za-z]:\\\\|/(?:home|users|var|tmp|opt|srv|app|etc)/)",
    "(?:\\.java|\\.kt|\\.js|\\.ts|\\.py):\\d+",
    "(?:sqlstate|sql\\s+exception|sqlexception|jdbc|hibernate|postgres|mysql|oracle)",
    "(?:constraint|relation|table|column)\\s+[\\\"'`]?[A-Za-z0-9_.-]+",
    "(?:^|\\s)at\\s+[A-Za-z0-9_.$]+\\([^)]*\\)",
    "(?:caused by:|stack trace|node_modules|exception in thread)",
  ].join("|"),
  "i",
);

function statusFallback(status?: number) {
  if (status === 401) return "Your session has expired. Please sign in again.";
  if (status === 403) return "You do not have permission to perform this action.";
  if (status === 404) return "The requested resource was not found.";
  if (status === 409) return "The request conflicts with the current data. Refresh and try again.";
  if (status === 413) return "The uploaded file is too large.";
  if (status === 429) return "Too many requests. Please wait and try again.";
  if (status === 503) return "The service is temporarily unavailable. Please try again.";
  return GENERIC_MESSAGE;
}

function containsControlCharacter(value: string) {
  for (const character of value) {
    const code = character.charCodeAt(0);
    if (code < 32 || code === 127) return true;
  }
  return false;
}

function safeMessage(value: unknown, fallback: string) {
  if (typeof value !== "string") return fallback;
  const message = value.trim();
  if (
    !message ||
    message.length > MAX_CLIENT_MESSAGE_LENGTH ||
    containsControlCharacter(message) ||
    SENSITIVE_DETAIL_PATTERN.test(message)
  ) {
    return fallback;
  }
  return message;
}

export function handleApiError(error: unknown): NormalizedApiError {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response;
    if (!response && ["ECONNABORTED", "ETIMEDOUT"].includes(error.code ?? "")) {
      return {
        message: "The request took too long on this connection. Check your network and try again.",
        fieldErrors: {},
      };
    }
    if (!response) {
      return {
        message: "The server could not be reached. Check your network connection and try again.",
        fieldErrors: {},
      };
    }
    const payload = response?.data;
    const fallback = statusFallback(response?.status);
    const fieldErrors: Record<string, string> = {};
    if (payload?.errors && !Array.isArray(payload.errors)) {
      for (const [field, value] of Object.entries(payload.errors)) {
        if (
          !NON_FIELD_ERROR_KEYS.has(field) &&
          /^[A-Za-z](?:[A-Za-z0-9_.-]|\[\d+\]){0,119}$/.test(field)
        ) {
          const message = safeMessage(value, "This value is invalid.");
          if (message !== "This value is invalid." || typeof value === "string") {
            fieldErrors[field] = message;
          }
        }
      }
    }
    return {
      message: safeMessage(payload?.message, fallback),
      status: response?.status,
      fieldErrors,
    };
  }
  return { message: GENERIC_MESSAGE, fieldErrors: {} };
}
