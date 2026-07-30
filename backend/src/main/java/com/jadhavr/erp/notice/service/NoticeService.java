package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
import com.jadhavr.erp.notice.dto.NoticeResponse;
import com.jadhavr.erp.notice.entity.NoticePriority;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.user.entity.RoleName;
import java.util.List;
import java.util.Set;

public interface NoticeService {
    NoticeResponse create(CreateNoticeRequest request);
    NoticeResponse createWorkflowNotice(String title, String message, NoticePriority priority,
                                        Set<RoleName> audienceRoles, College college, String actionPath);
    List<NoticeResponse> inbox();
    long unreadCount();
    List<NoticeResponse> sent();
    void acknowledge(Long id);
    void markInboxSeen();
    void delete(Long id);
}
