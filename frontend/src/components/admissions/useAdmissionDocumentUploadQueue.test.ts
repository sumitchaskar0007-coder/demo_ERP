import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useAdmissionDocumentUploadQueue } from "./useAdmissionDocumentUploadQueue";

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe("useAdmissionDocumentUploadQueue", () => {
  it("uploads no more than two documents concurrently and starts the next queued file", async () => {
    const uploads = [deferred(), deferred(), deferred()];
    let uploadIndex = 0;
    const upload = vi.fn(() => uploads[uploadIndex++].promise);
    const onUploaded = vi.fn();
    const { result } = renderHook(() =>
      useAdmissionDocumentUploadQueue({ upload, onUploaded, maxConcurrent: 2 }),
    );

    act(() => {
      result.current.enqueue("DOC_A", new File(["a"], "a.pdf", { type: "application/pdf" }));
      result.current.enqueue("DOC_B", new File(["b"], "b.pdf", { type: "application/pdf" }));
      result.current.enqueue("DOC_C", new File(["c"], "c.pdf", { type: "application/pdf" }));
    });

    expect(upload).toHaveBeenCalledTimes(2);
    await act(async () => uploads[0].resolve());
    await waitFor(() => expect(upload).toHaveBeenCalledTimes(3));
    await act(async () => {
      uploads[1].resolve();
      uploads[2].resolve();
    });
    await waitFor(() => expect(onUploaded).toHaveBeenCalledTimes(3));
  });

  it("retains a failed file and retries only that document", async () => {
    const upload = vi
      .fn()
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce(undefined);
    const onUploaded = vi.fn();
    const { result } = renderHook(() => useAdmissionDocumentUploadQueue({ upload, onUploaded }));

    act(() => {
      result.current.enqueue(
        "AADHAAR_CARD",
        new File(["id"], "aadhaar.pdf", { type: "application/pdf" }),
      );
    });
    await waitFor(() => expect(result.current.transfers.AADHAAR_CARD?.stage).toBe("error"));

    act(() => result.current.retry("AADHAAR_CARD"));
    await waitFor(() => expect(result.current.transfers.AADHAAR_CARD?.stage).toBe("completed"));
    expect(upload).toHaveBeenCalledTimes(2);
    expect(onUploaded).toHaveBeenCalledWith("AADHAAR_CARD");
  });
});
