package com.collegeerp.erp.fee.entity;

import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.fee.enums.FeeTransactionType;
import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_transaction_payment", columnNames = "fee_payment_id"))
public class FeeTransaction extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fee_account_id", nullable = false) private StudentFeeAccount studentFeeAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fee_payment_id") private FeePayment feePayment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private FeeTransactionType transactionType;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    private BigDecimal previousPaidAmount;
    private BigDecimal newPaidAmount;
    private BigDecimal previousRemainingAmount;
    private BigDecimal newRemainingAmount;
    @Column(length = 500) private String remarks;
    @ManyToOne(fetch = FetchType.LAZY) private User performedBy;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public StudentFeeAccount getStudentFeeAccount() { return studentFeeAccount; }
    public void setStudentFeeAccount(StudentFeeAccount value) { studentFeeAccount = value; }
    public FeePayment getFeePayment() { return feePayment; }
    public void setFeePayment(FeePayment value) { feePayment = value; }
    public FeeTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(FeeTransactionType value) { transactionType = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public BigDecimal getPreviousPaidAmount() { return previousPaidAmount; }
    public void setPreviousPaidAmount(BigDecimal value) { previousPaidAmount = value; }
    public BigDecimal getNewPaidAmount() { return newPaidAmount; }
    public void setNewPaidAmount(BigDecimal value) { newPaidAmount = value; }
    public BigDecimal getPreviousRemainingAmount() { return previousRemainingAmount; }
    public void setPreviousRemainingAmount(BigDecimal value) { previousRemainingAmount = value; }
    public BigDecimal getNewRemainingAmount() { return newRemainingAmount; }
    public void setNewRemainingAmount(BigDecimal value) { newRemainingAmount = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value; }
    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User value) { performedBy = value; }
}
