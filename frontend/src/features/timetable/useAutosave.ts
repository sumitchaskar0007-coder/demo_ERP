import { useEffect, useRef } from "react";

export function useAutosave(callback: () => void | Promise<void>, delay = 3000) {
  const savedRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>();
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  const schedule = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(async () => {
      await callbackRef.current();
      savedRef.current = true;
    }, delay);
  };

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  return { schedule, savedRef };
}
