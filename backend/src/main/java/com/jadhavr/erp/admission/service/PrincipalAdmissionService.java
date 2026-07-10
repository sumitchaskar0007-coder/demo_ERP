package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.common.dto.PageResponse;

import java.util.List;

public interface PrincipalAdmissionService {
    PageResponse<StudentSectionAdmissionResponse> getReviewReadyAdmissions(
            String keyword, Long departmentId, int page, int size, String sortBy, String sortDir);
    StudentSectionAdmissionResponse getAdmissionForPrincipal(Long admissionId);
    List<AdmissionStatusHistoryResponse> getAdmissionHistoryForPrincipal(Long admissionId);
}
