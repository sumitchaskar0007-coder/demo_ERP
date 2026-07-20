package com.jadhavr.erp.notice.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
import com.jadhavr.erp.notice.dto.NoticeResponse;
import com.jadhavr.erp.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService service;
    public NoticeController(NoticeService service) { this.service = service; }
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_NOTICE_SEND')")
    public ResponseEntity<ApiResponse<NoticeResponse>> create(@Valid @RequestBody CreateNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Notice sent successfully", service.create(request)));
    }
    @GetMapping("/inbox")
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public ApiResponse<List<NoticeResponse>> inbox() { return ApiResponse.success("Notices retrieved successfully", service.inbox()); }
    @GetMapping("/sent")
    @PreAuthorize("hasAuthority('PERM_NOTICE_SEND')")
    public ApiResponse<List<NoticeResponse>> sent() { return ApiResponse.success("Sent notices retrieved successfully", service.sent()); }
    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public ApiResponse<Void> acknowledge(@PathVariable Long id) {
        service.acknowledge(id);
        return ApiResponse.success("Notice acknowledged", null);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success("Notice deleted successfully", null);
    }
}
