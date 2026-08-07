package com.jadhavr.erp.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoveScholarshipRequest(
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
