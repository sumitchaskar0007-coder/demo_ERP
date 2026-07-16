import type { WeeklyTimetable } from "./api";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

const titleFor = (table: WeeklyTimetable) =>
  `${table.department} ${table.year} Division ${table.division} Weekly Timetable`;

const cellText = (table: WeeklyTimetable, day: string, periodId: number) => {
  const entry = table.entries.find((item) => item.dayOfWeek === day && item.periodId === periodId);
  if (!entry) return "—";
  return [entry.subject, entry.teacher, entry.room, entry.lectureType].filter(Boolean).join("\n");
};

export async function exportWeeklyTimetablePdf(table: WeeklyTimetable) {
  const [{ default: JsPdf }, { default: autoTable }] = await Promise.all([
    import("jspdf"),
    import("jspdf-autotable"),
  ]);
  const document = new JsPdf({ orientation: "landscape", unit: "mm", format: "a4" });
  document.setFontSize(14);
  document.text(titleFor(table), 14, 15);
  document.setFontSize(9);
  document.text(
    `Academic year: ${table.academicYear}  |  Class teacher: ${table.classTeacher}`,
    14,
    21,
  );
  autoTable(document, {
    startY: 26,
    head: [["Period", "Time", ...DAYS.map((day) => day[0] + day.slice(1).toLowerCase())]],
    body: table.periods.map((period) =>
      period.kind === "TEACHING"
        ? [
            period.label,
            `${period.startTime.slice(0, 5)}–${period.endTime.slice(0, 5)}`,
            ...DAYS.map((day) => cellText(table, day, period.id)),
          ]
        : [
            period.label,
            `${period.startTime.slice(0, 5)}–${period.endTime.slice(0, 5)}`,
            ...DAYS.map(() => period.label.toUpperCase()),
          ],
    ),
    styles: { fontSize: 7, cellPadding: 2, valign: "middle" },
    headStyles: { fillColor: [37, 99, 235] },
  });
  document.save(`${titleFor(table).replace(/\s+/g, "_")}.pdf`);
}

export async function exportWeeklyTimetableExcel(table: WeeklyTimetable) {
  const XLSX = await import("xlsx");
  const rows = table.periods.map((period) => {
    const row: Record<string, string> = {
      Period: period.label,
      Time: `${period.startTime.slice(0, 5)}–${period.endTime.slice(0, 5)}`,
    };
    DAYS.forEach((day) => {
      row[day[0] + day.slice(1).toLowerCase()] =
        period.kind === "TEACHING"
          ? cellText(table, day, period.id).replaceAll("\n", " | ")
          : period.label.toUpperCase();
    });
    return row;
  });
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(rows);
  XLSX.utils.book_append_sheet(workbook, worksheet, "Weekly timetable");
  XLSX.writeFile(workbook, `${titleFor(table).replace(/\s+/g, "_")}.xlsx`);
}
