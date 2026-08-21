package com.collegeerp.erp.admission.mapper;

import com.collegeerp.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.collegeerp.erp.admission.entity.AdmissionStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class AdmissionStatusHistoryMapper {
    public AdmissionStatusHistoryResponse toResponse(AdmissionStatusHistory history) {
        return new AdmissionStatusHistoryResponse(
                history.getId(),
                history.getAdmissionForm().getId(),
                history.getAdmissionForm().getAdmissionReferenceNumber(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getAction(),
                history.getRemarks(),
                history.getChangedBy() == null ? null : history.getChangedBy().getId(),
                history.getChangedBy() == null ? null : history.getChangedBy().getFullName(),
                history.getCreatedAt()
        );
    }
}
