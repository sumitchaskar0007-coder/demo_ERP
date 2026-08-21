export type ExcelCell = string | number | boolean | Date | null | undefined;

export interface CollegeExcelOptions {
  filename: string;
  sheetName: string;
  title: string;
  institution?: string;
  collegeName?: string;
  subtitle?: string;
  metadata?: Array<[string, ExcelCell]>;
  headers: string[];
  rows: ExcelCell[][];
  widths?: number[];
  orientation?: "portrait" | "landscape";
}

const textWidth = (value: ExcelCell) => Math.min(42, Math.max(10, String(value ?? "").length + 2));

/** Creates a consistent, print-ready college report instead of a plain data dump. */
export async function exportCollegeExcel(options: CollegeExcelOptions) {
  const { default: XLSX } = await import("xlsx-js-style");
  const columnCount = Math.max(1, options.headers.length);
  const generatedAt = new Date().toLocaleString("en-IN");
  const metadata = options.metadata ?? [];
  const collegeRow = options.collegeName ? 1 : null;
  const reportTitleRow = collegeRow === null ? 1 : 2;
  const subtitleRow = options.subtitle ? reportTitleRow + 1 : null;
  const generatedRow = (subtitleRow ?? reportTitleRow) + 1;
  const data: ExcelCell[][] = [
    [options.institution ?? "COLLEGE ERP GROUP OF INSTITUTES"],
    ...(options.collegeName ? [[options.collegeName.toUpperCase()]] : []),
    [options.title.toUpperCase()],
    ...(options.subtitle ? [[options.subtitle]] : []),
    [`Generated on: ${generatedAt}`],
    ...metadata.map(([label, value]) => [label, value]),
    [],
    options.headers,
    ...options.rows,
    [],
    [],
  ];
  const signatureRow = data.length;
  const middleColumn = Math.floor(columnCount / 3);
  const finalColumn = Math.floor((columnCount * 2) / 3);
  const signatures: ExcelCell[] = Array(columnCount).fill("");
  signatures[0] = "Prepared By";
  signatures[middleColumn] = "Verified By";
  signatures[finalColumn] = "Principal / Authorized Signatory";
  data.push(signatures);
  const sheet = XLSX.utils.aoa_to_sheet(data);
  const titleRows = generatedRow + 1;
  const headerRow = titleRows + metadata.length + 1;

  const merges = Array.from({ length: titleRows }, (_, row) => ({
    s: { r: row, c: 0 },
    e: { r: row, c: columnCount - 1 },
  }));
  sheet["!merges"] = merges;
  for (let row = titleRows; row < titleRows + metadata.length; row += 1) {
    if (columnCount > 2) merges.push({ s: { r: row, c: 1 }, e: { r: row, c: columnCount - 1 } });
  }
  const signatureStarts = [0, middleColumn, finalColumn];
  const signatureEnds = [middleColumn - 1, finalColumn - 1, columnCount - 1];
  signatureStarts.forEach((start, index) => {
    const end = signatureEnds[index];
    if (end !== undefined && end > start) {
      merges.push({ s: { r: signatureRow, c: start }, e: { r: signatureRow, c: end } });
    }
  });
  sheet["!cols"] = options.headers.map((header, column) => ({
    wch:
      options.widths?.[column] ??
      Math.max(
        textWidth(header),
        ...options.rows.slice(0, 100).map((row) => textWidth(row[column])),
      ),
  }));
  sheet["!rows"] = [
    ...Array.from({ length: titleRows }, (_, row) => ({
      hpt: row === 0 ? 30 : row === generatedRow ? 18 : row === subtitleRow ? 20 : 24,
    })),
    ...metadata.map(() => ({ hpt: 18 })),
    { hpt: 8 },
    { hpt: 24 },
    ...options.rows.map(() => ({ hpt: 20 })),
    { hpt: 10 },
    { hpt: 26 },
    { hpt: 24 },
  ];
  sheet["!autofilter"] = {
    ref: XLSX.utils.encode_range(
      { r: headerRow, c: 0 },
      { r: headerRow + options.rows.length, c: columnCount - 1 },
    ),
  };
  sheet["!freeze"] = { xSplit: 0, ySplit: headerRow + 1, topLeftCell: `A${headerRow + 2}` };
  sheet["!margins"] = { left: 0.3, right: 0.3, top: 0.5, bottom: 0.5, header: 0.2, footer: 0.2 };
  sheet["!pageSetup"] = {
    orientation: options.orientation ?? (columnCount > 7 ? "landscape" : "portrait"),
    fitToWidth: 1,
    fitToHeight: 0,
  };

  const thinBorder = {
    top: { style: "thin", color: { rgb: "CBD5E1" } },
    bottom: { style: "thin", color: { rgb: "CBD5E1" } },
    left: { style: "thin", color: { rgb: "CBD5E1" } },
    right: { style: "thin", color: { rgb: "CBD5E1" } },
  } as const;
  const styleCell = (row: number, column: number, style: object) => {
    const address = XLSX.utils.encode_cell({ r: row, c: column });
    if (sheet[address]) sheet[address].s = style;
  };

  styleCell(0, 0, {
    font: { name: "Calibri", sz: 18, bold: true, color: { rgb: "FFFFFF" } },
    fill: { patternType: "solid", fgColor: { rgb: "173B6C" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  if (collegeRow !== null)
    styleCell(collegeRow, 0, {
      font: { name: "Calibri", sz: 14, bold: true, color: { rgb: "FFFFFF" } },
      fill: { patternType: "solid", fgColor: { rgb: "2563EB" } },
      alignment: { horizontal: "center", vertical: "center" },
    });
  styleCell(reportTitleRow, 0, {
    font: {
      name: "Calibri",
      sz: 14,
      bold: true,
      color: { rgb: collegeRow === null ? "FFFFFF" : "173B6C" },
    },
    fill: { patternType: "solid", fgColor: { rgb: collegeRow === null ? "2563EB" : "DBEAFE" } },
    alignment: { horizontal: "center", vertical: "center" },
  });
  if (subtitleRow !== null) {
    styleCell(subtitleRow, 0, {
      font: { name: "Calibri", sz: 11, bold: true, italic: true, color: { rgb: "334155" } },
      fill: { patternType: "solid", fgColor: { rgb: "DBEAFE" } },
      alignment: { horizontal: "center", vertical: "center" },
    });
  }
  styleCell(generatedRow, 0, {
    font: { name: "Calibri", sz: 9, italic: true, color: { rgb: "64748B" } },
    alignment: { horizontal: "right", vertical: "center" },
  });
  metadata.forEach((_, index) => {
    const row = titleRows + index;
    styleCell(row, 0, {
      font: { name: "Calibri", sz: 10, bold: true, color: { rgb: "173B6C" } },
      fill: { patternType: "solid", fgColor: { rgb: "EFF6FF" } },
      border: thinBorder,
      alignment: { vertical: "center" },
    });
    styleCell(row, 1, {
      font: { name: "Calibri", sz: 10, color: { rgb: "0F172A" } },
      fill: { patternType: "solid", fgColor: { rgb: "F8FAFC" } },
      border: thinBorder,
      alignment: { vertical: "center" },
    });
  });
  for (let column = 0; column < columnCount; column += 1) {
    styleCell(headerRow, column, {
      font: { name: "Calibri", sz: 10, bold: true, color: { rgb: "FFFFFF" } },
      fill: { patternType: "solid", fgColor: { rgb: "2563EB" } },
      border: thinBorder,
      alignment: { horizontal: "center", vertical: "center", wrapText: true },
    });
  }
  options.rows.forEach((_, rowIndex) => {
    for (let column = 0; column < columnCount; column += 1) {
      styleCell(headerRow + 1 + rowIndex, column, {
        font: { name: "Calibri", sz: 10, color: { rgb: "1E293B" } },
        fill: { patternType: "solid", fgColor: { rgb: rowIndex % 2 === 0 ? "FFFFFF" : "F8FAFC" } },
        border: thinBorder,
        alignment: { vertical: "top", wrapText: true },
      });
    }
  });
  signatureStarts.forEach((column) => {
    styleCell(signatureRow, column, {
      font: { name: "Calibri", sz: 10, bold: true, color: { rgb: "173B6C" } },
      border: { top: { style: "thin", color: { rgb: "64748B" } } },
      alignment: { horizontal: "center", vertical: "center" },
    });
  });

  const workbook = XLSX.utils.book_new();
  workbook.Props = {
    Title: options.title,
    Subject: options.subtitle ?? options.title,
    Author: "College ERP",
    Company: options.institution ?? "College ERP Group of Institutes",
    CreatedDate: new Date(),
  };
  XLSX.utils.book_append_sheet(workbook, sheet, options.sheetName.slice(0, 31));
  XLSX.writeFile(workbook, options.filename, { compression: true });
}
