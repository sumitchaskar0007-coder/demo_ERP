package com.jadhavr.erp.auth.repository;

import com.jadhavr.erp.auth.entity.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long> {}
