package com.jadhavr.erp.auth.security;

public class AuthorizationStateUnavailableException extends RuntimeException {
    public AuthorizationStateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
