package com.collegeerp.erp.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestHealthActuatorController {

    @GetMapping({
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    })
    Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
