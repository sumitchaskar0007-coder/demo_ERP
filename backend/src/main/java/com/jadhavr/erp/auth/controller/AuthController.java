package com.jadhavr.erp.auth.controller;

import com.jadhavr.erp.auth.dto.AuthUserResponse;
import com.jadhavr.erp.auth.dto.LoginRequest;
import com.jadhavr.erp.auth.dto.LoginResponse;
import com.jadhavr.erp.auth.dto.UpdateOwnProfileRequest;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.auth.service.ProfileImageStorageService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository users;
    private final UserMapper mapper;
    private final ProfileImageStorageService profileImages;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserRepository users, UserMapper mapper,
                          ProfileImageStorageService profileImages) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.mapper = mapper;
        this.profileImages = profileImages;
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

    @PutMapping("/profile")
    @Transactional
    public ApiResponse<AuthUserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails details,
            @Valid @RequestBody UpdateOwnProfileRequest request) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        user.setPhone(trimToNull(request.phone()));
        user.setAddress(trimToNull(request.address()));
        user.setBio(trimToNull(request.bio()));
        return ApiResponse.success("Profile updated successfully", mapper.toAuthResponse(users.save(user)));
    }

    @PostMapping(value = "/profile/photo", consumes = "multipart/form-data")
    @Transactional
    public ApiResponse<AuthUserResponse> uploadProfilePhoto(
            @AuthenticationPrincipal CustomUserDetails details,
            @RequestPart("file") MultipartFile file) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        String oldImage = user.getProfileImageUrl();
        String newImage = profileImages.store(file);
        user.setProfileImageUrl(newImage);
        User saved = users.save(user);
        profileImages.deleteManagedFile(oldImage);
        return ApiResponse.success("Profile photo updated successfully", mapper.toAuthResponse(saved));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
