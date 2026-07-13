package com.jadhavr.erp.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePrincipalRequest(
        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,
        @Pattern(
                regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,100}$",
                message = "Password must contain uppercase, lowercase, digit and special character")
        String password
) {}
