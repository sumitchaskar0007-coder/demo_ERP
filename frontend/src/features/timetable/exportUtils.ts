import { DAYS, DAY_LABELS } from "./constants";
import type { TimetableEntry } from "./types";
import { exportCollegeExcel } from "@/lib/collegeExcel";

export async function exportToPDF(
  entries: TimetableEntry[],
  title: string,
  periods: { number: number; start: string; end: string; label: string; isBreak?: boolean }[],
  collegeName?: string,
) {
  const jsPDFModule = await import("jspdf");
  const autoTableModule = await import("jspdf-autotable");
  const jsPDFClass = jsPDFModule.default;
  const autoTableFn = autoTableModule.default;

  const doc = new jsPDFClass({ orientation: "landscape", unit: "mm", format: "a4" });
  doc.setFontSize(16);
  doc.setFont("helvetica", "bold");
  doc.text(collegeName || "Jadhavar Group of Institutes", 14, 14);
  doc.setFontSize(12);
  doc.text(title, 14, 22);

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
    startY: 28,
    head: [["Period", "Time", ...DAYS.map((d) => DAY_LABELS[d])]],
    body,
    styles: { fontSize: 8, cellPadding: 3 },
    headStyles: { fillColor: [59, 130, 246] },
    didDrawPage: (hook) => {
      doc.setFontSize(8);
      doc.setFont("helvetica", "normal");
      doc.text(`${collegeName || "Jadhavar Group of Institutes"} · ${title}`, 14, doc.internal.pageSize.height - 7);
      doc.text(`Page ${hook.pageNumber}`, doc.internal.pageSize.width - 24, doc.internal.pageSize.height - 7);
    },
  });

  doc.save(`${title.replace(/\s+/g, "_")}.pdf`);
}

export async function exportToExcel(
  entries: TimetableEntry[],
  title: string,
  periods: { number: number; label: string; start: string; end: string; isBreak?: boolean }[],
  collegeName?: string,
) {
  const rows = periods
    .filter((p) => !p.isBreak)
    .map((p) => [
      p.label,
      `${p.start}\u2013${p.end}`,
      ...DAYS.map((day) => {
        const entry = entries.find((e) => e.dayOfWeek === day && e.periodNumber === p.number);
        return entry ? `${entry.subject || ""} (${entry.teacher || ""})` : "\u2014";
      }),
    ]);
  await exportCollegeExcel({
    filename: `${title.replace(/\s+/g, "_")}.xlsx`,
    sheetName: "Timetable",
    title,
    collegeName,
    subtitle: "Official weekly academic timetable",
    headers: ["Period", "Time", ...DAYS.map((day) => DAY_LABELS[day])],
    rows,
    widths: [16, 18, ...DAYS.map(() => 28)],
    orientation: "landscape",
  });
}
