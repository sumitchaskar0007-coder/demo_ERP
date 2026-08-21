package com.collegeerp.erp.auth.controller;

import com.collegeerp.erp.auth.dto.AuthUserResponse;
import com.collegeerp.erp.auth.dto.UpdateOwnProfileRequest;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.auth.service.ProfileImageStorageService;
import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.mapper.UserMapper;
import com.collegeerp.erp.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final UserMapper mapper;
    private final ProfileImageStorageService profileImages;

    public AuthController(UserRepository users, UserMapper mapper,
                          ProfileImageStorageService profileImages) {
        this.users = users;
        this.mapper = mapper;
        this.profileImages = profileImages;
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
        String newImage = profileImages.store(file, user.getId(),
                user.getCollege() == null ? null : user.getCollege().getId());
        try {
            user.setProfileImageUrl(newImage);
            User saved = users.saveAndFlush(user);
            profileImages.cleanupAfterTransaction(newImage, oldImage);
            return ApiResponse.success("Profile photo updated successfully", mapper.toAuthResponse(saved));
        } catch (RuntimeException exception) {
            profileImages.deleteNewObjectAfterFailure(newImage);
            throw exception;
        }
    }

    @GetMapping("/profile/photo")
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> profilePhoto(
            @AuthenticationPrincipal CustomUserDetails details) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        if (user.getProfileImageUrl() == null) return ResponseEntity.notFound().build();
        var image = profileImages.load(user.getProfileImageUrl());
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .header("Content-Disposition", "inline; filename=\"profile-image\"")
                .body(image.resource());
    }

    @DeleteMapping("/profile/photo")
    @Transactional
    public ApiResponse<Void> deleteProfilePhoto(
            @AuthenticationPrincipal CustomUserDetails details) {
        User user = users.findByEmail(details.getUsername()).orElseThrow();
        String oldImage = user.getProfileImageUrl();
        if (oldImage != null) {
            user.setProfileImageUrl(null);
            users.saveAndFlush(user);
            profileImages.deleteAfterCommit(oldImage);
        }
        return ApiResponse.success("Profile photo removed successfully", null);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
