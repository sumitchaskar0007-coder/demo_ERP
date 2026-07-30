package com.jadhavr.erp.notice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeReceiptResponse(
        Long noticeId,
        NoticeDeliveryMode deliveryMode,
        int recipientCount,
        int seenCount,
        List<RecipientReceipt> recipients
) {
    public record RecipientReceipt(
            Long userId,
            String fullName,
            String email,
            boolean seen,
            LocalDateTime seenAt
    ) {}
}
