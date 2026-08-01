package com.jadhavr.erp.common.exception;

import com.jadhavr.erp.fee.entity.FeeStructure;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerOptimisticLockTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSpringOptimisticLockConflictToSafeHttp409() {
        var response = handler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException(FeeStructure.class, 42L), request());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "This record was changed by another request. Reload it and try again.",
                response.getBody().message());
    }

    @Test
    void mapsJpaOptimisticLockConflictWithoutExposingPersistenceDetails() {
        var response = handler.handleOptimisticLocking(
                new OptimisticLockException("stale row in secret_schema.fee_structures"), request());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().message().contains("secret_schema"));
        assertFalse(response.getBody().message().contains("stale row"));
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("PATCH", "/api/fee-structures/42");
    }
}
