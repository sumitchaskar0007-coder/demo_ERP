import { apiClient } from "@/lib/apiClient";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  CreateFeeStructureRequest,
  FeeAccountStatus,
  FeeDashboardResponse,
  FeeStructureResponse,
  FeeStructureStatus,
  FeeTransactionResponse,
  PaymentResponse,
  PaymentStatus,
  StudentFeeAccountResponse,
  SubmitPaymentRequest,
  UpdateFeeStructureRequest,
} from "./types";
const unwrap = <T>(r: { data: ApiResponse<T> }) => r.data.data;
export const createFeeStructure = (v: CreateFeeStructureRequest) =>
  apiClient
    .post<ApiResponse<FeeStructureResponse>>("/api/principal/fee-structures", v)
    .then(unwrap);
export const searchFeeStructures = (params: Record<string, unknown>) =>
  apiClient
    .get<
      ApiResponse<PageResponse<FeeStructureResponse>>
    >("/api/principal/fee-structures/search", { params })
    .then(unwrap);
export const getFeeStructureById = (id: number) =>
  apiClient
    .get<ApiResponse<FeeStructureResponse>>(`/api/principal/fee-structures/${id}`)
    .then(unwrap);
export const updateFeeStructure = (id: number, v: UpdateFeeStructureRequest) =>
  apiClient
    .put<ApiResponse<FeeStructureResponse>>(`/api/principal/fee-structures/${id}`, v)
    .then(unwrap);
export const deleteFeeStructure = (id: number) =>
  apiClient.delete(`/api/principal/fee-structures/${id}`);
export const setFeeStructureStatus = (id: number, s: FeeStructureStatus) =>
  apiClient
    .patch<
      ApiResponse<FeeStructureResponse>
    >(`/api/principal/fee-structures/${id}/${s === "ACTIVE" ? "activate" : "deactivate"}`)
    .then(unwrap);
export const getMyFeeAccount = () =>
  apiClient.get<ApiResponse<StudentFeeAccountResponse>>("/api/student/fees/me").then(unwrap);
export const submitPayment = (v: SubmitPaymentRequest, proof: File) => {
  const body = new FormData();
  body.append("request", new Blob([JSON.stringify(v)], { type: "application/json" }));
  body.append("proof", proof);
  return apiClient
    .post<ApiResponse<PaymentResponse>>("/api/student/fees/payments", body, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then(unwrap);
};
export const getMyPayments = () =>
  apiClient.get<ApiResponse<PaymentResponse[]>>("/api/student/fees/payments").then(unwrap);
export const getMyFeeTransactions = () =>
  apiClient
    .get<ApiResponse<FeeTransactionResponse[]>>("/api/student/fees/transactions")
    .then(unwrap);
export const getFeeDashboard = () =>
  apiClient.get<ApiResponse<FeeDashboardResponse>>("/api/fee-section/dashboard").then(unwrap);
export const searchFeeAccounts = (params: {
  keyword?: string;
  status?: FeeAccountStatus;
  page?: number;
  size?: number;
}) =>
  apiClient
    .get<
      ApiResponse<PageResponse<StudentFeeAccountResponse>>
    >("/api/fee-section/fee-accounts", { params })
    .then(unwrap);
export const getFeeAccountById = (id: number) =>
  apiClient
    .get<ApiResponse<StudentFeeAccountResponse>>(`/api/fee-section/fee-accounts/${id}`)
    .then(unwrap);
export const sendPendingFeeReminder = (id: number) =>
  apiClient.post(`/api/fee-section/fee-accounts/${id}/send-reminder`);
export const searchPayments = (params: {
  keyword?: string;
  status?: PaymentStatus;
  page?: number;
  size?: number;
}) =>
  apiClient
    .get<ApiResponse<PageResponse<PaymentResponse>>>("/api/fee-section/payments", { params })
    .then(unwrap);
export const getPaymentById = (id: number) =>
  apiClient.get<ApiResponse<PaymentResponse>>(`/api/fee-section/payments/${id}`).then(unwrap);
export const getPaymentProof = async (id: number) => {
  const response = await apiClient.get<Blob>(`/api/fee-section/payments/${id}/proof`, {
    responseType: "blob",
  });
  return response.data;
};
export const verifyPayment = (id: number, remarks = "") =>
  apiClient
    .patch<ApiResponse<PaymentResponse>>(`/api/fee-section/payments/${id}/verify`, { remarks })
    .then(unwrap);
export const rejectPayment = (id: number, rejectionReason: string) =>
  apiClient
    .patch<
      ApiResponse<PaymentResponse>
    >(`/api/fee-section/payments/${id}/reject`, { rejectionReason })
    .then(unwrap);
export const getFeeAccountTransactions = (id: number) =>
  apiClient
    .get<ApiResponse<FeeTransactionResponse[]>>(`/api/fee-section/fee-accounts/${id}/transactions`)
    .then(unwrap);
