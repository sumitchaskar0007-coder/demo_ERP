import { describe, expect, it } from "vitest";
import {
  createFeeReceiptPdf,
  feeReceiptHeader,
  feeReceiptLogoUrls,
  feeReceiptRows,
} from "./feeReceiptPdf";
import type { FeeReceiptResponse } from "./types";

const receipt: FeeReceiptResponse = {
  paymentId: 31,
  receiptNumber: "RCP-AIMS-2026-000031",
  receiptTitle: "Fee Receipt",
  issuedOn: "2026-06-24",
  collegeId: 7,
  collegeCode: "AIMS",
  collegeName: "Aditya Institute of Management",
  collegeAddress: "Narhe",
  collegeCity: "Pune",
  collegeState: "Maharashtra",
  collegePincode: "411041",
  collegeContactEmail: "admission@example.test",
  collegeContactPhone: "9356399629",
  studentName: "Test Student",
  admissionNumber: "AIMS-2026-001",
  prn: "PRN-1001",
  departmentName: "MBA",
  courseName: "MBA - First Year",
  academicYear: "2026-2027",
  paymentDate: "2026-06-05",
  amount: 1000,
  paymentMode: "UPI",
  transactionReference: "UTR123456789012",
  verifiedAt: "2026-06-24T10:30:00",
  verifiedByName: "Fee Officer",
  remark: "Verified provisional admission payment.",
};

describe("fee receipt PDF", () => {
  it("uses Jadhavar on the left and only the configured college logo on the right", () => {
    expect(
      feeReceiptLogoUrls({ ...receipt, collegeLogoUrl: "/api/public/colleges/AIMS/logo" }),
    ).toEqual({
      leftLogoUrl: "/assets/jadhavar-logo.png",
      rightLogoUrl: "/api/public/colleges/AIMS/logo",
    });
    expect(feeReceiptLogoUrls({ ...receipt, collegeLogoUrl: null }).rightLogoUrl).toBeNull();
  });

  it("builds the complete two-logo institute header from the reference structure", () => {
    expect(feeReceiptHeader(receipt)).toEqual({
      foundationName: "Aditya Educational Foundation's",
      motto: '"Education for Strength, Intellect & Wisdom"',
      founder: "- Prin. Dr. Sudhakarrao Jadhavar",
      institutionName: "ADITYA INSTITUTE OF MANAGEMENT - AIMS",
      affiliation:
        "Affiliated to Savitribai Phule Pune University, Approved by AICTE, NAAC Accredited",
    });
  });

  it("contains only the six requested receipt details", () => {
    expect(feeReceiptRows(receipt)).toEqual([
      ["Student Name-", "Test Student"],
      ["Admitted Course Name-", "MBA - First Year"],
      ["Date of Fees Paid", "05.06.2026"],
      ["Received Amount in Rs", "1,000/-"],
      ["Mode of Payment", "Online"],
      ["Transaction Details", "UPI NO-UTR123456789012"],
    ]);
  });

  it("creates a valid PDF from verified receipt data", async () => {
    const pdf = await createFeeReceiptPdf(receipt, null);
    const bytes = new Uint8Array(pdf.output("arraybuffer"));

    expect(new TextDecoder().decode(bytes.slice(0, 5))).toBe("%PDF-");
    expect(bytes.length).toBeGreaterThan(5_000);
    expect(pdf.output()).not.toContain(receipt.remark);
  });
});
