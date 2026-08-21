package com.collegeerp.erp.fee.repository;

import com.collegeerp.erp.fee.entity.FeeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeTransactionRepository extends JpaRepository<FeeTransaction, Long> {
    List<FeeTransaction> findByStudentFeeAccountIdOrderByCreatedAtDesc(Long id);

    boolean existsByFeePaymentId(Long paymentId);
    java.util.Optional<FeeTransaction> findByFeePaymentId(Long paymentId);
    List<FeeTransaction> findByStudentFeeAccountCollegeId(Long collegeId);
}
