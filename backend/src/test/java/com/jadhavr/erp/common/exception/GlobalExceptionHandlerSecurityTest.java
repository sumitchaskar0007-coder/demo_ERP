package com.jadhavr.erp.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerSecurityTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void databaseFailureReturnsGenericConflictAndCorrelationId() {
        var request = request();
        var exception = new DataIntegrityViolationException(
                "duplicate key violates constraint secret_schema.users_email_key",
                new SQLException("relation secret_schema.users, password=hidden", "23505"));

        var response = handler.handleDataIntegrity(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("The request conflicts with existing or related data",
                response.getBody().message());
        assertFalse(response.getBody().toString().contains("secret_schema"));
        assertFalse(response.getBody().toString().contains("password=hidden"));
        assertNotNull(response.getHeaders().getFirst("X-Request-ID"));
    }

    @Test
    void arbitraryIllegalArgumentMessageIsNeverReturned() {
        var response = handler.handleIllegalArgument(
                new IllegalArgumentException("C:\\app\\private\\config.yml: JDBC failure"),
                request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request contains an invalid value", response.getBody().message());
        assertFalse(response.getBody().toString().contains("config.yml"));
        assertFalse(response.getBody().toString().contains("JDBC"));
    }

    @Test
    void customClientExceptionCannotLeakInternalDetails() {
        var response = handler.handleBadRequest(
                new BadRequestException("/opt/app/private.env SQLSTATE 23505"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("The request is invalid", response.getBody().message());
        assertFalse(response.getBody().toString().contains("private.env"));
        assertFalse(response.getBody().toString().contains("SQLSTATE"));
    }

    @Test
    void unexpectedFailureRejectsUnsafeCallerCorrelationId() {
        var request = request();
        request.addHeader("X-Request-ID", "unsafe\nlog-entry");

        var response = handler.handleUnexpected(
                new IllegalStateException("/opt/app/secret.properties\n"
                        + "at com.jadhavr.Internal.run(Internal.java:12)"),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("The request could not be completed. Please try again.",
                response.getBody().message());
        assertFalse(response.getBody().toString().contains("secret.properties"));
        String correlationId = response.getHeaders().getFirst("X-Request-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.contains("\n"));
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/api/test");
    }
}
