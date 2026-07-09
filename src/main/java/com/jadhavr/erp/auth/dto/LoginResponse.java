package com.jadhavr.erp.auth.dto;

public record LoginResponse(
        String token, String tokenType, long expiresInMs, AuthUserResponse user
) {}
