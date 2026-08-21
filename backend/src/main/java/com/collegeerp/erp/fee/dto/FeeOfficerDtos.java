package com.collegeerp.erp.fee.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class FeeOfficerDtos {
    private FeeOfficerDtos(){}
    public record Summary(long totalFeeAccounts,long pendingVerifications,long verifiedPayments,long rejectedPayments,BigDecimal todayCollections,BigDecimal monthCollection,BigDecimal pendingAmount,long todayPendingRequests,long fullyPaid,long partiallyPaid,double averageVerificationMinutes){}
    public record MoneyPoint(String label,BigDecimal value){}
    public record FilterOption(Long id,String label){}
    public record AccountRow(Long id,Long studentId,String student,String prn,String department,String academicYear,String course,String division,String feeStructure,BigDecimal totalFee,BigDecimal paid,BigDecimal pending,String status,LocalDate dueDate,LocalDate lastPayment){}
    public record AccountPage(List<AccountRow> content,long totalElements,int page,int totalPages){}
    public record PaymentRow(Long id,Long accountId,Long studentId,String receiptNumber,String student,String prn,String department,String course,String division,BigDecimal amount,String paymentMode,String transactionId,LocalDate paymentDate,String proofUrl,String remarks,String status,LocalDateTime submittedAt,LocalDateTime verifiedAt,String verifiedBy,LocalDateTime rejectedAt,String rejectedBy,String rejectionReason,BigDecimal remainingFee,long previousPayments,int currentInstallment,boolean duplicateDetected,boolean amountMismatch){}
    public record DueRow(Long accountId,Long studentId,String student,String prn,String department,BigDecimal totalFee,BigDecimal paid,BigDecimal pending,LocalDate dueDate,LocalDate lastPayment,String urgency){}
    public record InstallmentRow(int number,BigDecimal amount,LocalDate dueDate,String status){}
    public record TimelineRow(String step,LocalDateTime occurredAt,String actor,String status){}
    public record AccountDetail(AccountRow account,List<PaymentRow> payments,List<FeeTransactionResponse> transactions,List<InstallmentRow> installments,List<String> receiptNumbers){}
    public record Workspace(Summary summary,AccountPage accounts,List<PaymentRow> pending,List<PaymentRow> verified,List<PaymentRow> rejected,List<PaymentRow> history,List<DueRow> dues,List<MoneyPoint> dailyCollection,List<MoneyPoint> monthlyCollection,List<MoneyPoint> departmentCollection,List<MoneyPoint> pendingVsCollected,List<PaymentRow> recentApprovals,List<PaymentRow> recentRejections,List<PaymentRow> largePayments,List<DueRow> studentsWithPendingDues,List<FilterOption> departments,List<FilterOption> courseYears,List<String> academicYears){}
}
