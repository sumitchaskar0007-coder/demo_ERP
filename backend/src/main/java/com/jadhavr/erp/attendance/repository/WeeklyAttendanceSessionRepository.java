package com.jadhavr.erp.attendance.repository;

import com.jadhavr.erp.attendance.entity.WeeklyAttendanceSession;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.*;

public interface WeeklyAttendanceSessionRepository extends JpaRepository<WeeklyAttendanceSession, Long> {
    Optional<WeeklyAttendanceSession> findByTimetableEntryIdAndAttendanceDate(Long entryId, LocalDate date);
    List<WeeklyAttendanceSession> findByTeacherIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(Long teacherId, LocalDate from, LocalDate to);
    List<WeeklyAttendanceSession> findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(Long collegeId, LocalDate from, LocalDate to);
    /** Date-bounded variant used by super-admin reports; avoids loading the entire table. */
    List<WeeklyAttendanceSession> findByAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(LocalDate from, LocalDate to);
}
