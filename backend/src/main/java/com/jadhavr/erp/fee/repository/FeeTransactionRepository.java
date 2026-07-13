package com.jadhavr.erp.fee.repository;

import com.jadhavr.erp.fee.entity.FeeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeTransactionRepository extends JpaRepository<FeeTransaction, Long> {
    List<FeeTransaction> findByStudentFeeAccountIdOrderByCreatedAtDesc(Long id);

    boolean existsByFeePaymentId(Long paymentId);
}
