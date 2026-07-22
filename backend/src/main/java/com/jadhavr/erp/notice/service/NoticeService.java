package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
import com.jadhavr.erp.notice.dto.NoticeResponse;
import java.util.List;

public interface NoticeService {
    NoticeResponse create(CreateNoticeRequest request);
    List<NoticeResponse> inbox();
    List<NoticeResponse> sent();
    void acknowledge(Long id);
    void delete(Long id);
}
