package com.jadhavr.erp.attendance.repository;
import com.jadhavr.erp.attendance.entity.AttendanceModels.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>{}
