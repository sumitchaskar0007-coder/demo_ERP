import type { College } from "@/features/colleges/types";

const HIDDEN_NOTICE_COLLEGE_NAMES = new Set(
  Array.from(
    { length: 20 },
    (_, index) => `College ERP Review College ${String(index + 1).padStart(2, "0")}`,
  ),
);

export function isCollegeVisibleInNoticeScope(college: Pick<College, "name">) {
  return isNoticeCollegeNameVisible(college.name);
}

export function isNoticeCollegeNameVisible(name: string) {
  return !HIDDEN_NOTICE_COLLEGE_NAMES.has(name.trim());
}

export function getVisibleNoticeCollegeNames(names: string[]) {
  return names.filter(isNoticeCollegeNameVisible);
}
