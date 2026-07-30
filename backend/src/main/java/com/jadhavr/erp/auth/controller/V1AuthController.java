package com.jadhavr.erp.auth.controller;

import com.jadhavr.erp.auth.dto.AuthUserResponse;
import com.jadhavr.erp.auth.dto.LoginRequest;
import com.jadhavr.erp.auth.security.AuthCookieService;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.TrustedClientIpResolver;
import com.jadhavr.erp.auth.service.AuthenticationService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/v1/auth")
public class V1AuthController {
    private final AuthenticationService authentication;
    private final AuthCookieService cookies;
    private final UserRepository users;
    private final UserMapper mapper;
    private final TrustedClientIpResolver clientIps;
    public V1AuthController(AuthenticationService authentication, AuthCookieService cookies,
            UserRepository users, UserMapper mapper, TrustedClientIpResolver clientIps) {
        this.authentication = authentication;
        this.cookies = cookies;
        this.users = users;
        this.mapper = mapper;
        this.clientIps = clientIps;
    }
    @PostMapping("/login")
    public ApiResponse<AuthUserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        var pair = authentication.login(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent")); cookies.setTokens(response, pair.accessToken(), pair.refreshToken());
        return ApiResponse.success("Login successful", pair.user());
    }
    @PostMapping("/refresh")
    public ApiResponse<AuthUserResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        var pair = authentication.rotate(cookie(request, AuthCookieService.REFRESH_COOKIE), clientIp(request), request.getHeader("User-Agent"));
        cookies.setTokens(response, pair.accessToken(), pair.refreshToken());
        return ApiResponse.success("Session refreshed successfully", pair.user());
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails current, HttpServletRequest request, HttpServletResponse response) {
        authentication.logout(current == null ? null : current.getId(),
                cookie(request, AuthCookieService.REFRESH_COOKIE),
                clientIp(request), request.getHeader("User-Agent"));
        cookies.clear(response);
        return ApiResponse.success("Logout successful", null);
    }
    @GetMapping("/me") @Transactional(readOnly = true)
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal CustomUserDetails current) {
        var user = users.findById(current.getId()).orElseThrow();
        return ApiResponse.success("Profile retrieved successfully", mapper.toAuthResponse(user));
    }
    @GetMapping("/csrf") public ApiResponse<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ApiResponse.success("CSRF cookie initialized", null);
    }
    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private String clientIp(HttpServletRequest request) {
        return clientIps.resolve(request);
    }
}
