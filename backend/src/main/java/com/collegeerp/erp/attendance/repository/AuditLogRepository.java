package com.collegeerp.erp.attendance.repository;
import com.collegeerp.erp.attendance.entity.AttendanceModels.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>{}
