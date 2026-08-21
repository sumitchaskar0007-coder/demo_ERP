package com.collegeerp.erp.college.service;

import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class CollegePaymentQrService {
    private final CollegeRepository colleges;
    private final CollegeImageStorageService images;

    public CollegePaymentQrService(CollegeRepository colleges, CollegeImageStorageService images) {
        this.colleges = colleges;
        this.images = images;
    }

    public PaymentQrSettings settings(Long requestedCollegeId) {
        College college = college(requestedCollegeId);
        return response(college);
    }

    public CollegeImageStorageService.ImageResource image(Long requestedCollegeId) {
        return images.load(college(requestedCollegeId).getQrCodeUrl());
    }

    @Transactional
    public PaymentQrSettings update(Long requestedCollegeId, String accountName, MultipartFile file) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only Super Admin can configure payment QR settings");
        }
        String normalizedAccountName = normalizeAccountName(accountName);
        College college = college(requestedCollegeId);
        String pendingReference = images.storePending(file, "qr-code");
        college.setQrCodeUrl(images.claim(
                pendingReference, college.getId(), "qr-code", college.getQrCodeUrl()));
        college.setPaymentQrAccountName(normalizedAccountName);
        return response(colleges.saveAndFlush(college));
    }

    private String normalizeAccountName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            throw new BadRequestException("QR account name is required");
        }
        String normalized = accountName.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 150) {
            throw new BadRequestException("QR account name must not exceed 150 characters");
        }
        return normalized;
    }

    private College college(Long requestedCollegeId) {
        Long collegeId;
        if (SecurityUtils.isSuperAdmin()) {
            if (requestedCollegeId == null) {
                throw new BadRequestException("College is required");
            }
            collegeId = requestedCollegeId;
        } else if (SecurityUtils.isPrincipal()) {
            collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
            if (collegeId == null) throw new BadRequestException("Principal has no college assigned");
            if (requestedCollegeId != null && !requestedCollegeId.equals(collegeId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Payment QR settings are outside your college");
            }
        } else {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Payment QR settings access denied");
        }
        return colleges.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
    }

    private PaymentQrSettings response(College college) {
        return new PaymentQrSettings(
                college.getId(),
                college.getName(),
                college.getPaymentQrAccountName(),
                college.getQrCodeUrl() == null ? null
                        : "/api/college-settings/payment-qr/image?collegeId=" + college.getId());
    }

    public record PaymentQrSettings(
            Long collegeId,
            String collegeName,
            String accountName,
            String qrCodeUrl) {}
}
