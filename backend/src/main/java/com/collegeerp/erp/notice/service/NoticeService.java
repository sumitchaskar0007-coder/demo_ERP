package com.collegeerp.erp.notice.service;

import com.collegeerp.erp.notice.dto.CreateNoticeRequest;
import com.collegeerp.erp.notice.dto.NoticeResponse;
import com.collegeerp.erp.notice.dto.NoticeRecipientOption;
import com.collegeerp.erp.notice.dto.NoticeReceiptResponse;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.notice.entity.NoticePriority;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import java.util.List;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NoticeService {
    NoticeResponse create(CreateNoticeRequest request);
    PageResponse<NoticeRecipientOption> searchRecipients(
            Long collegeId, Long departmentId, RoleName role, String query, int page, int size);
    NoticeReceiptResponse receipts(Long noticeId);
    NoticeResponse createWorkflowNotice(String title, String message, NoticePriority priority,
                                        Set<RoleName> audienceRoles, College college, String actionPath);
    NoticeResponse createUserWorkflowNotice(String title, String message, NoticePriority priority,
                                            RoleName audienceRole, College college, User recipient,
                                            String actionPath);
    List<NoticeResponse> inbox();
    long unreadCount();
    SseEmitter stream();
    List<NoticeResponse> sent();
    void acknowledge(Long id);
    void markInboxSeen();
    void delete(Long id);
}
