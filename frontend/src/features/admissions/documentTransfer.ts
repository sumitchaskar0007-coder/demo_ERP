import axios from "axios";
import { apiClient } from "@/lib/apiClient";
import type { ApiErrorResponse, ApiResponse } from "@/types/api";
import type {
  AdmissionDocumentCompletionResponse,
  AdmissionDocumentDownloadUrlResponse,
  AdmissionDocumentTransferOptions,
  AdmissionDocumentUploadResponse,
} from "./types";

const DIRECT_TRANSFER_UNAVAILABLE_STATUSES = new Set([404, 405, 501]);
const MINIMUM_SIGNED_URL_LIFETIME_MS = 5_000;

interface MultipartUploadResult<T> {
  upload: (signal?: AbortSignal) => Promise<T>;
}

interface LegacyDownloadResult<T> {
  download: (signal?: AbortSignal) => Promise<T>;
}

export async function uploadAdmissionDocumentWithFallback<T>(
  endpoint: string,
  file: File,
  multipart: MultipartUploadResult<T>,
  options: AdmissionDocumentTransferOptions = {},
): Promise<T | AdmissionDocumentCompletionResponse> {
  throwIfAborted(options.signal);
  if (!directTransferEnabled(options)) {
    options.onProgress?.({ stage: "multipart-fallback" });
    const result = await multipart.upload(options.signal);
    options.onProgress?.({ stage: "completed" });
    return result;
  }

  options.onProgress?.({ stage: "hashing" });
  const sha256 = await sha256Hex(file, options.signal);
  const contentType = recognizedDocumentContentType(file);
  options.onProgress?.({ stage: "requesting-upload" });

  let uploadSession: AdmissionDocumentUploadResponse;
  try {
    const { data } = await apiClient.post<ApiResponse<AdmissionDocumentUploadResponse>>(
      `${endpoint}/presign`,
      {
        originalFilename: file.name,
        contentType,
        fileSize: file.size,
        sha256,
      },
      { signal: options.signal },
    );
    uploadSession = data.data;
  } catch (error) {
    // Falling back is safe only when the server explicitly says this route is
    // unavailable. A network error may mean a session was already created.
    if (!isDirectTransferUnavailable(error)) throw error;
    options.onProgress?.({ stage: "multipart-fallback" });
    const result = await multipart.upload(options.signal);
    options.onProgress?.({ stage: "completed" });
    return result;
  }

  validateUploadSession(uploadSession);
  options.onProgress?.({ stage: "uploading" });
  const uploadResponse = await fetch(uploadSession.uploadUrl, {
    method: "PUT",
    headers: signedHeaders(uploadSession.requiredHeaders),
    body: file,
    signal: options.signal,
    credentials: "omit",
    referrerPolicy: "no-referrer",
  });
  if (!uploadResponse.ok) {
    throw new Error(`Document storage upload failed with status ${uploadResponse.status}`);
  }

  throwIfAborted(options.signal);
  options.onProgress?.({ stage: "verifying" });
  const data = await completeAfterSecurityScan(
    endpoint,
    uploadSession,
    options.signal,
  );
  options.onProgress?.({ stage: "completed" });
  return data;
}

async function completeAfterSecurityScan(
  endpoint: string,
  session: AdmissionDocumentUploadResponse,
  signal?: AbortSignal,
) {
  const deadline = Date.parse(session.completionDeadline);
  for (;;) {
    throwIfAborted(signal);
    try {
      const { data } = await apiClient.post<ApiResponse<AdmissionDocumentCompletionResponse>>(
        `${endpoint}/complete`,
        { uploadId: session.uploadId },
        { signal },
      );
      return data.data;
    } catch (error) {
      if (!axios.isAxiosError(error) || error.response?.status !== 425) throw error;
      if (!Number.isFinite(deadline) || Date.now() + 500 >= deadline) {
        throw new Error("The document security scan did not finish before the upload expired");
      }
      const retryHeader = Number(error.response.headers?.["retry-after"]);
      const delayMs = Number.isFinite(retryHeader)
        ? Math.min(5_000, Math.max(500, retryHeader * 1_000))
        : 2_000;
      await abortableDelay(delayMs, signal);
    }
  }
}

function abortableDelay(milliseconds: number, signal?: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Upload cancelled", "AbortError"));
      return;
    }
    const timer = globalThis.setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, milliseconds);
    const onAbort = () => {
      globalThis.clearTimeout(timer);
      reject(new DOMException("Upload cancelled", "AbortError"));
    };
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

export async function resolveAdmissionDocumentDownload<T>(
  endpoint: string,
  legacy: LegacyDownloadResult<T>,
  options: Pick<AdmissionDocumentTransferOptions, "signal" | "directTransferEnabled"> = {},
): Promise<
  { direct: true; value: AdmissionDocumentDownloadUrlResponse } | { direct: false; value: T }
> {
  throwIfAborted(options.signal);
  if (!directTransferEnabled(options)) {
    return { direct: false, value: await legacy.download(options.signal) };
  }

  try {
    const { data } = await apiClient.get<ApiResponse<AdmissionDocumentDownloadUrlResponse>>(
      `${endpoint}/download-url`,
      { signal: options.signal },
    );
    validateDownload(data.data);
    return { direct: true, value: data.data };
  } catch (error) {
    if (!isDirectTransferUnavailable(error) && !isLegacyDocumentResponse(error)) throw error;
    return { direct: false, value: await legacy.download(options.signal) };
  }
}

export async function sha256Hex(file: File, signal?: AbortSignal): Promise<string> {
  throwIfAborted(signal);
  if (!globalThis.crypto?.subtle) {
    throw new Error("Secure document hashing is not supported by this browser");
  }
  const contents = await file.arrayBuffer();
  throwIfAborted(signal);
  const digest = await globalThis.crypto.subtle.digest("SHA-256", contents);
  throwIfAborted(signal);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function directTransferEnabled(
  options: Pick<AdmissionDocumentTransferOptions, "directTransferEnabled">,
) {
  if (options.directTransferEnabled !== undefined) return options.directTransferEnabled;
  return import.meta.env.VITE_ADMISSION_DIRECT_TRANSFER_ENABLED === "true";
}

function recognizedDocumentContentType(file: File) {
  const normalized = file.type.trim().toLowerCase();
  if (
    normalized === "application/pdf" ||
    normalized === "image/jpeg" ||
    normalized === "image/png"
  ) {
    return normalized;
  }
  const filename = file.name.toLowerCase();
  if (filename.endsWith(".pdf")) return "application/pdf";
  if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
  if (filename.endsWith(".png")) return "image/png";
  return normalized;
}

function signedHeaders(requiredHeaders: Record<string, string[]>) {
  if (!requiredHeaders || typeof requiredHeaders !== "object" || Array.isArray(requiredHeaders)) {
    throw new Error("The document upload URL returned invalid signed headers");
  }
  const headers = new Headers();
  for (const [name, values] of Object.entries(requiredHeaders)) {
    if (
      !name ||
      !Array.isArray(values) ||
      values.length === 0 ||
      values.some((value) => typeof value !== "string")
    ) {
      throw new Error("The document upload URL returned invalid signed headers");
    }
    for (const value of values) headers.append(name, value);
  }
  return headers;
}

function validateUploadSession(session: AdmissionDocumentUploadResponse) {
  if (!session?.uploadId) throw new Error("The document upload session is invalid");
  requireUsableHttpsUrl(session.uploadUrl, session.uploadUrlExpiresAt, "upload");
  const completionDeadline = Date.parse(session.completionDeadline);
  if (!Number.isFinite(completionDeadline) || completionDeadline <= Date.now()) {
    throw new Error("The document upload session has expired");
  }
}

function validateDownload(download: AdmissionDocumentDownloadUrlResponse) {
  requireUsableHttpsUrl(download?.downloadUrl, download?.expiresAt, "download");
  if (!download.originalFilename || !download.contentType || download.fileSize < 0) {
    throw new Error("The document download response is invalid");
  }
}

function requireUsableHttpsUrl(urlValue: string, expiresAt: string, purpose: string) {
  let url: URL;
  try {
    url = new URL(urlValue);
  } catch {
    throw new Error(`The document ${purpose} URL is invalid`);
  }
  if (url.protocol !== "https:" || url.username || url.password) {
    throw new Error(`The document ${purpose} URL is not secure`);
  }
  const expiry = Date.parse(expiresAt);
  if (!Number.isFinite(expiry) || expiry <= Date.now() + MINIMUM_SIGNED_URL_LIFETIME_MS) {
    throw new Error(`The document ${purpose} URL has expired`);
  }
}

function isDirectTransferUnavailable(error: unknown) {
  return (
    axios.isAxiosError(error) &&
    error.response !== undefined &&
    DIRECT_TRANSFER_UNAVAILABLE_STATUSES.has(error.response.status)
  );
}

function isLegacyDocumentResponse(error: unknown) {
  if (!axios.isAxiosError<ApiErrorResponse>(error) || error.response?.status !== 400) return false;
  return error.response.data?.message?.toLowerCase().includes("existing document endpoint");
}

function throwIfAborted(signal?: AbortSignal) {
  if (!signal?.aborted) return;
  throw new DOMException("The document upload was cancelled", "AbortError");
}
