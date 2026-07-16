import { DAYS, DAY_LABELS } from "./constants";
import type { TimetableEntry } from "./types";

export async function exportToPDF(
  entries: TimetableEntry[],
  title: string,
  periods: { number: number; start: string; end: string; label: string; isBreak?: boolean }[],
) {
  const jsPDFModule = await import("jspdf");
  const autoTableModule = await import("jspdf-autotable");
  const jsPDFClass = jsPDFModule.default;
  const autoTableFn = autoTableModule.default;

  const doc = new jsPDFClass({ orientation: "landscape", unit: "mm", format: "a4" });
  doc.setFontSize(14);
  doc.text(title, 14, 15);

  const body = periods
    .filter((p) => !p.isBreak)
    .map((p) => {
      const row = [p.label, p.start + "\u2013" + p.end];
      DAYS.forEach((day) => {
        const entry = entries.find((e) => e.dayOfWeek === day && e.periodNumber === p.number);
        row.push(entry ? `${entry.subject || ""} ${entry.teacher || ""}` : "\u2014");
      });
      return row;
    });

  autoTableFn(doc, {
    startY: 22,
    head: [["Period", "Time", ...DAYS.map((d) => DAY_LABELS[d])]],
    body,
    styles: { fontSize: 8, cellPadding: 3 },
    headStyles: { fillColor: [59, 130, 246] },
  });

  doc.save(`${title.replace(/\s+/g, "_")}.pdf`);
}

export async function exportToExcel(
  entries: TimetableEntry[],
  title: string,
  periods: { number: number; label: string; start: string; end: string; isBreak?: boolean }[],
) {
  const XLSX = await import("xlsx");

  const data = periods
    .filter((p) => !p.isBreak)
    .map((p) => {
      const row: Record<string, string> = { Period: p.label, Time: `${p.start}\u2013${p.end}` };
      DAYS.forEach((day) => {
        const entry = entries.find((e) => e.dayOfWeek === day && e.periodNumber === p.number);
        row[DAY_LABELS[day]] = entry ? `${entry.subject || ""} (${entry.teacher || ""})` : "\u2014";
      });
      return row;
    });

  const ws = XLSX.utils.json_to_sheet(data);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Timetable");
  XLSX.writeFile(wb, `${title.replace(/\s+/g, "_")}.xlsx`);
}
