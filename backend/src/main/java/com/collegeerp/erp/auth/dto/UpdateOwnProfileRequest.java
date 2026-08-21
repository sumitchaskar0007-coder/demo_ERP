package com.collegeerp.erp.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,
        @Size(max = 500, message = "Address must not exceed 500 characters") String address,
        @Size(max = 500, message = "Bio must not exceed 500 characters") String bio
) {}
