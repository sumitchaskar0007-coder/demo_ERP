package com.jadhavr.erp.email.dto;import jakarta.validation.constraints.*;public record ForgotPasswordRequest(@NotBlank @Email String email){}
