package com.jadhavr.erp.fee.repository;

import com.jadhavr.erp.fee.dto.CollegeAmountPoint;
import com.jadhavr.erp.fee.dto.PendingFeeRow;
import com.jadhavr.erp.fee.dto.PendingFeeSummary;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.fee.enums.StudentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentFeeAccountRepository extends JpaRepository<StudentFeeAccount, Long>,
        JpaSpecificationExecutor<StudentFeeAccount> {

    Optional<StudentFeeAccount> findTopByStudentUserIdOrderByCreatedAtDesc(Long id);

    Optional<StudentFeeAccount> findByAdmissionFormId(Long id);

    boolean existsByAdmissionFormId(Long id);

    boolean existsByFeeStructureId(Long id);

    long countByCollegeId(Long id);

    @Query("select coalesce(sum(a.paidAmount), 0) from StudentFeeAccount a")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(a.remainingAmount), 0) from StudentFeeAccount a")
    BigDecimal sumRemainingAmount();

    @Query("""
            select new com.jadhavr.erp.fee.dto.PendingFeeSummary(
                coalesce(sum(a.remainingAmount), 0), count(a.id))
            from StudentFeeAccount a
            where a.remainingAmount > 0
            """)
    PendingFeeSummary pendingFeeSummary();

    @Query("""
            select new com.jadhavr.erp.fee.dto.CollegeAmountPoint(
                c.name, coalesce(sum(a.paidAmount), 0))
            from College c
            left join StudentFeeAccount a on a.college = c
            group by c.id, c.name
            order by c.name
            """)
    List<CollegeAmountPoint> sumPaidByCollege();

    @Query(value = """
            select new com.jadhavr.erp.fee.dto.PendingFeeRow(
                a.id,
                a.student.fullName,
                a.student.admissionNumber,
                a.college.name,
                a.department.name,
                a.feeStructure.studentCategory,
                a.totalFee,
                a.paidAmount,
                a.remainingAmount)
            from StudentFeeAccount a
            where a.remainingAmount > 0
              and (:collegeId is null or a.college.id = :collegeId)
              and (:departmentId is null or a.department.id = :departmentId)
              and (:academicYear is null or a.academicYear = :academicYear)
              and (:studentCategory is null or a.feeStructure.studentCategory = :studentCategory)
            """, countQuery = """
            select count(a.id)
            from StudentFeeAccount a
            where a.remainingAmount > 0
              and (:collegeId is null or a.college.id = :collegeId)
              and (:departmentId is null or a.department.id = :departmentId)
              and (:academicYear is null or a.academicYear = :academicYear)
              and (:studentCategory is null or a.feeStructure.studentCategory = :studentCategory)
            """)
    Page<PendingFeeRow> findPendingFees(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("academicYear") String academicYear,
            @Param("studentCategory") StudentCategory studentCategory,
            Pageable pageable);
}
