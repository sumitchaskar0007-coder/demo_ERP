package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdmissionDocumentPresignRequest(
        @NotBlank @Size(max = 255) String originalFilename,
        @NotBlank @Size(max = 100) String contentType,
        @Positive long fileSize,
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "sha256 must be a 64-character hexadecimal digest")
        String sha256) {
}
