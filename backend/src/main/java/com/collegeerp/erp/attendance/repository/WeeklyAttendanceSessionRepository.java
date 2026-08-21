package com.collegeerp.erp.attendance.repository;

import com.collegeerp.erp.attendance.entity.WeeklyAttendanceSession;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.*;

public interface WeeklyAttendanceSessionRepository extends JpaRepository<WeeklyAttendanceSession, Long> {
    Optional<WeeklyAttendanceSession> findByTimetableEntryIdAndAttendanceDate(Long entryId, LocalDate date);
    List<WeeklyAttendanceSession> findByTeacherIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(Long teacherId, LocalDate from, LocalDate to);
    List<WeeklyAttendanceSession> findTop20ByTeacherIdOrderByAttendanceDateDescStartTimeDesc(Long teacherId);
    long countByTeacherId(Long teacherId);
    long countByTeacherIdAndStatus(Long teacherId, WeeklyAttendanceSession.Status status);
    List<WeeklyAttendanceSession> findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(Long collegeId, LocalDate from, LocalDate to);
    /** Date-bounded variant used by super-admin reports; avoids loading the entire table. */
    List<WeeklyAttendanceSession> findByAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(LocalDate from, LocalDate to);
    long countByStatusAndAttendanceDate(
            WeeklyAttendanceSession.Status status, LocalDate attendanceDate);
    long countByCollegeIdAndStatusAndAttendanceDate(
            Long collegeId, WeeklyAttendanceSession.Status status, LocalDate attendanceDate);
    long countByCollegeIdAndSectionDepartmentIdAndStatusAndAttendanceDate(
            Long collegeId, Long departmentId, WeeklyAttendanceSession.Status status,
            LocalDate attendanceDate);
}
