import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/apiClient";
import { weeklyTimetableApi, type WeeklyPeriodInput } from "./api";

vi.mock("@/lib/apiClient", () => ({
  apiClient: {
    put: vi.fn(),
  },
}));

const put = vi.mocked(apiClient.put);

describe("weekly timetable period updates", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    put.mockResolvedValue({ data: { data: {} } });
  });

  it("sends only request fields when periods originated from an API response", async () => {
    const responsePeriod = {
      id: 17,
      position: 1,
      label: "Period 1",
      startTime: "08:30:00",
      endTime: "09:20:00",
      kind: "TEACHING" as const,
    };

    await weeklyTimetableApi.updatePeriods(9, [responsePeriod as WeeklyPeriodInput]);

    expect(put).toHaveBeenCalledWith("/api/weekly-timetables/9/periods", {
      periods: [
        {
          id: 17,
          label: "Period 1",
          startTime: "08:30",
          endTime: "09:20",
          kind: "TEACHING",
        },
      ],
    });
    expect(put.mock.calls[0][1]).not.toEqual(
      expect.objectContaining({ periods: [expect.objectContaining({ position: 1 })] }),
    );
  });

  it("omits an absent id for a newly added period", async () => {
    await weeklyTimetableApi.updatePeriods(9, [
      {
        label: "Lunch",
        startTime: "12:30",
        endTime: "13:00",
        kind: "LUNCH_BREAK",
      },
    ]);

    expect(put).toHaveBeenCalledWith("/api/weekly-timetables/9/periods", {
      periods: [
        {
          label: "Lunch",
          startTime: "12:30",
          endTime: "13:00",
          kind: "LUNCH_BREAK",
        },
      ],
    });
  });
});
