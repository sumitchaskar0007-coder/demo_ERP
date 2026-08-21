package com.collegeerp.erp.attendance.repository;
import com.collegeerp.erp.attendance.entity.AttendanceModels.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.*; import java.util.*;
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession,Long>{
 Optional<AttendanceSession> findByIdAndCollegeId(Long id,Long tenant);
 boolean existsByCollegeIdAndTimetableEntryIdAndSessionDate(Long tenant,Long entry,LocalDate date);
 List<AttendanceSession> findByCollegeIdAndSessionDateBetweenOrderBySessionDateAsc(Long tenant,LocalDate from,LocalDate to);
}
