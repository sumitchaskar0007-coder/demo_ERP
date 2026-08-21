package com.collegeerp.erp.email.dto;import jakarta.validation.constraints.*;public record TokenRequest(@NotBlank @Size(min=40,max=200)String token){}
