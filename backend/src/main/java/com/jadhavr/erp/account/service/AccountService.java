package com.jadhavr.erp.account.service;

import com.jadhavr.erp.account.dto.AccountDtos.*;
import com.jadhavr.erp.audit.enums.*;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.auth.security.AuthorizationSnapshotService;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditLogService audit;
    private AuthorizationSnapshotService authorizationSnapshots;
    private RefreshTokenRepository refreshTokens;

    public AccountService(UserRepository users, PasswordEncoder encoder, AuditLogService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Autowired(required = false)
    void setAuthorizationSnapshots(AuthorizationSnapshotService snapshots) {
        this.authorizationSnapshots = snapshots;
    }

    @Autowired(required = false)
    void setRefreshTokens(RefreshTokenRepository tokens) {
        this.refreshTokens = tokens;
    }

    @Transactional(readOnly = true)
    public Profile me() {
        return map(user());
    }

    @Transactional
    public Profile update(UpdateProfile request) {
        User user = user();
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        users.save(user);
        audit.log(AuditModule.ACCOUNT, AuditAction.UPDATE, "User", user.getId(), "Updated own account profile");
        return map(user);
    }

    @Transactional
    public void change(ChangePassword request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }
        User user = user();
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (encoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }
        user.setPasswordHash(encoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setSessionVersion(user.getSessionVersion() + 1);
        users.save(user);
        if (refreshTokens != null) refreshTokens.revokeAllForUser(user.getId());
        if (authorizationSnapshots != null) authorizationSnapshots.evict(user.getId());
        audit.log(AuditModule.ACCOUNT, AuditAction.CHANGE_PASSWORD, "User", user.getId(),
                "Changed account password and revoked active sessions");
    }

    private User user() {
        return users.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Profile map(User user) {
        var college = user.getCollege();
        return new Profile(user.getId(), college == null ? null : college.getId(),
                college == null ? null : college.getName(), college == null ? null : college.getCode(),
                user.getFullName(), user.getEmail(), user.getPhone(), user.getStatus(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet()),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
