import { describe, expect, it } from "vitest";
import {
  getVisibleNoticeCollegeNames,
  isCollegeVisibleInNoticeScope,
} from "./noticeCollegeVisibility";

describe("notice college visibility", () => {
  it.each(["01", "09", "10", "19", "20"])(
    "hides Jadhavar Review College %s from the notice picker",
    (suffix) => {
      expect(
        isCollegeVisibleInNoticeScope({ name: `Jadhavar Review College ${suffix}` }),
      ).toBe(false);
    },
  );

  it("keeps real and newly added colleges visible", () => {
    expect(isCollegeVisibleInNoticeScope({ name: "Jadhavar College of Arts" })).toBe(true);
    expect(isCollegeVisibleInNoticeScope({ name: "New College" })).toBe(true);
  });

  it("removes review-college names saved on older sent notices", () => {
    expect(
      getVisibleNoticeCollegeNames([
        "Jadhavar Review College 01",
        "New College",
        "Jadhavar Review College 20",
      ]),
    ).toEqual(["New College"]);
  });
});
