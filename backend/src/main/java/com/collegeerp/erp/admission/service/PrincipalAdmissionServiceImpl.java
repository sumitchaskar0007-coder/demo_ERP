package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.collegeerp.erp.admission.dto.StudentSectionAdmissionResponse;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.admission.mapper.AdmissionStatusHistoryMapper;
import com.collegeerp.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PrincipalAdmissionServiceImpl implements PrincipalAdmissionService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "admissionReferenceNumber", "fullName", "email", "status",
            "submittedAt", "createdAt", "updatedAt");

    private final AdmissionFormRepository admissions;
    private final AdmissionStatusHistoryRepository histories;
    private final StudentSectionAdmissionMapper admissionMapper;
    private final AdmissionStatusHistoryMapper historyMapper;

    public PrincipalAdmissionServiceImpl(
            AdmissionFormRepository admissions,
            AdmissionStatusHistoryRepository histories,
            StudentSectionAdmissionMapper admissionMapper,
            AdmissionStatusHistoryMapper historyMapper) {
        this.admissions = admissions;
        this.histories = histories;
        this.admissionMapper = admissionMapper;
        this.historyMapper = historyMapper;
    }

    @Override
    public PageResponse<StudentSectionAdmissionResponse> getReviewReadyAdmissions(
            String keyword, Long departmentId, int page, int size, String sortBy, String sortDir) {
        validatePage(page, size);
        Specification<AdmissionForm> spec = buildSpec(keyword, scopedCollegeId(), departmentId)
                .and((root, query, cb) -> cb.equal(root.get("status"), AdmissionStatus.PRINCIPAL_REVIEW_PENDING));
        return PageResponse.from(admissions.findAll(
                spec, PageRequest.of(page, size, Sort.by(directionOrDefault(sortDir), safeSort(sortBy))))
                .map(admissionMapper::toResponse));
    }

    @Override
    public StudentSectionAdmissionResponse getAdmissionForPrincipal(Long admissionId) {
        return admissionMapper.toResponse(findScopedAdmission(admissionId));
    }

    @Override
    public List<AdmissionStatusHistoryResponse> getAdmissionHistoryForPrincipal(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        return histories.findByAdmissionFormIdOrderByCreatedAtAsc(admission.getId())
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    private AdmissionForm findScopedAdmission(Long admissionId) {
        AdmissionForm admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        Long scopedCollegeId = scopedCollegeId();
        if (scopedCollegeId != null && !admission.getCollege().getId().equals(scopedCollegeId)) {
            throw new AccessDeniedException("Access denied");
        }
        return admission;
    }

    private Specification<AdmissionForm> buildSpec(String keyword, Long collegeId, Long departmentId) {
        Specification<AdmissionForm> spec = Specification.where(null);
        if (collegeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("college").get("id"), collegeId));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("admissionReferenceNumber")), pattern),
                    cb.like(cb.lower(root.get("student").get("admissionNumber")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("department").get("name")), pattern),
                    cb.like(cb.lower(root.get("department").get("code")), pattern)
            ));
        }
        return spec;
    }

    private Long scopedCollegeId() {
        return SecurityUtils.isSuperAdmin() ? null : SecurityUtils.requireCurrentUser().getCollegeId();
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new BadRequestException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new BadRequestException("Page size must be between 1 and 100");
    }

    private String safeSort(String sortBy) {
        return SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
    }

    private Sort.Direction directionOrDefault(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            return Sort.Direction.DESC;
        }
    }
}
