import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdmissionDocumentTransferOptions,
  AdmissionPrintResponse,
  AdmissionResponse,
  AdmissionStatus,
  AdmissionStatusHistoryResponse,
  PublicAdmissionInfoResponse,
  StudentSectionAdmissionResponse,
  StudentAdmissionAccessResponse,
  SubmitAdmissionRequest,
  SubmitAdmissionResponse,
} from "./types";
import {
  resolveAdmissionDocumentDownload,
  uploadAdmissionDocumentWithFallback,
} from "./documentTransfer";
import { normalizeAdmissionPhotoUpload } from "./fileValidation";

const ADMISSION_UPLOAD_TIMEOUT_MS = 120_000;

export async function getPublicAdmissionInfo(collegeCode: string) {
  const { data } = await apiClient.get<ApiResponse<PublicAdmissionInfoResponse>>(
    `/api/public/admissions/college/${collegeCode}/info`,
  );
  return data.data;
}
export async function getPublicAdmissionCategories(
  collegeCode: string,
  departmentId: number,
  filters?: { gender?: string; academicYear?: string; courseYear?: string },
) {
  const { data } = await apiClient.get<
    ApiResponse<
      Array<{
        category: import("./types").StudentCategory;
        customCategoryName?: string | null;
        label: string;
      }>
    >
  >(`/api/public/admissions/college/${collegeCode}/departments/${departmentId}/categories`, {
    params: filters,
  });
  return data.data;
}
export async function submitAdmission(collegeCode: string, values: SubmitAdmissionRequest) {
  const payload = {
    ...values,
    customCategoryName:
      values.studentCategory === "OTHER" && values.customCategoryName?.trim()
        ? values.customCategoryName.trim()
        : undefined,
    previousPercentage: values.previousPercentage === "" ? undefined : values.previousPercentage,
  };
  const { data } = await apiClient.post<ApiResponse<SubmitAdmissionResponse>>(
    `/api/public/admissions/college/${collegeCode}/submit`,
    payload,
  );
  return data.data;
}
export async function getMyAdmission() {
  const { data } = await apiClient.get<ApiResponse<StudentSectionAdmissionResponse>>(
    "/api/student/admissions/me",
  );
  return data.data;
}
export async function getStudentAdmissionAccess() {
  const { data } = await apiClient.get<ApiResponse<StudentAdmissionAccessResponse>>(
    "/api/student/admissions/access-state",
  );
  return data.data;
}
export async function getMyAdmissionCourseYears() {
  const { data } = await apiClient.get<ApiResponse<import("./types").AdmissionCourseYearOption[]>>(
    "/api/student/admissions/me/course-years",
  );
  return data.data;
}
export async function getMyAdmissionDocumentRequirements() {
  const { data } = await apiClient.get<
    ApiResponse<import("./types").AdmissionDocumentRequirement[]>
  >("/api/admission-document-requirements/me");
  return data.data;
}
export async function getAdmissionDocumentRequirements(id: number) {
  const { data } = await apiClient.get<
    ApiResponse<import("./types").AdmissionDocumentRequirement[]>
  >(`/api/admission-document-requirements/admission/${id}`);
  return data.data;
}
export async function getAdmissionDocumentSettings(departmentId: number) {
  const { data } = await apiClient.get<
    ApiResponse<import("./types").AdmissionDocumentRequirement[]>
  >("/api/college-settings/admission-documents", { params: { departmentId } });
  return data.data;
}
export async function createAdmissionDocumentSetting(
  values: {
    documentName: string;
    required: boolean;
  },
  departmentId: number,
) {
  const { data } = await apiClient.post<
    ApiResponse<import("./types").AdmissionDocumentRequirement>
  >("/api/college-settings/admission-documents", values, { params: { departmentId } });
  return data.data;
}
export async function updateAdmissionDocumentSetting(
  id: number,
  values: { documentName: string; required: boolean },
  departmentId: number,
) {
  const { data } = await apiClient.put<ApiResponse<import("./types").AdmissionDocumentRequirement>>(
    `/api/college-settings/admission-documents/${id}`,
    values,
    { params: { departmentId } },
  );
  return data.data;
}
export async function setAdmissionDocumentSettingActive(
  id: number,
  active: boolean,
  departmentId: number,
) {
  const { data } = await apiClient.patch<
    ApiResponse<import("./types").AdmissionDocumentRequirement>
  >(`/api/college-settings/admission-documents/${id}/active`, undefined, {
    params: { active, departmentId },
  });
  return data.data;
}
export async function getAdmissionCourseYears(id: number) {
  const { data } = await apiClient.get<ApiResponse<import("./types").AdmissionCourseYearOption[]>>(
    `/api/student-section/admissions/${id}/course-years`,
  );
  return data.data;
}
export async function submitMyAdmissionDetails(values: import("./types").DetailedAdmissionRequest) {
  const { data } = await apiClient.put<ApiResponse<StudentSectionAdmissionResponse>>(
    "/api/student/admissions/me/details",
    values,
  );
  return data.data;
}
export async function validateMyAdmissionDetails(
  values: import("./types").DetailedAdmissionRequest,
) {
  const { data } = await apiClient.post<ApiResponse<{ valid: boolean }>>(
    "/api/student/admissions/me/details/validate",
    values,
  );
  return data.data;
}
export async function getMyAdmissionDetailDraft() {
  const { data } = await apiClient.get<
    ApiResponse<{
      values: Partial<import("./types").DetailedAdmissionRequest> | null;
      version: number;
      updatedAt?: string | null;
    }>
  >("/api/student/admissions/me/details/draft");
  return data.data;
}
export async function saveMyAdmissionDetailDraft(
  values: import("./types").DetailedAdmissionRequest,
  version: number,
) {
  const { data } = await apiClient.put<
    ApiResponse<{
      values: Partial<import("./types").DetailedAdmissionRequest>;
      version: number;
      updatedAt: string;
    }>
  >("/api/student/admissions/me/details/draft", { values, version });
  return data.data;
}
export async function uploadMyAdmissionPhoto(file: File, signal?: AbortSignal) {
  const body = new FormData();
  body.append("file", normalizeAdmissionPhotoUpload(file));
  const { data } = await apiClient.post<ApiResponse<StudentSectionAdmissionResponse>>(
    "/api/student/admissions/me/photo",
    body,
    {
      headers: { "Content-Type": "multipart/form-data" },
      timeout: ADMISSION_UPLOAD_TIMEOUT_MS,
      signal,
    },
  );
  return data.data;
}
export async function getMyAdmissionPhoto() {
  const response = await apiClient.get<Blob>("/api/student/admissions/me/photo", {
    responseType: "blob",
  });
  return URL.createObjectURL(response.data);
}
export async function uploadMyAdmissionDocument(
  type: import("./types").AdmissionDocumentType,
  file: File,
  options: AdmissionDocumentTransferOptions = {},
) {
  const endpoint = `/api/student/admissions/me/documents/${type}`;
  return uploadAdmissionDocumentWithFallback(
    endpoint,
    file,
    { upload: (signal) => uploadAdmissionDocumentMultipart(endpoint, file, signal) },
    options,
  );
}
export async function getMyAdmissionDocument(
  type: import("./types").AdmissionDocumentType,
  options: Pick<AdmissionDocumentTransferOptions, "signal" | "directTransferEnabled"> = {},
) {
  const endpoint = `/api/student/admissions/me/documents/${type}`;
  const result = await resolveAdmissionDocumentDownload(
    endpoint,
    {
      download: async (signal) => {
        const response = await getAdmissionDocumentBlob(endpoint, signal);
        return URL.createObjectURL(response.data);
      },
    },
    options,
  );
  return result.direct ? result.value.downloadUrl : result.value;
}
export async function getStudentProfile() {
  const { data } =
    await apiClient.get<ApiResponse<import("@/features/student/types").StudentProfileResponse>>(
      "/api/student/profile/me",
    );
  return data.data;
}
export async function searchStudentSectionAdmissions(params: {
  keyword?: string;
  departmentId?: number;
  status?: AdmissionStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudentSectionAdmissionResponse>>>(
    "/api/student-section/admissions",
    { params },
  );
  return data.data;
}
export async function getStudentSectionAdmission(id: number) {
  const { data } = await apiClient.get<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}`,
  );
  return data.data;
}
export async function startAdmissionReview(id: number) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/start-review`,
  );
  return data.data;
}
export async function approveAdmission(
  id: number,
  values: {
    studentCategory: import("./types").StudentCategory;
    customCategoryName?: string;
    photoVerified: boolean;
    tenthMarksheetVerified: boolean;
    twelfthMarksheetVerified: boolean;
    leavingCertificateVerified: boolean;
    aadhaarCardVerified: boolean;
    graduationPgCertificateVerified: boolean;
    migrationCertificateVerified: boolean;
    gapAffidavitVerified: boolean;
    casteCertificateVerified: boolean;
    incomeProofVerified: boolean;
    nameChangeCertificateVerified: boolean;
    documentCustody: Array<{
      documentType: string;
      originalReceived: boolean;
      xeroxReceived: boolean;
    }>;
    remarks?: string;
  },
) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/approve`,
    values,
  );
  return data.data;
}
export async function rejectAdmission(id: number, values: { rejectionReason: string }) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/reject`,
    values,
  );
  return data.data;
}
export async function getAdmissionHistory(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionStatusHistoryResponse[]>>(
    `/api/student-section/admissions/${id}/history`,
  );
  return data.data;
}
export async function getAdmissionPrintData(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionPrintResponse>>(
    `/api/student-section/admissions/${id}/print-data`,
  );
  return data.data;
}
export async function markAdmissionPrinted(id: number, values: { remarks?: string }) {
  const { data } = await apiClient.patch<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/mark-printed`,
    values,
  );
  return data.data;
}
export async function getPrincipalReviewReadyAdmissions(params: {
  keyword?: string;
  departmentId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudentSectionAdmissionResponse>>>(
    "/api/principal/admissions/review-ready",
    { params },
  );
  return data.data;
}
export async function getPrincipalAdmission(id: number) {
  const { data } = await apiClient.get<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/principal/admissions/${id}`,
  );
  return data.data;
}
export async function getPrincipalAdmissionHistory(id: number) {
  const { data } = await apiClient.get<ApiResponse<AdmissionStatusHistoryResponse[]>>(
    `/api/principal/admissions/${id}/history`,
  );
  return data.data;
}
export async function updateAdmissionDetails(
  id: number,
  values: import("./types").DetailedAdmissionRequest,
) {
  const { data } = await apiClient.put<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/details`,
    values,
  );
  return data.data;
}

export async function uploadAdmissionPhoto(id: number, file: File) {
  const body = new FormData();
  body.append("file", normalizeAdmissionPhotoUpload(file));
  const { data } = await apiClient.post<ApiResponse<StudentSectionAdmissionResponse>>(
    `/api/student-section/admissions/${id}/photo`,
    body,
    { headers: { "Content-Type": "multipart/form-data" }, timeout: ADMISSION_UPLOAD_TIMEOUT_MS },
  );
  return data.data;
}
export async function getPrincipalAdmissionFees(id: number) {
  const { data } = await apiClient.get<
    ApiResponse<import("@/features/fees/types").AdmissionFeeSummaryResponse>
  >(`/api/principal/admissions/${id}/fees`);
  return data.data;
}
export async function getMyAdmissionPrintData() {
  const { data } = await apiClient.get<ApiResponse<AdmissionPrintResponse>>(
    "/api/student/admissions/me/print-data",
  );
  return data.data;
}
export async function getDocumentCustody(id: number) {
  const { data } = await apiClient.get<ApiResponse<import("./types").AdmissionDocumentCustody[]>>(
    `/api/student-section/admissions/${id}/document-custody`,
  );
  return data.data;
}
export async function markDocumentReturned(
  id: number,
  documentType: string,
  returnedToStudent: boolean,
  remarks = "",
) {
  const { data } = await apiClient.patch<ApiResponse<import("./types").AdmissionDocumentCustody>>(
    `/api/student-section/admissions/${id}/document-custody/${documentType}/returned`,
    { returnedToStudent, remarks },
  );
  return data.data;
}

export async function getStudentSectionAdmissionFees(id: number) {
  const { data } = await apiClient.get<
    ApiResponse<import("@/features/fees/types").AdmissionFeeSummaryResponse>
  >(`/api/student-section/admissions/${id}/fees`);
  return data.data;
}

export async function uploadAdmissionDocument(
  id: number,
  type: import("./types").AdmissionDocumentType,
  file: File,
  options: AdmissionDocumentTransferOptions = {},
) {
  const endpoint = `/api/student-section/admissions/${id}/documents/${type}`;
  return uploadAdmissionDocumentWithFallback(
    endpoint,
    file,
    { upload: (signal) => uploadAdmissionDocumentMultipart(endpoint, file, signal) },
    options,
  );
}

export async function getAdmissionDocument(
  id: number,
  type: import("./types").AdmissionDocumentType,
  options: Pick<AdmissionDocumentTransferOptions, "signal" | "directTransferEnabled"> & {
    principal?: boolean;
  } = {},
) {
  const prefix = options.principal
    ? "/api/principal/admissions"
    : "/api/student-section/admissions";
  const endpoint = `${prefix}/${id}/documents/${type}`;
  const result = await resolveAdmissionDocumentDownload(
    endpoint,
    {
      download: async (signal) => {
        const response = await getAdmissionDocumentBlob(endpoint, signal);
        return URL.createObjectURL(response.data);
      },
    },
    options,
  );
  return result.direct ? result.value.downloadUrl : result.value;
}

export async function downloadAdmissionDocument(
  id: number,
  type: import("./types").AdmissionDocumentType,
  options: Pick<AdmissionDocumentTransferOptions, "signal" | "directTransferEnabled"> & {
    principal?: boolean;
  } = {},
) {
  const prefix = options.principal
    ? "/api/principal/admissions"
    : "/api/student-section/admissions";
  const endpoint = `${prefix}/${id}/documents/${type}`;
  const result = await resolveAdmissionDocumentDownload(
    endpoint,
    {
      download: async (signal) => {
        const response = await getAdmissionDocumentBlob(endpoint, signal);
        return {
          blob: response.data,
          contentDisposition: String(response.headers["content-disposition"] || ""),
        };
      },
    },
    options,
  );

  let blob: Blob;
  let originalFilename: string | undefined;
  if (result.direct) {
    const response = await fetch(result.value.downloadUrl, {
      method: "GET",
      signal: options.signal,
      credentials: "omit",
      referrerPolicy: "no-referrer",
    });
    if (!response.ok) {
      throw new Error(`Document storage download failed with status ${response.status}`);
    }
    blob = await response.blob();
    if (blob.size !== result.value.fileSize) {
      throw new Error("The downloaded document size did not match the verified document");
    }
    originalFilename = result.value.originalFilename;
  } else {
    blob = result.value.blob;
    originalFilename = filenameFromContentDisposition(result.value.contentDisposition);
  }

  const extension =
    blob.type === "application/pdf" ? ".pdf" : blob.type === "image/png" ? ".png" : ".jpg";

  const originalName = originalFilename ?? `${type.toLowerCase().replaceAll("_", "-")}${extension}`;
  const baseName = originalName.replace(/\.[^.]+$/, "");
  const hasMatchingExtension =
    (extension === ".pdf" && /\.pdf$/i.test(originalName)) ||
    (extension === ".png" && /\.png$/i.test(originalName)) ||
    (extension === ".jpg" && /\.jpe?g$/i.test(originalName));
  const filename = hasMatchingExtension ? originalName : `${baseName}${extension}`;
  return { blob, filename, format: extension.slice(1).toUpperCase() };
}

async function uploadAdmissionDocumentMultipart(
  endpoint: string,
  file: File,
  signal?: AbortSignal,
) {
  const body = new FormData();
  body.append("file", file);
  const { data } = await apiClient.post<ApiResponse<StudentSectionAdmissionResponse>>(
    endpoint,
    body,
    {
      headers: { "Content-Type": "multipart/form-data" },
      signal,
      timeout: ADMISSION_UPLOAD_TIMEOUT_MS,
    },
  );
  return data.data;
}

function getAdmissionDocumentBlob(endpoint: string, signal?: AbortSignal) {
  return apiClient.get<Blob>(endpoint, { responseType: "blob", signal });
}

function filenameFromContentDisposition(disposition: string) {
  const matchedName = disposition.match(/filename\*?=(?:UTF-8''|")?([^";]+)/i)?.[1];
  if (!matchedName) return undefined;
  try {
    return decodeURIComponent(matchedName.replace(/^"|"$/g, ""));
  } catch {
    return matchedName.replace(/^"|"$/g, "");
  }
}

export async function getAdmissionPhoto(id: number, principal = false) {
  const prefix = principal ? "/api/principal/admissions" : "/api/student-section/admissions";
  const response = await apiClient.get<Blob>(`${prefix}/${id}/photo`, {
    responseType: "blob",
  });
  return URL.createObjectURL(response.data);
}

export async function principalApproveAdmission(id: number, remarks?: string) {
  const { data } = await apiClient.patch<ApiResponse<AdmissionResponse>>(
    `/api/principal/final-admissions/${id}/approve`,
    { remarks },
  );
  return data.data;
}

export async function principalRejectAdmission(id: number, rejectionReason: string) {
  const { data } = await apiClient.patch<ApiResponse<AdmissionResponse>>(
    `/api/principal/final-admissions/${id}/reject`,
    { rejectionReason },
  );
  return data.data;
}
