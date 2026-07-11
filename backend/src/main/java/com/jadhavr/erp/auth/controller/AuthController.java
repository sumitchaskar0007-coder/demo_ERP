package com.jadhavr.erp.auth.controller;

import com.jadhavr.erp.auth.dto.AuthUserResponse;
import com.jadhavr.erp.auth.dto.LoginRequest;
import com.jadhavr.erp.auth.dto.LoginResponse;
import com.jadhavr.erp.auth.dto.ChangePasswordRequest;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.jadhavr.erp.email.service.EmailNotificationService;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.audit.enums.AuditModule;
import com.jadhavr.erp.audit.enums.AuditAction;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotifications;
    private AuditLogService auditLogs;

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void setAuditLogs(AuditLogService service) { this.auditLogs = service; }

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserRepository users, UserMapper mapper, PasswordEncoder passwordEncoder,
                          EmailNotificationService emailNotifications) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.emailNotifications = emailNotifications;
    }

    @PostMapping("/login")
    @Transactional
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        User user = users.findByEmail(email).orElseThrow();
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);
        if (auditLogs != null) auditLogs.logWithUser(user, AuditModule.AUTH, AuditAction.LOGIN, "User", user.getId(), "Successful login");
        return ApiResponse.success("Login successful", new LoginResponse(
                jwtService.generateToken(details), "Bearer",
                jwtService.getExpirationMs(), mapper.toAuthResponse(user)));
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ApiResponse<AuthUserResponse> profile(@AuthenticationPrincipal CustomUserDetails details) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        return ApiResponse.success("Profile retrieved successfully", mapper.toAuthResponse(user));
    }

    @PostMapping("/change-password")
    @Transactional
    public ApiResponse<AuthUserResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails details,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        User saved = users.save(user);
        emailNotifications.queuePasswordChangedEmail(saved);
        return ApiResponse.success("Password changed successfully", mapper.toAuthResponse(saved));
    }
}
