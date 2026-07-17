import type { TimetableDay } from "./types";

export const DAYS: TimetableDay[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

export const DAY_LABELS: Record<TimetableDay, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
};

export const LECTURE_TYPES = [
  { label: "Theory", value: "THEORY" },
  { label: "Practical", value: "PRACTICAL" },
  { label: "Lab", value: "LAB" },
  { label: "Tutorial", value: "TUTORIAL" },
  { label: "Break", value: "BREAK" },
];

export const DEFAULT_PERIODS = [
  { number: 1, start: "08:30", end: "09:20", label: "Period 1" },
  { number: 2, start: "09:20", end: "10:10", label: "Period 2" },
  { number: 3, start: "10:10", end: "10:25", label: "Short Break", isBreak: true },
  { number: 4, start: "10:25", end: "11:15", label: "Period 3" },
  { number: 5, start: "11:15", end: "12:05", label: "Period 4" },
  { number: 6, start: "12:05", end: "12:45", label: "Lunch Break", isBreak: true },
  { number: 7, start: "12:45", end: "01:35", label: "Period 5" },
  { number: 8, start: "01:35", end: "02:25", label: "Period 6" },
  { number: 9, start: "02:25", end: "03:15", label: "Period 7" },
];

export const SUBJECT_COLORS = [
  "bg-blue-100 border-blue-300 text-blue-800",
  "bg-green-100 border-green-300 text-green-800",
  "bg-orange-100 border-orange-300 text-orange-800",
  "bg-purple-100 border-purple-300 text-purple-800",
  "bg-pink-100 border-pink-300 text-pink-800",
  "bg-teal-100 border-teal-300 text-teal-800",
  "bg-indigo-100 border-indigo-300 text-indigo-800",
  "bg-amber-100 border-amber-300 text-amber-800",
  "bg-cyan-100 border-cyan-300 text-cyan-800",
  "bg-rose-100 border-rose-300 text-rose-800",
];

export const BREAK_STYLE = "bg-slate-100 border-slate-200 text-slate-500";
export const EMPTY_CELL_STYLE = "bg-white hover:bg-slate-50 border-slate-100";

let colorIndex = 0;
const colorMap = new Map<number, string>();

export function getSubjectColor(subjectId: number): string {
  if (!colorMap.has(subjectId)) {
    colorMap.set(subjectId, SUBJECT_COLORS[colorIndex % SUBJECT_COLORS.length]);
    colorIndex++;
  }
  return colorMap.get(subjectId)!;
}
