import { apiClient } from "@/lib/apiClient";
import type { FeeReceiptResponse } from "./types";

const formatDate = (value: string) => {
  const [year, month, day] = value.slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
};

const MOTTO = '"Education for Strength, Intellect & Wisdom"';
const FOUNDER = "- Prin. Dr. Sudhakarrao College ERP";
const AFFILIATION =
  "Affiliated to Savitribai Phule Pune University, Approved by AICTE, NAAC Accredited";

interface FeeReceiptBranding {
  leftLogoDataUrl?: string | null;
  rightLogoDataUrl?: string | null;
}

export function feeReceiptLogoUrls(receipt: FeeReceiptResponse) {
  return {
    leftLogoUrl: "/assets/college-erp-logo.png",
    rightLogoUrl: receipt.collegeLogoUrl?.trim() || null,
  };
}

export function feeReceiptHeader(receipt: FeeReceiptResponse) {
  const collegeName = receipt.collegeName.trim().toUpperCase();
  const collegeCode = receipt.collegeCode.trim().toUpperCase();
  return {
    motto: MOTTO,
    founder: FOUNDER,
    institutionName:
      collegeCode && !collegeName.includes(collegeCode)
        ? `${collegeName} - ${collegeCode}`
        : collegeName,
    affiliation: AFFILIATION,
  };
}

export function feeReceiptRows(receipt: FeeReceiptResponse) {
  const amount = Number(receipt.amount);
  const amountText = amount.toLocaleString("en-IN", {
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2,
  });
  const paymentMode =
    receipt.paymentMode === "UPI" || receipt.paymentMode === "BANK_TRANSFER"
      ? "Online"
      : receipt.paymentMode.charAt(0) + receipt.paymentMode.slice(1).toLowerCase();
  const reference = receipt.transactionReference.trim();
  const transactionDetails =
    receipt.paymentMode === "UPI"
      ? `UPI NO-${reference.replace(/^UPI(?:\s+NO)?(?:-|:|\s)*/i, "")}`
      : reference;

  return [
    ["Student Name-", receipt.studentName],
    ["Admitted Course Name-", receipt.courseName],
    ["Date of Fees Paid", formatDate(receipt.paymentDate).replaceAll("/", ".")],
    ["Received Amount in Rs", `${amountText}/-`],
    ["Mode of Payment", paymentMode],
    ["Transaction Details", transactionDetails],
  ];
}

export async function createFeeReceiptPdf(
  receipt: FeeReceiptResponse,
  branding?: FeeReceiptBranding | null,
) {
  const [jsPdfModule, { default: autoTable }] = await Promise.all([
    import("jspdf"),
    import("jspdf-autotable"),
  ]);
  const JsPdf = jsPdfModule.jsPDF;
  const doc = new JsPdf({ orientation: "portrait", unit: "mm", format: "a4" });
  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  const header = feeReceiptHeader(receipt);

  doc.setFillColor(255, 255, 255);
  doc.rect(0, 0, pageWidth, pageHeight, "F");

  const drawLogo = (
    dataUrl: string | null | undefined,
    x: number,
    y: number,
    maxWidth: number,
    maxHeight: number,
    alignRight = false,
  ) => {
    if (!dataUrl) return;
    try {
      const image = doc.getImageProperties(dataUrl);
      const scale = Math.min(maxWidth / image.width, maxHeight / image.height);
      const width = image.width * scale;
      const height = image.height * scale;
      doc.saveGraphicsState();
      try {
        doc.addImage(
          dataUrl,
          alignRight ? x - width : x,
          y + (maxHeight - height) / 2,
          width,
          height,
        );
      } finally {
        doc.restoreGraphicsState();
      }
    } catch {
      // A receipt must remain downloadable even when a configured logo is unavailable.
    }
  };

  drawLogo(branding?.leftLogoDataUrl, 12, 9, 43, 18);
  drawLogo(branding?.rightLogoDataUrl, pageWidth - 12, 7.5, 23, 22, true);

  doc.setTextColor(15, 23, 42);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(6.7);
  doc.text(header.motto, pageWidth / 2, 10.5, { align: "center" });
  doc.setFont("helvetica", "bold");
  doc.text(header.founder, pageWidth / 2, 14, { align: "center" });

  let institutionFontSize = 14;
  doc.setFont("helvetica", "bold");
  doc.setTextColor(24, 78, 150);
  doc.setFontSize(institutionFontSize);
  while (doc.getTextWidth(header.institutionName) > 128 && institutionFontSize > 10) {
    institutionFontSize -= 0.5;
    doc.setFontSize(institutionFontSize);
  }
  doc.text(header.institutionName, pageWidth / 2, 28, { align: "center" });

  doc.setTextColor(15, 23, 42);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(6.8);
  doc.text(header.affiliation, pageWidth / 2, 34, { align: "center", maxWidth: 150 });
  const address = [
    receipt.collegeAddress,
    receipt.collegeCity,
    receipt.collegeState,
    receipt.collegePincode,
  ]
    .filter(Boolean)
    .join(", ");
  const contacts = [receipt.collegeContactPhone, receipt.collegeContactEmail]
    .filter(Boolean)
    .join(" | ");
  doc.setFont("helvetica", "bold");
  doc.setFontSize(7);
  doc.text(
    [address || receipt.collegeCode, contacts].filter(Boolean).join("  |  "),
    pageWidth / 2,
    39.5,
    {
      align: "center",
      maxWidth: pageWidth - 30,
    },
  );
  doc.setDrawColor(30, 64, 175);
  doc.setLineWidth(0.5);
  doc.line(12, 43, pageWidth - 12, 43);

  doc.setFont("helvetica", "bold");
  doc.setFontSize(9.5);
  doc.text(`Issued: ${formatDate(receipt.issuedOn)}`, pageWidth - 15, 53, { align: "right" });
  doc.setFontSize(16);
  doc.text(receipt.receiptTitle, pageWidth / 2, 63, { align: "center" });
  doc.setFontSize(11);
  doc.text(`Academic Year ${receipt.academicYear}`, pageWidth / 2, 71, { align: "center" });
  doc.setFontSize(10);
  doc.text(receipt.courseName, pageWidth / 2, 78, { align: "center" });

  autoTable(doc, {
    startY: 86,
    margin: { left: 20, right: 20 },
    body: feeReceiptRows(receipt),
    theme: "grid",
    styles: {
      font: "helvetica",
      fontSize: 11,
      fontStyle: "bold",
      cellPadding: 4.5,
      minCellHeight: 14,
      lineColor: [15, 23, 42],
      lineWidth: 0.25,
      textColor: [15, 23, 42],
      overflow: "linebreak",
    },
    columnStyles: {
      0: { fillColor: [255, 255, 255], cellWidth: 85 },
      1: { cellWidth: "auto" },
    },
  });

  doc.setDrawColor(203, 213, 225);
  doc.line(20, 272, pageWidth - 20, 272);
  doc.setTextColor(71, 85, 105);
  doc.setFontSize(8.5);
  doc.text(
    "This is a computer-generated receipt and does not require a signature.",
    pageWidth / 2,
    279,
    {
      align: "center",
    },
  );
  doc.setProperties({
    title: `${receipt.receiptTitle} - ${receipt.receiptNumber}`,
    subject: `Verified payment receipt for ${receipt.studentName}`,
    author: receipt.collegeName,
    creator: "College ERP",
  });
  return doc;
}

async function apiImageDataUrl(url?: string | null) {
  if (!url) return null;
  try {
    const response = await apiClient.get<Blob>(url, { responseType: "blob" });
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(response.data);
    });
  } catch {
    return null;
  }
}

async function publicImageDataUrl(url: string) {
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const blob = await response.blob();
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(blob);
    });
  } catch {
    return null;
  }
}

export async function downloadFeeReceipt(receipt: FeeReceiptResponse) {
  const { leftLogoUrl, rightLogoUrl } = feeReceiptLogoUrls(receipt);
  const [leftLogoDataUrl, rightLogoDataUrl] = await Promise.all([
    publicImageDataUrl(leftLogoUrl),
    apiImageDataUrl(rightLogoUrl),
  ]);
  const doc = await createFeeReceiptPdf(receipt, {
    leftLogoDataUrl,
    rightLogoDataUrl,
  });
  doc.save(`${receipt.receiptNumber}.pdf`);
}
