package com.jadhavr.erp.attendance.repository;
import com.jadhavr.erp.attendance.entity.AttendanceModels.AttendanceCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection,Long>{}
