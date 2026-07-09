package com.jadhavr.erp.common.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final int serverPort;

    public HealthController(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "College ERP Backend");
        data.put("port", serverPort);
        return ApiResponse.success("College ERP backend is running", data);
    }
}
