package com.jadhavr.erp.college.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCollegeRequest(
        @NotBlank(message = "College name is required")
        @Size(min = 2, max = 150, message = "College name must be between 2 and 150 characters")
        String name,
        @Size(max = 500, message = "Address must not exceed 500 characters") String address,
        @Size(max = 100, message = "City must not exceed 100 characters") String city,
        @Size(max = 100, message = "State must not exceed 100 characters") String state,
        @Size(max = 10, message = "Pincode must not exceed 10 characters") String pincode,
        @Email(message = "Contact email must be valid")
        @Size(max = 150, message = "Contact email must not exceed 150 characters") String contactEmail,
        @Size(max = 20, message = "Contact phone must not exceed 20 characters") String contactPhone,
        @Size(max = 500, message = "Logo URL must not exceed 500 characters") String logoUrl,
        @Size(max = 500, message = "QR code URL must not exceed 500 characters") String qrCodeUrl,
        @Size(max = 150, message = "QR account name must not exceed 150 characters")
        String paymentQrAccountName
) {
}
