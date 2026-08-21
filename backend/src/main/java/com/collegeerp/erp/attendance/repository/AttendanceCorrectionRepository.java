package com.collegeerp.erp.attendance.repository;
import com.collegeerp.erp.attendance.entity.AttendanceModels.AttendanceCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection,Long>{}
