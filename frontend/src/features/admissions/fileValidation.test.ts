import { describe, expect, it } from "vitest";
import {
  normalizeAdmissionPhotoUpload,
  validateAdmissionDocumentFile,
  validateAdmissionPhotoFile,
} from "./fileValidation";

function file(name: string, type: string, size = 10) {
  return new File([new Uint8Array(size)], name, { type });
}

describe("admission file validation", () => {
  it("accepts supported mobile photo and document formats", () => {
    expect(validateAdmissionPhotoFile(file("camera.jpg", "image/jpeg"))).toBeNull();
    expect(validateAdmissionDocumentFile(file("marksheet.pdf", "application/pdf"))).toBeNull();
  });

  it("accepts supported extensions when a mobile browser omits the MIME type", () => {
    expect(validateAdmissionPhotoFile(file("camera.png", ""))).toBeNull();
    expect(validateAdmissionDocumentFile(file("aadhaar.jpeg", ""))).toBeNull();
  });

  it("accepts generic mobile MIME types and normalizes photo uploads", () => {
    const mobilePhoto = file("camera.jpg", "application/octet-stream");
    const mobileDocument = file("marksheet.pdf", "application/octet-stream");

    expect(validateAdmissionPhotoFile(mobilePhoto)).toBeNull();
    expect(validateAdmissionDocumentFile(mobileDocument)).toBeNull();
    expect(normalizeAdmissionPhotoUpload(mobilePhoto).type).toBe("image/jpeg");
  });

  it("gives a useful error for iPhone HEIC files", () => {
    expect(validateAdmissionPhotoFile(file("IMG_1001.HEIC", "image/heic"))).toContain("HEIC/HEIF");
    expect(validateAdmissionDocumentFile(file("IMG_1001.HEIF", "image/heif"))).toContain(
      "HEIC/HEIF",
    );
  });

  it("rejects files larger than two megabytes", () => {
    const oversized = file("camera.jpg", "image/jpeg", 2 * 1024 * 1024 + 1);
    expect(validateAdmissionPhotoFile(oversized)).toBe("Passport photo must be 2 MB or smaller");
  });
});
