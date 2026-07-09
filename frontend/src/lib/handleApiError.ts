import axios from "axios";
import type { ApiErrorResponse } from "@/types/api";

export interface NormalizedApiError {
  message: string;
  status?: number;
  fieldErrors: Record<string, string>;
}

export function handleApiError(error: unknown): NormalizedApiError {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response;
    const payload = response?.data;
    const fieldErrors: Record<string, string> = {};
    if (payload?.errors && !Array.isArray(payload.errors)) {
      Object.assign(fieldErrors, payload.errors);
    }
    return {
      message: payload?.message || "The request could not be completed.",
      status: response?.status,
      fieldErrors,
    };
  }
  if (error instanceof Error) {
    return { message: error.message, fieldErrors: {} };
  }
  return { message: "An unexpected error occurred.", fieldErrors: {} };
}
