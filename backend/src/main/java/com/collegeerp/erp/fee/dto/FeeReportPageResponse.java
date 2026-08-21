package com.collegeerp.erp.fee.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public record FeeReportPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        BigDecimal totalAmount
) {
    public static <T> FeeReportPageResponse<T> from(Page<T> result, BigDecimal totalAmount) {
        return new FeeReportPageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast(),
                totalAmount == null ? BigDecimal.ZERO : totalAmount
        );
    }
}
