package com.jadhavr.erp.college.controller;

import com.jadhavr.erp.college.service.CollegePaymentQrService;
import com.jadhavr.erp.common.api.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/college-settings/payment-qr")
public class CollegePaymentQrController {
    private final CollegePaymentQrService service;

    public CollegePaymentQrController(CollegePaymentQrService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<CollegePaymentQrService.PaymentQrSettings> settings(
            @RequestParam(required = false) Long collegeId) {
        return ApiResponse.success("Payment QR settings retrieved", service.settings(collegeId));
    }

    @GetMapping("/image")
    public ResponseEntity<Resource> image(@RequestParam(required = false) Long collegeId) {
        var image = service.image(collegeId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=payment-qr")
                .contentType(image.mediaType())
                .body(image.resource());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<CollegePaymentQrService.PaymentQrSettings> update(
            @RequestParam(required = false) Long collegeId,
            @RequestParam("accountName") String accountName,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Payment QR code updated",
                service.update(collegeId, accountName, file));
    }
}
