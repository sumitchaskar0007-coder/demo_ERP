package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.fee.dto.*;
import com.collegeerp.erp.fee.enums.*;

import java.math.BigDecimal;
import java.util.List;

public interface FeeService {
    FeeStructureResponse createStructure(CreateFeeStructureRequest request);
    FeeStructureResponse updateStructure(Long id, UpdateFeeStructureRequest request);
    void deleteStructure(Long id);
    FeeStructureResponse setStructureStatus(Long id, FeeStructureStatus status);
    FeeStructureResponse getStructure(Long id);
    PageResponse<FeeStructureResponse> searchStructures(String keyword, Long collegeId, Long departmentId,
            String academicYear, FeeStructureStatus status, int page, int size, String sort, String direction);
    PageResponse<FeeStructureResponse> searchStructures(String keyword, Long collegeId, Long departmentId,
            String academicYear, StudentCategory category, FeeStructureStatus status,
            int page, int size, String sort, String direction);
    void createAccountForAdmission(AdmissionForm admission);
    int synchronizeUntouchedAdmissionFeeAccounts(Long departmentId, BigDecimal admissionFormFee);
    void createRegularFeeAccount(AdmissionForm admission);
    boolean isAdmissionFormFeeVerified(Long admissionId);
    StudentFeeAccountResponse myAccount();
    String myCollegeQrStorageName();
    PaymentResponse submitPayment(SubmitPaymentRequest request, String proofStorageName);
    String myPaymentProofStorageName(Long paymentId);
    List<PaymentResponse> myPayments();
    List<FeeTransactionResponse> myTransactions();
    FeeDashboardResponse dashboard();
    PageResponse<StudentFeeAccountResponse> searchAccounts(String keyword, Long collegeId, Long departmentId,
            FeeAccountStatus status, int page, int size);
    StudentFeeAccountResponse getAccount(Long id);
    AdmissionFeeSummaryResponse getAdmissionFeeSummary(Long admissionId);
    void sendPendingFeeReminder(Long id);
    int sendPendingFeeReminders();
    PageResponse<PaymentResponse> searchPayments(String keyword, Long collegeId, Long departmentId,
            PaymentStatus status, int page, int size);
    PaymentResponse getPayment(Long id);
    String paymentProofStorageName(Long paymentId);
    PaymentResponse verify(Long id, VerifyPaymentRequest request);
    PaymentResponse reject(Long id, RejectPaymentRequest request);
    List<FeeTransactionResponse> accountTransactions(Long id);
}
