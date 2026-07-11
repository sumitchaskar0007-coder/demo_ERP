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
export const setFeeStructureStatus = (id: number, s: FeeStructureStatus) =>
  apiClient
    .patch<
      ApiResponse<FeeStructureResponse>
    >(`/api/principal/fee-structures/${id}/${s === "ACTIVE" ? "activate" : "deactivate"}`)
    .then(unwrap);
export const getMyFeeAccount = () =>
  apiClient.get<ApiResponse<StudentFeeAccountResponse>>("/api/student/fees/me").then(unwrap);
export const submitPayment = (v: SubmitPaymentRequest) =>
  apiClient.post<ApiResponse<PaymentResponse>>("/api/student/fees/payments", v).then(unwrap);
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
