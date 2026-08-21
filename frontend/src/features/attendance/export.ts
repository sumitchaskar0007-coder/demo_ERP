import type { AttendanceReport } from "./api";
import { exportCollegeExcel } from "@/lib/collegeExcel";

const headers = [
  "Date",
  "Time",
  "Subject",
  "Department",
  "Class",
  "Teacher",
  "Present",
  "Absent",
  "Late",
  "Leave",
  "Percentage",
  "Status",
];
const rows = (report: AttendanceReport) =>
  report.rows.map((r) => [
    r.date,
    r.time,
    r.subject,
    r.department,
    `${r.year} - ${r.division}`,
    r.teacher,
    r.present,
    r.absent,
    r.late,
    r.leave,
    `${r.percentage}%`,
    r.status,
  ]);

export async function exportAttendancePdf(report: AttendanceReport, collegeName?: string) {
  const [{ default: JsPdf }, { default: autoTable }] = await Promise.all([
    import("jspdf"),
    import("jspdf-autotable"),
  ]);
  const document = new JsPdf({ orientation: "landscape", unit: "mm", format: "a4" });
  document.setFontSize(16);
  document.setFont("helvetica", "bold");
  document.text(collegeName || "College ERP Group of Institutes", 14, 14);
  document.setFontSize(13);
  document.text("Attendance Report", 14, 21);
  document.setFont("helvetica", "normal");
  document.setFontSize(9);
  document.text(
    `${report.from} to ${report.to}  |  Overall: ${report.percentage}%  |  ${report.rows.length} records`,
    14,
    27,
  );
  autoTable(document, {
    startY: 32,
    head: [headers],
    body: rows(report),
    styles: { fontSize: 7 },
    headStyles: { fillColor: [37, 99, 235] },
    didDrawPage: (data) => {
      document.setFontSize(8);
      document.text(
        `${collegeName || "College ERP Group of Institutes"} · Attendance Report`,
        14,
        document.internal.pageSize.height - 7,
      );
      document.text(
        `Page ${data.pageNumber}`,
        document.internal.pageSize.width - 24,
        document.internal.pageSize.height - 7,
      );
    },
  });
  document.save(`attendance-${report.from}-${report.to}.pdf`);
}

export async function exportAttendanceExcel(report: AttendanceReport, collegeName?: string) {
  await exportCollegeExcel({
    filename: `attendance-${report.from}-${report.to}.xlsx`,
    sheetName: "Attendance Register",
    title: "Attendance Report",
    collegeName,
    subtitle: "Official lecture-wise attendance register",
    metadata: [
      ["Reporting Period", `${report.from} to ${report.to}`],
      ["Overall Attendance", `${report.percentage}%`],
      ["Total Sessions", report.sessions],
    ],
    headers,
    rows: rows(report),
    widths: [12, 16, 24, 20, 20, 24, 10, 10, 8, 8, 12, 14],
    orientation: "landscape",
  });
}

export function exportAttendanceCsv(report: AttendanceReport) {
  const content = [headers, ...rows(report)]
    .map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(","))
    .join("\n");
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([content], { type: "text/csv" }));
  link.download = `attendance-${report.from}-${report.to}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}
