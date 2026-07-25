package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.AttendanceRecord;
import com.jadhavr.erp.reports.dto.AttendanceReportRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySessionId(Long id);
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AttendanceRecord> findByStudentId(Long id);

    @Query("""
            select s.fullName as studentName,
                   s.rollNumber as rollNumber,
                   count(r.id) as totalSessions,
                   coalesce(sum(case when r.status = com.jadhavr.erp.academic.enums.AttendanceStatus.PRESENT then 1 else 0 end), 0) as presentCount
            from StudentProfile s
            left join AttendanceRecord r on r.student = s
            where (:collegeId is null or s.college.id = :collegeId)
              and (:departmentId is null or s.department.id = :departmentId)
            group by s.id, s.fullName, s.rollNumber
            order by s.fullName
            """)
    List<AttendanceReportRow> attendanceReport(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            Pageable pageable);
}
