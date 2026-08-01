package com.jadhavr.erp.email.dto;import jakarta.validation.constraints.*;public record ForgotPasswordRequest(@NotBlank @Email @Size(max=150) String email){}
