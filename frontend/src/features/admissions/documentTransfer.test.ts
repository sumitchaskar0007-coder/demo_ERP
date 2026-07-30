import { webcrypto } from "node:crypto";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/apiClient";
import {
  resolveAdmissionDocumentDownload,
  uploadAdmissionDocumentWithFallback,
} from "./documentTransfer";

vi.mock("@/lib/apiClient", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const post = vi.mocked(apiClient.post);
const get = vi.mocked(apiClient.get);
const future = () => new Date(Date.now() + 60_000).toISOString();

function documentFile(contents = "hello") {
  const bytes = new TextEncoder().encode(contents);
  const file = new File([bytes], "marksheet.pdf", { type: "application/pdf" });
  Object.defineProperty(file, "arrayBuffer", {
    configurable: true,
    value: vi.fn(async () => bytes.buffer),
  });
  return file;
}

function axiosError(status?: number, message?: string) {
  return {
    isAxiosError: true,
    response:
      status === undefined
        ? undefined
        : {
            status,
            data: {
              success: false,
              message: message ?? "Request failed",
            },
          },
  };
}

describe("admission document direct transfer", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("crypto", webcrypto);
    vi.stubGlobal("fetch", vi.fn());
  });

  it("hashes, PUTs with only signed headers, and completes the same upload session", async () => {
    const requiredHeaders = {
      "content-type": ["application/pdf"],
      "x-amz-checksum-sha256": ["LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ="],
    };
    post
      .mockResolvedValueOnce({
        data: {
          data: {
            uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df",
            uploadUrl: "https://uploads.example.test/signed",
            requiredHeaders,
            uploadUrlExpiresAt: future(),
            completionDeadline: future(),
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: {
            documentId: 9,
            documentType: "TENTH_MARKSHEET",
            originalFilename: "marksheet.pdf",
            contentType: "application/pdf",
            fileSize: 5,
            sha256: "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            verifiedAt: new Date().toISOString(),
          },
        },
      });
    const fetchMock = vi.mocked(fetch).mockResolvedValue({
      ok: true,
      status: 200,
    } as Response);
    const multipart = { upload: vi.fn() };
    const stages: string[] = [];

    const result = await uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      documentFile(),
      multipart,
      {
        directTransferEnabled: true,
        onProgress: ({ stage }) => stages.push(stage),
      },
    );

    expect(post).toHaveBeenNthCalledWith(
      1,
      "/api/student/admissions/me/documents/TENTH_MARKSHEET/presign",
      {
        originalFilename: "marksheet.pdf",
        contentType: "application/pdf",
        fileSize: 5,
        sha256: "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      },
      { signal: undefined },
    );
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("https://uploads.example.test/signed");
    expect(init?.method).toBe("PUT");
    expect(init?.credentials).toBe("omit");
    expect(Array.from(new Headers(init?.headers).entries())).toEqual([
      ["content-type", "application/pdf"],
      ["x-amz-checksum-sha256", "LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ="],
    ]);
    expect(post).toHaveBeenNthCalledWith(
      2,
      "/api/student/admissions/me/documents/TENTH_MARKSHEET/complete",
      { uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df" },
      { signal: undefined },
    );
    expect(multipart.upload).not.toHaveBeenCalled();
    expect(stages).toEqual(["hashing", "requesting-upload", "uploading", "verifying", "completed"]);
    expect(result).toMatchObject({ documentId: 9 });
  });

  it("uses multipart when the direct route is explicitly disabled", async () => {
    const multipart = { upload: vi.fn().mockResolvedValue({ id: 42 }) };
    const onProgress = vi.fn();

    const result = await uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      documentFile(),
      multipart,
      { directTransferEnabled: false, onProgress },
    );

    expect(post).not.toHaveBeenCalled();
    expect(fetch).not.toHaveBeenCalled();
    expect(multipart.upload).toHaveBeenCalledOnce();
    expect(onProgress).toHaveBeenNthCalledWith(1, { stage: "multipart-fallback" });
    expect(onProgress).toHaveBeenNthCalledWith(2, { stage: "completed" });
    expect(result).toEqual({ id: 42 });
  });

  it("uses multipart by default unless direct S3 transfer is explicitly enabled", async () => {
    const multipart = { upload: vi.fn().mockResolvedValue({ id: 43 }) };

    const result = await uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      documentFile(),
      multipart,
    );

    expect(post).not.toHaveBeenCalled();
    expect(fetch).not.toHaveBeenCalled();
    expect(multipart.upload).toHaveBeenCalledOnce();
    expect(result).toEqual({ id: 43 });
  });

  it("infers a PDF content type when the browser leaves File.type empty", async () => {
    const file = documentFile();
    Object.defineProperty(file, "type", { configurable: true, value: "" });
    post
      .mockResolvedValueOnce({
        data: {
          data: {
            uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df",
            uploadUrl: "https://uploads.example.test/signed",
            requiredHeaders: { "content-type": ["application/pdf"] },
            uploadUrlExpiresAt: future(),
            completionDeadline: future(),
          },
        },
      })
      .mockResolvedValueOnce({ data: { data: { documentId: 9 } } });
    vi.mocked(fetch).mockResolvedValueOnce({ ok: true, status: 200 } as Response);

    await uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      file,
      { upload: vi.fn() },
      { directTransferEnabled: true },
    );

    expect(post).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining("/presign"),
      expect.objectContaining({ contentType: "application/pdf" }),
      { signal: undefined },
    );
  });

  it("falls back only for an explicit unavailable response before a session exists", async () => {
    post.mockRejectedValueOnce(axiosError(404));
    const multipart = { upload: vi.fn().mockResolvedValue({ id: 42 }) };

    await uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      documentFile(),
      multipart,
      { directTransferEnabled: true },
    );

    expect(multipart.upload).toHaveBeenCalledOnce();
    expect(fetch).not.toHaveBeenCalled();
  });

  it.each([
    ["an ambiguous presign network failure", axiosError()],
    ["a storage PUT failure", new Error("storage failure")],
  ])("never silently retries %s through multipart", async (_label, failure) => {
    const multipart = { upload: vi.fn() };
    if ("isAxiosError" in failure) {
      post.mockRejectedValueOnce(failure);
    } else {
      post.mockResolvedValueOnce({
        data: {
          data: {
            uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df",
            uploadUrl: "https://uploads.example.test/signed",
            requiredHeaders: { "content-type": ["application/pdf"] },
            uploadUrlExpiresAt: future(),
            completionDeadline: future(),
          },
        },
      });
      vi.mocked(fetch).mockRejectedValueOnce(failure);
    }

    await expect(
      uploadAdmissionDocumentWithFallback(
        "/api/student/admissions/me/documents/TENTH_MARKSHEET",
        documentFile(),
        multipart,
        { directTransferEnabled: true },
      ),
    ).rejects.toBe(failure);

    expect(multipart.upload).not.toHaveBeenCalled();
  });

  it("does not fallback after S3 returns a failed PUT response", async () => {
    post.mockResolvedValueOnce({
      data: {
        data: {
          uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df",
          uploadUrl: "https://uploads.example.test/signed",
          requiredHeaders: { "content-type": ["application/pdf"] },
          uploadUrlExpiresAt: future(),
          completionDeadline: future(),
        },
      },
    });
    vi.mocked(fetch).mockResolvedValueOnce({ ok: false, status: 403 } as Response);
    const multipart = { upload: vi.fn() };

    await expect(
      uploadAdmissionDocumentWithFallback(
        "/api/student/admissions/me/documents/TENTH_MARKSHEET",
        documentFile(),
        multipart,
        { directTransferEnabled: true },
      ),
    ).rejects.toThrow("status 403");

    expect(post).toHaveBeenCalledOnce();
    expect(multipart.upload).not.toHaveBeenCalled();
  });

  it("aborts an in-flight storage PUT without completing or falling back", async () => {
    post.mockResolvedValueOnce({
      data: {
        data: {
          uploadId: "e785d57a-1b16-45f4-b7ea-03965dff81df",
          uploadUrl: "https://uploads.example.test/signed",
          requiredHeaders: { "content-type": ["application/pdf"] },
          uploadUrlExpiresAt: future(),
          completionDeadline: future(),
        },
      },
    });
    vi.mocked(fetch).mockImplementationOnce(
      (_url, init) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener(
            "abort",
            () => reject(new DOMException("Upload cancelled", "AbortError")),
            { once: true },
          );
        }),
    );
    const controller = new AbortController();
    const multipart = { upload: vi.fn() };
    const upload = uploadAdmissionDocumentWithFallback(
      "/api/student/admissions/me/documents/TENTH_MARKSHEET",
      documentFile(),
      multipart,
      { directTransferEnabled: true, signal: controller.signal },
    );

    await vi.waitFor(() => expect(fetch).toHaveBeenCalledOnce());
    controller.abort();

    await expect(upload).rejects.toMatchObject({ name: "AbortError" });
    expect(post).toHaveBeenCalledOnce();
    expect(multipart.upload).not.toHaveBeenCalled();
  });
});

describe("admission document download resolution", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns a short-lived HTTPS URL from the scoped API", async () => {
    get.mockResolvedValueOnce({
      data: {
        data: {
          downloadUrl: "https://downloads.example.test/signed",
          expiresAt: future(),
          originalFilename: "marksheet.pdf",
          contentType: "application/pdf",
          fileSize: 5,
          sha256: "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        },
      },
    });
    const legacy = { download: vi.fn() };

    const result = await resolveAdmissionDocumentDownload(
      "/api/student-section/admissions/42/documents/TENTH_MARKSHEET",
      legacy,
      { directTransferEnabled: true },
    );

    expect(result).toMatchObject({
      direct: true,
      value: { downloadUrl: "https://downloads.example.test/signed" },
    });
    expect(legacy.download).not.toHaveBeenCalled();
  });

  it("uses the legacy endpoint for an explicitly identified legacy document", async () => {
    get.mockRejectedValueOnce(
      axiosError(400, "This document must be downloaded through the existing document endpoint"),
    );
    const legacy = { download: vi.fn().mockResolvedValue("blob:legacy") };

    const result = await resolveAdmissionDocumentDownload(
      "/api/student-section/admissions/42/documents/TENTH_MARKSHEET",
      legacy,
      { directTransferEnabled: true },
    );

    expect(result).toEqual({ direct: false, value: "blob:legacy" });
    expect(legacy.download).toHaveBeenCalledOnce();
  });

  it("rejects unsafe URLs instead of opening them", async () => {
    get.mockResolvedValueOnce({
      data: {
        data: {
          downloadUrl: "javascript:alert(1)",
          expiresAt: future(),
          originalFilename: "marksheet.pdf",
          contentType: "application/pdf",
          fileSize: 5,
          sha256: "checksum",
        },
      },
    });

    await expect(
      resolveAdmissionDocumentDownload(
        "/api/student-section/admissions/42/documents/TENTH_MARKSHEET",
        { download: vi.fn() },
        { directTransferEnabled: true },
      ),
    ).rejects.toThrow("not secure");
  });
});
