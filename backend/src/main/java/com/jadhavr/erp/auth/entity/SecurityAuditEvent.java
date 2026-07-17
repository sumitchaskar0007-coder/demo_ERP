package com.jadhavr.erp.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit_events", indexes = {
        @Index(name = "idx_security_audit_user_time", columnList = "user_id, created_at"),
        @Index(name = "idx_security_audit_type_time", columnList = "event_type, created_at")
})
public class SecurityAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "institution_id") private Long institutionId;
    @Column(name = "event_type", nullable = false, length = 60) private String eventType;
    @Column(nullable = false) private boolean success;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 300) private String userAgent;
    @Column(length = 300) private String details;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public void setUserId(Long value) { userId = value; } public void setInstitutionId(Long value) { institutionId = value; }
    public void setEventType(String value) { eventType = value; } public void setSuccess(boolean value) { success = value; }
    public void setIpAddress(String value) { ipAddress = value; } public void setUserAgent(String value) { userAgent = value; }
    public void setDetails(String value) { details = value; }
}
