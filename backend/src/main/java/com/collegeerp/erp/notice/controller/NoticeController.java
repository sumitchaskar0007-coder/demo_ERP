package com.collegeerp.erp.notice.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.notice.dto.CreateNoticeRequest;
import com.collegeerp.erp.notice.dto.NoticeResponse;
import com.collegeerp.erp.notice.dto.NoticeRecipientOption;
import com.collegeerp.erp.notice.dto.NoticeReceiptResponse;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
    @GetMapping("/recipients")
    @PreAuthorize("hasAuthority('PERM_NOTICE_SEND')")
    public ApiResponse<PageResponse<NoticeRecipientOption>> recipients(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success("Notice recipients retrieved",
                service.searchRecipients(collegeId, departmentId, role, query, page, size));
    }
    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('PERM_NOTICE_SEND')")
    public ApiResponse<NoticeReceiptResponse> receipts(@PathVariable Long id) {
        return ApiResponse.success("Notice receipts retrieved", service.receipts(id));
    }
    @GetMapping("/inbox")
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public ApiResponse<List<NoticeResponse>> inbox() { return ApiResponse.success("Notices retrieved successfully", service.inbox()); }
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success("Unread notice count retrieved", service.unreadCount());
    }
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public SseEmitter stream() {
        return service.stream();
    }
    @PostMapping("/inbox/seen")
    @PreAuthorize("hasAuthority('PERM_NOTICE_READ')")
    public ApiResponse<Void> markInboxSeen() {
        service.markInboxSeen();
        return ApiResponse.success("Notices marked as seen", null);
    }
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
