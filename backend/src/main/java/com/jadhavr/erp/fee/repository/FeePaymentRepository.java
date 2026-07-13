package com.jadhavr.erp.fee.repository;

import com.jadhavr.erp.fee.dto.FeeCollectionRow;
import com.jadhavr.erp.fee.entity.FeePayment;
import com.jadhavr.erp.fee.enums.PaymentStatus;
import com.jadhavr.erp.fee.enums.StudentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long>,
        JpaSpecificationExecutor<FeePayment> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FeePayment p where p.id = :id")
    java.util.Optional<FeePayment> findByIdForUpdate(@Param("id") Long id);

    List<FeePayment> findByStudentUserIdOrderByCreatedAtDesc(Long id);

    List<FeePayment> findByStudentFeeAccountIdOrderByCreatedAtDesc(Long id);

    boolean existsByCollegeIdAndTransactionReferenceIgnoreCase(Long id, String ref);

    long countByCollegeIdAndStatus(Long id, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.college.id=:c and p.status=:s")
    BigDecimal sumByCollegeAndStatus(Long c, PaymentStatus s);

    @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.status=:s")
    BigDecimal sumByStatus(PaymentStatus s);

    @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.college.id=:c and p.status='VERIFIED' and p.verifiedAt>=:start")
    BigDecimal sumToday(Long c, LocalDateTime start);

    @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.status='VERIFIED' and p.verifiedAt>=:start")
    BigDecimal sumTodayAll(LocalDateTime start);

    @Query(value = """
            select new com.jadhavr.erp.fee.dto.FeeCollectionRow(
                p.id,
                p.student.fullName,
                p.college.name,
                p.department.name,
                p.studentFeeAccount.studentCategory,
                p.amount,
                p.paymentDate,
                p.transactionReference)
            from FeePayment p
            where p.status = com.jadhavr.erp.fee.enums.PaymentStatus.VERIFIED
              and (:collegeId is null or p.college.id = :collegeId)
              and (:departmentId is null or p.department.id = :departmentId)
              and (:academicYear is null or p.studentFeeAccount.academicYear = :academicYear)
              and (:studentCategory is null or p.studentFeeAccount.studentCategory = :studentCategory)
            """, countQuery = """
            select count(p.id)
            from FeePayment p
            where p.status = com.jadhavr.erp.fee.enums.PaymentStatus.VERIFIED
              and (:collegeId is null or p.college.id = :collegeId)
              and (:departmentId is null or p.department.id = :departmentId)
              and (:academicYear is null or p.studentFeeAccount.academicYear = :academicYear)
              and (:studentCategory is null or p.studentFeeAccount.studentCategory = :studentCategory)
            """)
    Page<FeeCollectionRow> findVerifiedCollections(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("academicYear") String academicYear,
            @Param("studentCategory") StudentCategory studentCategory,
            Pageable pageable);
}
