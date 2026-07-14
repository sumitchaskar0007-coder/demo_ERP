package com.jadhavr.erp.attendance.repository;
import com.jadhavr.erp.attendance.entity.AttendanceModels.StudentAttendance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*; import java.util.*;
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance,Long>{
 Optional<StudentAttendance> findByIdAndCollegeId(Long id,Long tenant);
 Optional<StudentAttendance> findByCollegeIdAndSessionIdAndStudentId(Long tenant,Long session,Long student);
 List<StudentAttendance> findByCollegeIdAndSessionId(Long tenant,Long session);
 @Query("select a from StudentAttendance a where a.college.id=:tenant and a.student.id=:student and a.session.sessionDate between :from and :to")
 List<StudentAttendance> report(@Param("tenant")Long tenant,@Param("student")Long student,@Param("from")LocalDate from,@Param("to")LocalDate to);
}
