const MAX_ADMISSION_FILE_BYTES = 2 * 1024 * 1024;

const JPEG_TYPES = new Set(["image/jpeg", "image/jpg"]);
const PNG_TYPES = new Set(["image/png"]);
const PDF_TYPES = new Set(["application/pdf"]);
const GENERIC_FILE_TYPES = new Set(["", "application/octet-stream"]);

function extension(file: File) {
  return file.name.toLowerCase().match(/\.[^.]+$/)?.[0] ?? "";
}

function normalizedType(file: File) {
  return file.type.trim().toLowerCase();
}

function sizeError(file: File, label: string) {
  if (file.size <= 0) return `${label} must not be empty`;
  if (file.size > MAX_ADMISSION_FILE_BYTES) return `${label} must be 2 MB or smaller`;
  return null;
}

export function validateAdmissionPhotoFile(file: File) {
  const invalidSize = sizeError(file, "Passport photo");
  if (invalidSize) return invalidSize;

  const type = normalizedType(file);
  const suffix = extension(file);
  const validType = GENERIC_FILE_TYPES.has(type) || JPEG_TYPES.has(type) || PNG_TYPES.has(type);
  const validExtension = [".jpg", ".jpeg", ".png"].includes(suffix);
  if ((type && !validType) || !validExtension) {
    if (type.includes("heic") || type.includes("heif") || [".heic", ".heif"].includes(suffix)) {
      return "HEIC/HEIF phone photos are not supported. Convert or share the photo as JPEG or PNG, then upload it again";
    }
    return "Passport photo must be a JPEG or PNG file";
  }
  return null;
}

export function validateAdmissionDocumentFile(file: File) {
  const invalidSize = sizeError(file, "Document");
  if (invalidSize) return invalidSize;

  const type = normalizedType(file);
  const suffix = extension(file);
  const validType =
    GENERIC_FILE_TYPES.has(type) ||
    JPEG_TYPES.has(type) ||
    PNG_TYPES.has(type) ||
    PDF_TYPES.has(type);
  const validExtension = [".pdf", ".jpg", ".jpeg", ".png"].includes(suffix);
  if ((type && !validType) || !validExtension) {
    if (type.includes("heic") || type.includes("heif") || [".heic", ".heif"].includes(suffix)) {
      return "HEIC/HEIF phone files are not supported. Convert the file to JPEG, PNG, or PDF, then upload it again";
    }
    return "Document must be a PDF, JPEG, or PNG file";
  }
  return null;
}

export function normalizeAdmissionPhotoUpload(file: File) {
  const suffix = extension(file);
  const contentType = [".jpg", ".jpeg"].includes(suffix)
    ? "image/jpeg"
    : suffix === ".png"
      ? "image/png"
      : normalizedType(file);
  if (!contentType || contentType === file.type) return file;
  return new File([file], file.name, {
    type: contentType,
    lastModified: file.lastModified,
  });
}
