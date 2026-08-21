package com.collegeerp.erp.fee.repository;

import com.collegeerp.erp.fee.dto.CollegeAmountPoint;
import com.collegeerp.erp.fee.dto.FeeBalanceTotals;
import com.collegeerp.erp.fee.dto.PendingFeeRow;
import com.collegeerp.erp.fee.dto.PendingFeeSummary;
import com.collegeerp.erp.fee.entity.StudentFeeAccount;
import com.collegeerp.erp.fee.enums.StudentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface StudentFeeAccountRepository extends JpaRepository<StudentFeeAccount, Long>,
        JpaSpecificationExecutor<StudentFeeAccount> {

    Optional<StudentFeeAccount> findTopByStudentUserIdOrderByCreatedAtDesc(Long id);

    Optional<StudentFeeAccount> findTopByStudentIdOrderByCreatedAtDesc(Long id);

    Optional<StudentFeeAccount> findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StudentFeeAccount a where a.id = :id")
    Optional<StudentFeeAccount> findByIdForUpdate(@Param("id") Long id);

    Optional<StudentFeeAccount> findFirstByAdmissionFormIdAndFeeStructureIsNullOrderByCreatedAtDesc(Long id);
    Optional<StudentFeeAccount> findFirstByAdmissionFormIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(Long id);
    List<StudentFeeAccount> findByAdmissionFormIdIn(Collection<Long> ids);

    boolean existsByAdmissionFormIdAndFeeStructureIsNull(Long id);
    boolean existsByAdmissionFormIdAndFeeStructureIsNotNull(Long id);

    boolean existsByFeeStructureId(Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StudentFeeAccount a where a.feeStructure.id = :feeStructureId order by a.id")
    List<StudentFeeAccount> findByFeeStructureIdForUpdate(
            @Param("feeStructureId") Long feeStructureId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from StudentFeeAccount a
            where a.department.id = :departmentId
              and a.feeStructure is null
              and a.paidAmount = 0
              and a.status = com.collegeerp.erp.fee.enums.FeeAccountStatus.PENDING
            order by a.id
            """)
    List<StudentFeeAccount> findUntouchedAdmissionFeeAccountsForUpdate(
            @Param("departmentId") Long departmentId);

    long countByCollegeId(Long id);
    List<StudentFeeAccount> findByCollegeId(Long id);
    List<StudentFeeAccount> findByCollegeIdAndRemainingAmountGreaterThan(Long id, BigDecimal amount);

    @Query("select coalesce(sum(a.paidAmount), 0) from StudentFeeAccount a")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(a.remainingAmount), 0) from StudentFeeAccount a")
    BigDecimal sumRemainingAmount();

    @Query("""
            select new com.collegeerp.erp.fee.dto.FeeBalanceTotals(
                coalesce(sum(a.paidAmount), 0),
                coalesce(sum(a.remainingAmount), 0))
            from StudentFeeAccount a
            """)
    FeeBalanceTotals balanceTotals();

    @Query("""
            select new com.collegeerp.erp.fee.dto.FeeBalanceTotals(
                coalesce(sum(a.paidAmount), 0),
                coalesce(sum(a.remainingAmount), 0))
            from StudentFeeAccount a
            where a.college.id = :collegeId
            """)
    FeeBalanceTotals balanceTotalsByCollegeId(@Param("collegeId") Long collegeId);

    @Query("""
            select new com.collegeerp.erp.fee.dto.PendingFeeSummary(
                coalesce(sum(a.remainingAmount), 0), count(a.id))
            from StudentFeeAccount a
            where a.remainingAmount > 0
            """)
    PendingFeeSummary pendingFeeSummary();

    @Query("""
            select new com.collegeerp.erp.fee.dto.CollegeAmountPoint(
                c.name, coalesce(sum(a.paidAmount), 0))
            from College c
            left join StudentFeeAccount a on a.college = c
            group by c.id, c.name
            order by c.name
            """)
    List<CollegeAmountPoint> sumPaidByCollege();

    @Query(value = """
            select new com.collegeerp.erp.fee.dto.PendingFeeRow(
                a.id,
                a.student.fullName,
                a.student.admissionNumber,
                a.college.name,
                a.department.name,
                courseYear.name,
                division.name,
                a.studentCategory,
                a.totalFee,
                a.paidAmount,
                a.remainingAmount)
            from StudentFeeAccount a
            left join StudentSectionEnrollment e on e.student = a.student and e.status = com.collegeerp.erp.academic.enums.AcademicStatus.ACTIVE and e.academicYear = a.academicYear
            left join e.academicClass courseYear
            left join e.section division
            where a.remainingAmount > 0
              and (:collegeId is null or a.college.id = :collegeId)
              and (:departmentId is null or a.department.id = :departmentId)
              and (:academicYear is null or a.academicYear = :academicYear)
              and (:studentCategory is null or a.studentCategory = :studentCategory)
              and lower(a.student.fullName) like concat('%', lower(:keyword), '%')
              and (:courseYearId is null or courseYear.id = :courseYearId)
              and (:divisionId is null or division.id = :divisionId)
            """, countQuery = """
            select count(a.id)
            from StudentFeeAccount a
            left join StudentSectionEnrollment e on e.student = a.student and e.status = com.collegeerp.erp.academic.enums.AcademicStatus.ACTIVE and e.academicYear = a.academicYear
            left join e.academicClass courseYear
            left join e.section division
            where a.remainingAmount > 0
              and (:collegeId is null or a.college.id = :collegeId)
              and (:departmentId is null or a.department.id = :departmentId)
              and (:academicYear is null or a.academicYear = :academicYear)
              and (:studentCategory is null or a.studentCategory = :studentCategory)
              and lower(a.student.fullName) like concat('%', lower(:keyword), '%')
              and (:courseYearId is null or courseYear.id = :courseYearId)
              and (:divisionId is null or division.id = :divisionId)
            """)
    Page<PendingFeeRow> findPendingFees(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("academicYear") String academicYear,
            @Param("studentCategory") StudentCategory studentCategory,
            @Param("keyword") String keyword,
            @Param("courseYearId") Long courseYearId,
            @Param("divisionId") Long divisionId,
            Pageable pageable);

    @Query("""
            select coalesce(sum(a.remainingAmount), 0)
            from StudentFeeAccount a
            left join StudentSectionEnrollment e on e.student = a.student and e.status = com.collegeerp.erp.academic.enums.AcademicStatus.ACTIVE and e.academicYear = a.academicYear
            left join e.academicClass courseYear
            left join e.section division
            where a.remainingAmount > 0
              and (:collegeId is null or a.college.id = :collegeId)
              and (:departmentId is null or a.department.id = :departmentId)
              and (:academicYear is null or a.academicYear = :academicYear)
              and (:studentCategory is null or a.studentCategory = :studentCategory)
              and lower(a.student.fullName) like concat('%', lower(:keyword), '%')
              and (:courseYearId is null or courseYear.id = :courseYearId)
              and (:divisionId is null or division.id = :divisionId)
            """)
    BigDecimal sumPendingFees(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("academicYear") String academicYear,
            @Param("studentCategory") StudentCategory studentCategory,
            @Param("keyword") String keyword,
            @Param("courseYearId") Long courseYearId,
            @Param("divisionId") Long divisionId);
}
