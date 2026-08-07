import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type {
  AdmissionDocumentTransferOptions,
  AdmissionDocumentTransferStage,
  AdmissionDocumentType,
} from "@/features/admissions/types";
import { handleApiError } from "@/lib/handleApiError";

export type AdmissionUploadQueueState = {
  file: File;
  stage: "queued" | AdmissionDocumentTransferStage | "error" | "cancelled";
  message?: string;
};

type UploadTask = {
  type: AdmissionDocumentType;
  file: File;
  generation: number;
  controller: AbortController;
};

export function useAdmissionDocumentUploadQueue({
  upload,
  onUploaded,
  maxConcurrent = 2,
}: {
  upload: (
    type: AdmissionDocumentType,
    file: File,
    options: AdmissionDocumentTransferOptions,
  ) => Promise<unknown>;
  onUploaded: (type: AdmissionDocumentType) => void;
  maxConcurrent?: number;
}) {
  const [transfers, setTransfers] = useState<
    Partial<Record<AdmissionDocumentType, AdmissionUploadQueueState>>
  >({});
  const queue = useRef<UploadTask[]>([]);
  const activeCount = useRef(0);
  const generations = useRef(new Map<AdmissionDocumentType, number>());
  const activeControllers = useRef(new Map<AdmissionDocumentType, AbortController>());
  const uploadRef = useRef(upload);
  const onUploadedRef = useRef(onUploaded);
  const pumpRef = useRef<() => void>(() => undefined);

  uploadRef.current = upload;
  onUploadedRef.current = onUploaded;

  const isCurrent = (task: UploadTask) => generations.current.get(task.type) === task.generation;

  pumpRef.current = () => {
    while (activeCount.current < Math.max(1, maxConcurrent) && queue.current.length > 0) {
      const task = queue.current.shift();
      if (!task || !isCurrent(task) || task.controller.signal.aborted) continue;
      activeCount.current += 1;
      activeControllers.current.set(task.type, task.controller);
      void uploadRef
        .current(task.type, task.file, {
          signal: task.controller.signal,
          onProgress: ({ stage }) => {
            if (!isCurrent(task)) return;
            setTransfers((current) => ({
              ...current,
              [task.type]: { file: task.file, stage },
            }));
          },
        })
        .then(() => {
          if (!isCurrent(task)) return;
          setTransfers((current) => ({
            ...current,
            [task.type]: { file: task.file, stage: "completed" },
          }));
          onUploadedRef.current(task.type);
        })
        .catch((error) => {
          if (!isCurrent(task)) return;
          const cancelled = task.controller.signal.aborted;
          setTransfers((current) => ({
            ...current,
            [task.type]: {
              file: task.file,
              stage: cancelled ? "cancelled" : "error",
              message: cancelled
                ? "Upload cancelled. You can retry when ready."
                : handleApiError(error).message,
            },
          }));
        })
        .finally(() => {
          activeCount.current -= 1;
          if (activeControllers.current.get(task.type) === task.controller) {
            activeControllers.current.delete(task.type);
          }
          pumpRef.current();
        });
    }
  };

  const enqueue = useCallback((type: AdmissionDocumentType, file: File) => {
    activeControllers.current.get(type)?.abort();
    const generation = (generations.current.get(type) ?? 0) + 1;
    generations.current.set(type, generation);
    const task = { type, file, generation, controller: new AbortController() };
    queue.current = queue.current.filter((queued) => queued.type !== type);
    queue.current.push(task);
    setTransfers((current) => ({ ...current, [type]: { file, stage: "queued" } }));
    pumpRef.current();
  }, []);

  const cancel = useCallback((type: AdmissionDocumentType) => {
    activeControllers.current.get(type)?.abort();
    queue.current = queue.current.filter((task) => task.type !== type);
    setTransfers((current) => {
      const transfer = current[type];
      if (!transfer || transfer.stage === "completed") return current;
      return {
        ...current,
        [type]: {
          ...transfer,
          stage: "cancelled",
          message: "Upload cancelled. You can retry when ready.",
        },
      };
    });
  }, []);

  const retry = useCallback(
    (type: AdmissionDocumentType) => {
      const file = transfers[type]?.file;
      if (file) enqueue(type, file);
    },
    [enqueue, transfers],
  );

  const clear = useCallback((type: AdmissionDocumentType) => {
    activeControllers.current.get(type)?.abort();
    queue.current = queue.current.filter((task) => task.type !== type);
    generations.current.set(type, (generations.current.get(type) ?? 0) + 1);
    setTransfers((current) => {
      const next = { ...current };
      delete next[type];
      return next;
    });
  }, []);

  const reset = useCallback(() => {
    activeControllers.current.forEach((controller) => controller.abort());
    activeControllers.current.clear();
    queue.current = [];
    generations.current.clear();
    setTransfers({});
  }, []);

  useEffect(
    () => () => {
      activeControllers.current.forEach((controller) => controller.abort());
      queue.current = [];
    },
    [],
  );

  const hasPendingUploads = useMemo(
    () =>
      Object.values(transfers).some(
        (transfer) => transfer && !["completed", "error", "cancelled"].includes(transfer.stage),
      ),
    [transfers],
  );
  const hasFailedUploads = useMemo(
    () =>
      Object.values(transfers).some(
        (transfer) => transfer?.stage === "error" || transfer?.stage === "cancelled",
      ),
    [transfers],
  );

  return { transfers, enqueue, cancel, retry, clear, reset, hasPendingUploads, hasFailedUploads };
}
