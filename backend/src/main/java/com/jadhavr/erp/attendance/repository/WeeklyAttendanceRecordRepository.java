package com.jadhavr.erp.attendance.repository;

import com.jadhavr.erp.attendance.entity.WeeklyAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WeeklyAttendanceRecordRepository extends JpaRepository<WeeklyAttendanceRecord, Long> {
    List<WeeklyAttendanceRecord> findBySessionIdOrderByStudentFullNameAsc(Long sessionId);
    Optional<WeeklyAttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<WeeklyAttendanceRecord> findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(Long studentId);
    long countBySessionIdAndStatus(Long sessionId, WeeklyAttendanceRecord.Status status);
}
