import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { subscribeToNoticeChanges } from "./api";

class FakeEventSource {
  static instances: FakeEventSource[] = [];

  readonly url: string;
  readonly withCredentials: boolean;
  onopen: ((event: Event) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  private readonly listeners = new Map<string, EventListener[]>();
  readonly close = vi.fn();

  constructor(url: string | URL, init?: EventSourceInit) {
    this.url = String(url);
    this.withCredentials = init?.withCredentials ?? false;
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: EventListenerOrEventListenerObject) {
    const callback: EventListener =
      typeof listener === "function" ? listener : (event) => listener.handleEvent(event);
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), callback]);
  }

  emit(type: string, data: string) {
    const event = new MessageEvent(type, { data });
    for (const listener of this.listeners.get(type) ?? []) listener(event);
  }
}

describe("notice SSE subscription", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    Object.defineProperty(globalThis, "EventSource", {
      configurable: true,
      value: FakeEventSource,
    });
    Object.defineProperty(document, "visibilityState", {
      configurable: true,
      value: "visible",
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("uses credentials and delivers count and scoped change events", () => {
    const onUnreadCount = vi.fn();
    const onChange = vi.fn();
    const unsubscribe = subscribeToNoticeChanges({
      onUnreadCount,
      onChange,
      onReconnect: vi.fn(),
    });

    const source = FakeEventSource.instances[0];
    expect(source.url).toBe("/api/notices/stream");
    expect(source.withCredentials).toBe(true);

    source.emit("unread-count", "7");
    source.emit("notice-change", JSON.stringify({ type: "NOTICE_CREATED", noticeId: 42 }));

    expect(onUnreadCount).toHaveBeenCalledWith(7);
    expect(onChange).toHaveBeenCalledWith({ type: "NOTICE_CREATED", noticeId: 42 });
    unsubscribe();
    expect(source.close).toHaveBeenCalled();
  });

  it("backs off after errors and pauses connections while hidden", () => {
    const onReconnect = vi.fn();
    const unsubscribe = subscribeToNoticeChanges({
      onUnreadCount: vi.fn(),
      onChange: vi.fn(),
      onReconnect,
    });
    const first = FakeEventSource.instances[0];

    first.onerror?.(new Event("error"));
    expect(first.close).toHaveBeenCalled();
    vi.advanceTimersByTime(999);
    expect(FakeEventSource.instances).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(FakeEventSource.instances).toHaveLength(2);

    Object.defineProperty(document, "visibilityState", {
      configurable: true,
      value: "hidden",
    });
    document.dispatchEvent(new Event("visibilitychange"));
    const second = FakeEventSource.instances[1];
    expect(second.close).toHaveBeenCalled();
    vi.advanceTimersByTime(30_000);
    expect(FakeEventSource.instances).toHaveLength(2);

    Object.defineProperty(document, "visibilityState", {
      configurable: true,
      value: "visible",
    });
    document.dispatchEvent(new Event("visibilitychange"));
    expect(FakeEventSource.instances).toHaveLength(3);
    expect(onReconnect).toHaveBeenCalledTimes(1);
    unsubscribe();
  });
});
