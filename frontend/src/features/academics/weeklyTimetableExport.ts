import type { WeeklyTimetable } from "./api";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const DAY_LABELS = DAYS.map((day) => day[0] + day.slice(1).toLowerCase());

const titleFor = (table: WeeklyTimetable) =>
  `${table.department} - ${table.year} - ${table.division}`;

const safeFilename = (table: WeeklyTimetable, extension: "pdf" | "xlsx") =>
  `${table.college}_${table.department}_${table.year}_${table.division}_Timetable`
    .replace(/[^a-z0-9._-]+/gi, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 120) + `.${extension}`;

const timeText = (start: string, end: string) => `${start.slice(0, 5)} - ${end.slice(0, 5)}`;

const cellText = (table: WeeklyTimetable, day: string, periodId: number) => {
  const entry = table.entries.find((item) => item.dayOfWeek === day && item.periodId === periodId);
  if (!entry) return "-";
  const details = [entry.teacher, [entry.room, entry.lectureType].filter(Boolean).join(" | ")]
    .filter(Boolean)
    .join("\n");
  return details ? `${entry.subject}\n${details}` : entry.subject;
};

const metadataText = (table: WeeklyTimetable) =>
  `Academic year: ${table.academicYear} | Class teacher: ${table.classTeacher} | Status: ${table.status}`;

export async function exportWeeklyTimetablePdf(table: WeeklyTimetable) {
  const [jsPdfModule, autoTableModule] = await Promise.all([
    import("jspdf"),
    import("jspdf-autotable"),
  ]);
  const JsPdf = jsPdfModule.jsPDF;
  const autoTable = autoTableModule.autoTable;
  const document = new JsPdf({ orientation: "landscape", unit: "mm", format: "a4" });
  const pageWidth = document.internal.pageSize.getWidth();
  const pageHeight = document.internal.pageSize.getHeight();

  document.setFillColor(255, 255, 255);
  document.rect(0, 0, pageWidth, pageHeight, "F");

  document.setTextColor(15, 23, 42);
  document.setFont("helvetica", "bold");
  document.setFontSize(15);
  document.text(table.college, pageWidth / 2, 12, { align: "center", maxWidth: pageWidth - 24 });
  document.setFontSize(11);
  document.text("WEEKLY TIMETABLE", pageWidth / 2, 18, { align: "center" });
  document.setFont("helvetica", "normal");
  document.setFontSize(8.5);
  document.text(titleFor(table), pageWidth / 2, 23, {
    align: "center",
    maxWidth: pageWidth - 24,
  });
  document.setTextColor(71, 85, 105);
  document.text(metadataText(table), pageWidth / 2, 27.5, {
    align: "center",
    maxWidth: pageWidth - 24,
  });

  autoTable(document, {
    startY: 31,
    margin: { left: 8, right: 8, bottom: 12 },
    head: [["Period", "Time", ...DAY_LABELS]],
    body: table.periods.map((period) => [
      period.label,
      timeText(period.startTime, period.endTime),
      ...DAYS.map((day) =>
        period.kind === "TEACHING"
          ? cellText(table, day, period.id)
          : period.kind === "SHORT_BREAK"
            ? "SHORT BREAK"
            : "LUNCH BREAK",
      ),
    ]),
    theme: "grid",
    styles: {
      font: "helvetica",
      fontSize: 7,
      cellPadding: 2,
      valign: "middle",
      overflow: "linebreak",
      lineColor: [203, 213, 225],
      lineWidth: 0.15,
      minCellHeight: 14,
    },
    headStyles: {
      fillColor: [30, 64, 175],
      textColor: 255,
      fontStyle: "bold",
      halign: "center",
      minCellHeight: 9,
    },
    alternateRowStyles: { fillColor: [248, 250, 252] },
    columnStyles: {
      0: { cellWidth: 18, fontStyle: "bold" },
      1: { cellWidth: 21, halign: "center" },
    },
    didParseCell: (data) => {
      const period = data.section === "body" ? table.periods[data.row.index] : undefined;
      if (period?.kind !== "TEACHING") {
        data.cell.styles.fillColor = [255, 247, 237];
        data.cell.styles.textColor = [154, 52, 18];
        data.cell.styles.fontStyle = "bold";
        data.cell.styles.halign = "center";
      }
    },
    didDrawPage: () => {
      const pageNumber = document.getNumberOfPages();
      document.setFont("helvetica", "normal");
      document.setFontSize(7);
      document.setTextColor(100, 116, 139);
      document.text(`Generated ${new Date().toLocaleString("en-IN")}`, 8, pageHeight - 5);
      document.text(`Page ${pageNumber}`, pageWidth - 8, pageHeight - 5, { align: "right" });
    },
  });

  document.save(safeFilename(table, "pdf"));
}

export async function exportWeeklyTimetableExcel(table: WeeklyTimetable) {
  const { default: XLSX } = await import("xlsx-js-style");
  const headers = ["Period", "Time", ...DAY_LABELS];
  const rows = table.periods.map((period) => [
    period.label,
    timeText(period.startTime, period.endTime),
    ...DAYS.map((day) =>
      period.kind === "TEACHING"
        ? cellText(table, day, period.id)
        : period.kind === "SHORT_BREAK"
          ? "SHORT BREAK"
          : "LUNCH BREAK",
    ),
  ]);
  const worksheet = XLSX.utils.aoa_to_sheet([
    [table.college],
    ["WEEKLY TIMETABLE"],
    [titleFor(table)],
    [metadataText(table)],
    [],
    headers,
    ...rows,
  ]);
  worksheet["!merges"] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: 7 } },
    { s: { r: 1, c: 0 }, e: { r: 1, c: 7 } },
    { s: { r: 2, c: 0 }, e: { r: 2, c: 7 } },
    { s: { r: 3, c: 0 }, e: { r: 3, c: 7 } },
  ];
  worksheet["!cols"] = [{ wch: 17 }, { wch: 18 }, ...DAYS.map(() => ({ wch: 31 }))];
  worksheet["!rows"] = [
    { hpt: 26 },
    { hpt: 22 },
    { hpt: 20 },
    { hpt: 20 },
    { hpt: 8 },
    { hpt: 24 },
    ...table.periods.map((period) => ({ hpt: period.kind === "TEACHING" ? 48 : 24 })),
  ];
  worksheet["!autofilter"] = { ref: `A6:H${6 + rows.length}` };
  worksheet["!margins"] = { left: 0.25, right: 0.25, top: 0.4, bottom: 0.4 };
  worksheet["!pageSetup"] = { orientation: "landscape", fitToWidth: 1, fitToHeight: 1 };

  const border = {
    top: { style: "thin", color: { rgb: "CBD5E1" } },
    bottom: { style: "thin", color: { rgb: "CBD5E1" } },
    left: { style: "thin", color: { rgb: "CBD5E1" } },
    right: { style: "thin", color: { rgb: "CBD5E1" } },
  } as const;
  const style = (row: number, column: number, value: object) => {
    const cell = worksheet[XLSX.utils.encode_cell({ r: row, c: column })];
    if (cell) cell.s = value;
  };
  style(0, 0, {
    font: { name: "Calibri", sz: 18, bold: true, color: { rgb: "FFFFFF" } },
    fill: { patternType: "solid", fgColor: { rgb: "173B6C" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  style(1, 0, {
    font: { name: "Calibri", sz: 14, bold: true, color: { rgb: "FFFFFF" } },
    fill: { patternType: "solid", fgColor: { rgb: "2563EB" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  style(2, 0, {
    font: { name: "Calibri", sz: 12, bold: true, color: { rgb: "173B6C" } },
    fill: { patternType: "solid", fgColor: { rgb: "DBEAFE" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  style(3, 0, {
    font: { name: "Calibri", sz: 10, bold: true, color: { rgb: "475569" } },
    fill: { patternType: "solid", fgColor: { rgb: "F8FAFC" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  headers.forEach((_, column) =>
    style(5, column, {
      font: { name: "Calibri", sz: 10, bold: true, color: { rgb: "FFFFFF" } },
      fill: { patternType: "solid", fgColor: { rgb: "2563EB" } },
      border,
      alignment: { horizontal: "center", vertical: "center", wrapText: true },
    }),
  );
  rows.forEach((_, rowIndex) =>
    headers.forEach((__, column) =>
      style(6 + rowIndex, column, {
        font: { name: "Calibri", sz: 9, bold: column < 2, color: { rgb: "1E293B" } },
        fill: { patternType: "solid", fgColor: { rgb: rowIndex % 2 === 0 ? "FFFFFF" : "F8FAFC" } },
        border,
        alignment: {
          horizontal: column < 2 ? "center" : "left",
          vertical: "center",
          wrapText: true,
        },
      }),
    ),
  );

  const workbook = XLSX.utils.book_new();
  workbook.Props = {
    Title: `${table.college} Weekly Timetable`,
    Subject: titleFor(table),
    Author: "Jadhavar ERP",
    CreatedDate: new Date(),
  };
  XLSX.utils.book_append_sheet(workbook, worksheet, "Weekly Timetable");
  XLSX.writeFile(workbook, safeFilename(table, "xlsx"), { compression: true });
}
