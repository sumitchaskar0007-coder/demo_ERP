package com.collegeerp.erp.auth.service;

import com.collegeerp.erp.auth.entity.SecurityAuditEvent;
import com.collegeerp.erp.auth.repository.RefreshTokenRepository;
import com.collegeerp.erp.auth.repository.SecurityAuditEventRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SecurityEventService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final SecurityAuditEventRepository events;
    private final AuthorizationSnapshotService authorizationSnapshots;
    public SecurityEventService(UserRepository users, RefreshTokenRepository refreshTokens,
            SecurityAuditEventRepository events,
            AuthorizationSnapshotService authorizationSnapshots) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.events = events;
        this.authorizationSnapshots = authorizationSnapshots;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFailure(String email, String ip, String agent) {
        var user = users.findByEmail(email).orElse(null);
        if (user != null) {
            int failures = Math.min(Integer.MAX_VALUE, user.getFailedLoginAttempts() + 1);
            user.setFailedLoginAttempts(failures);
            // Authentication throttling is temporary and handled by the distributed
            // exponential backoff; accounts are never hard-locked due to failures.
            user.setLockedUntil(null);
            users.save(user);
        }
        record(user == null ? null : user.getId(), user == null || user.getCollege() == null ? null : user.getCollege().getId(),
                "LOGIN_FAILURE", false, ip, agent, user == null ? "Unknown account" : "Invalid credentials");
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshReuse(Long userId, String ip, String agent) {
        var user = users.findById(userId).orElse(null);
        if (user != null) {
            refreshTokens.revokeAllForUser(userId);
            user.setSessionVersion(user.getSessionVersion() + 1);
            users.save(user);
            authorizationSnapshots.invalidateOrThrow(userId);
        }
        record(userId, user == null || user.getCollege() == null ? null : user.getCollege().getId(),
                "REFRESH_TOKEN_REUSE", false, ip, agent, "All sessions revoked");
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(Long userId, Long institutionId, String type, boolean success, String ip, String agent, String details) {
        record(userId, institutionId, type, success, ip, agent, details);
    }
    private void record(Long userId, Long institutionId, String type, boolean success, String ip, String agent, String details) {
        SecurityAuditEvent event = new SecurityAuditEvent(); event.setUserId(userId); event.setInstitutionId(institutionId);
        event.setEventType(type); event.setSuccess(success); event.setIpAddress(trim(ip, 64));
        event.setUserAgent(trim(agent, 300)); event.setDetails(trim(details, 300)); events.save(event);
    }
    private String trim(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
}
