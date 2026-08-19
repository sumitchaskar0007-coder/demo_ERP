import type { AcademicTerm, AcademicYear } from "./api";

export function localDateIso(value = new Date()) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function dateFallsInTerm(date: string, term: Pick<AcademicTerm, "startDate" | "endDate">) {
  return date >= term.startDate && date <= term.endDate;
}

export function calendarState(years: AcademicYear[], today: string) {
  const activeYear = years.find((year) => year.status === "ACTIVE") ?? null;
  const activeTerm = activeYear?.terms.find((term) => term.status === "ACTIVE") ?? null;
  const expectedTerm = activeYear?.terms.find((term) => dateFallsInTerm(today, term)) ?? null;
  return { activeYear, activeTerm, expectedTerm, mismatch: Boolean(expectedTerm && activeTerm?.id !== expectedTerm.id) };
}
