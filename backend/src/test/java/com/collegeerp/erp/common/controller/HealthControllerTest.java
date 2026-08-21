package com.collegeerp.erp.common.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthControllerTest {

    @Test
    void exposesTheDeployedReleaseForEndToEndRoutingVerification() {
        HealthController controller = new HealthController(8081, "abc123def456");

        var response = controller.health();

        assertTrue(response.success());
        assertEquals("UP", response.data().get("status"));
        assertEquals("abc123def456", response.data().get("release"));
    }
}
