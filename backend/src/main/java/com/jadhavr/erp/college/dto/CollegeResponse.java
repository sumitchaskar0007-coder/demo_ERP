package com.jadhavr.erp.college.dto;

import com.jadhavr.erp.college.entity.CollegeStatus;

import java.time.LocalDateTime;

public record CollegeResponse(
        Long id,
        String name,
        String code,
        String address,
        String city,
        String state,
        String pincode,
        String contactEmail,
        String contactPhone,
        String logoUrl,
        String qrCodeUrl,
        String paymentQrAccountName,
        CollegeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
