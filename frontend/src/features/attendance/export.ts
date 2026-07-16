import type { AttendanceReport } from "./api";

const headers = ["Date", "Time", "Subject", "Department", "Class", "Teacher", "Present", "Absent", "Late", "Leave", "Percentage", "Status"];
const rows = (report: AttendanceReport) => report.rows.map((r) => [r.date, r.time, r.subject, r.department,
  `${r.year} - ${r.division}`, r.teacher, r.present, r.absent, r.late, r.leave, `${r.percentage}%`, r.status]);

export async function exportAttendancePdf(report: AttendanceReport) {
  const [{ default: JsPdf }, { default: autoTable }] = await Promise.all([import("jspdf"), import("jspdf-autotable")]);
  const document = new JsPdf({ orientation: "landscape", unit: "mm", format: "a4" });
  document.setFontSize(16); document.text("Attendance Report", 14, 15);
  document.setFontSize(9); document.text(`${report.from} to ${report.to}  |  Overall: ${report.percentage}%`, 14, 21);
  autoTable(document, { startY: 26, head: [headers], body: rows(report), styles: { fontSize: 7 }, headStyles: { fillColor: [79, 70, 229] } });
  document.save(`attendance-${report.from}-${report.to}.pdf`);
}

export async function exportAttendanceExcel(report: AttendanceReport) {
  const XLSX = await import("xlsx");
  const sheet = XLSX.utils.aoa_to_sheet([
    ["Attendance Report"], ["Period", `${report.from} to ${report.to}`], ["Overall", `${report.percentage}%`], [], headers, ...rows(report),
  ]);
  sheet["!cols"] = [{wch:12},{wch:16},{wch:24},{wch:20},{wch:20},{wch:24},{wch:10},{wch:10},{wch:8},{wch:8},{wch:12},{wch:12}];
  const workbook = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(workbook, sheet, "Attendance");
  XLSX.writeFile(workbook, `attendance-${report.from}-${report.to}.xlsx`);
}

export function exportAttendanceCsv(report: AttendanceReport) {
  const content = [headers, ...rows(report)].map((row) => row.map((value) => `"${String(value).replaceAll('"','""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([content], { type: "text/csv" }));
  link.download = `attendance-${report.from}-${report.to}.csv`; link.click(); URL.revokeObjectURL(link.href);
}
