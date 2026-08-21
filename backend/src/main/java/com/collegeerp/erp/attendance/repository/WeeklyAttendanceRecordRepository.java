package com.collegeerp.erp.attendance.repository;

import com.collegeerp.erp.attendance.entity.WeeklyAttendanceRecord;
import com.collegeerp.erp.attendance.entity.WeeklyAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WeeklyAttendanceRecordRepository extends JpaRepository<WeeklyAttendanceRecord, Long> {
    List<WeeklyAttendanceRecord> findBySessionIdIn(Collection<Long> sessionIds);
    List<WeeklyAttendanceRecord> findBySessionIdOrderByStudentFullNameAsc(Long sessionId);
    Optional<WeeklyAttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<WeeklyAttendanceRecord> findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(Long studentId);
    List<WeeklyAttendanceRecord> findBySessionSectionDepartmentIdAndSessionStatus(
            Long departmentId, WeeklyAttendanceSession.Status status);
    long countBySessionSectionDepartmentIdAndSessionStatus(
            Long departmentId, WeeklyAttendanceSession.Status status);
    long countBySessionSectionDepartmentIdAndSessionStatusAndStatusIn(
            Long departmentId, WeeklyAttendanceSession.Status sessionStatus,
            Collection<WeeklyAttendanceRecord.Status> statuses);
    long countBySessionIdAndStatus(Long sessionId, WeeklyAttendanceRecord.Status status);
    long countBySessionTeacherId(Long teacherId);
    long countBySessionTeacherIdAndStatus(Long teacherId, WeeklyAttendanceRecord.Status status);
}
