package com.collegeerp.erp.reports.repository;

import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.reports.dto.AdmissionCsvRow;
import com.collegeerp.erp.reports.dto.FeeCsvRow;
import com.collegeerp.erp.reports.dto.StudentCsvRow;
import com.collegeerp.erp.student.enums.StudentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReportExportReadRepository {
    private final EntityManager entityManager;

    public ReportExportReadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<AdmissionCsvRow> admissions(
            Long collegeId,
            Long departmentId,
            AdmissionStatus status,
            long afterId,
            int limit) {
        String jpql = """
                select new com.collegeerp.erp.reports.dto.AdmissionCsvRow(
                    admission.id,
                    admission.admissionReferenceNumber,
                    admission.student.admissionNumber,
                    admission.fullName,
                    admission.college.name,
                    admission.department.name,
                    admission.status)
                from AdmissionForm admission
                where admission.college.id = :collegeId
                  and admission.id > :afterId
                """
                + (departmentId == null ? "" : " and admission.department.id = :departmentId")
                + (status == null ? "" : " and admission.status = :status")
                + " order by admission.id";
        TypedQuery<AdmissionCsvRow> query =
                entityManager.createQuery(jpql, AdmissionCsvRow.class);
        query.setParameter("collegeId", collegeId);
        query.setParameter("afterId", afterId);
        if (departmentId != null) query.setParameter("departmentId", departmentId);
        if (status != null) query.setParameter("status", status);
        return query.setMaxResults(limit).getResultList();
    }

    public List<FeeCsvRow> fees(
            Long collegeId, Long departmentId, long afterId, int limit) {
        String jpql = """
                select new com.collegeerp.erp.reports.dto.FeeCsvRow(
                    account.id,
                    account.student.fullName,
                    account.student.admissionNumber,
                    account.totalFee,
                    account.paidAmount,
                    account.remainingAmount,
                    account.status)
                from StudentFeeAccount account
                where account.college.id = :collegeId
                  and account.id > :afterId
                """
                + (departmentId == null ? "" : " and account.department.id = :departmentId")
                + " order by account.id";
        TypedQuery<FeeCsvRow> query = entityManager.createQuery(jpql, FeeCsvRow.class);
        query.setParameter("collegeId", collegeId);
        query.setParameter("afterId", afterId);
        if (departmentId != null) query.setParameter("departmentId", departmentId);
        return query.setMaxResults(limit).getResultList();
    }

    public List<StudentCsvRow> students(
            Long collegeId,
            Long departmentId,
            StudentStatus status,
            long afterId,
            int limit) {
        String jpql = """
                select new com.collegeerp.erp.reports.dto.StudentCsvRow(
                    student.id,
                    student.fullName,
                    student.rollNumber,
                    student.email,
                    student.phone,
                    student.college.name,
                    student.department.name,
                    student.status)
                from StudentProfile student
                where student.college.id = :collegeId
                  and student.id > :afterId
                """
                + (departmentId == null ? "" : " and student.department.id = :departmentId")
                + (status == null ? "" : " and student.status = :status")
                + " order by student.id";
        TypedQuery<StudentCsvRow> query =
                entityManager.createQuery(jpql, StudentCsvRow.class);
        query.setParameter("collegeId", collegeId);
        query.setParameter("afterId", afterId);
        if (departmentId != null) query.setParameter("departmentId", departmentId);
        if (status != null) query.setParameter("status", status);
        return query.setMaxResults(limit).getResultList();
    }
}
