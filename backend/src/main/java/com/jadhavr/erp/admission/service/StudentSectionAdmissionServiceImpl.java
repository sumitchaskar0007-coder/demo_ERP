package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.admission.enums.AdmissionAction;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.mapper.AdmissionPrintMapper;
import com.jadhavr.erp.admission.mapper.AdmissionStatusHistoryMapper;
import com.jadhavr.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.fee.service.FeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StudentSectionAdmissionServiceImpl implements StudentSectionAdmissionService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "admissionReferenceNumber", "fullName", "email", "status",
            "submittedAt", "createdAt", "updatedAt");
    private static final Set<AdmissionStatus> VERIFIABLE = Set.of(
            AdmissionStatus.SUBMITTED,
            AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);

    private final AdmissionFormRepository admissions;
    private final AdmissionStatusHistoryRepository histories;
    private final UserRepository users;
    private final StudentSectionAdmissionMapper admissionMapper;
    private final AdmissionStatusHistoryMapper historyMapper;
    private final AdmissionPrintMapper printMapper;
    private final FeeService feeService;

    @Autowired
    public StudentSectionAdmissionServiceImpl(
            AdmissionFormRepository admissions,
            AdmissionStatusHistoryRepository histories,
            UserRepository users,
            StudentSectionAdmissionMapper admissionMapper,
            AdmissionStatusHistoryMapper historyMapper,
            AdmissionPrintMapper printMapper, FeeService feeService) {
        this.admissions = admissions;
        this.histories = histories;
        this.users = users;
        this.admissionMapper = admissionMapper;
        this.historyMapper = historyMapper;
        this.printMapper = printMapper;
        this.feeService = feeService;
    }

    public StudentSectionAdmissionServiceImpl(AdmissionFormRepository admissions, AdmissionStatusHistoryRepository histories,
            UserRepository users, StudentSectionAdmissionMapper admissionMapper,
            AdmissionStatusHistoryMapper historyMapper, AdmissionPrintMapper printMapper) {
        this(admissions, histories, users, admissionMapper, historyMapper, printMapper, null);
    }

    @Override
    public PageResponse<StudentSectionAdmissionResponse> searchAdmissionsForStudentSection(
            String keyword, Long departmentId, AdmissionStatus status,
            int page, int size, String sortBy, String sortDir) {
        validatePage(page, size);
        Specification<AdmissionForm> spec = buildSpec(keyword, scopedCollegeId(), departmentId, status);
        return PageResponse.from(admissions.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(directionOrDefault(sortDir), safeSort(sortBy)))
        ).map(admissionMapper::toResponse));
    }

    @Override
    public StudentSectionAdmissionResponse getAdmissionForStudentSection(Long admissionId) {
        return admissionMapper.toResponse(findScopedAdmission(admissionId));
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse startReview(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        if (admission.getStatus() != AdmissionStatus.SUBMITTED) {
            throw new BadRequestException("Review can be started only for submitted admissions");
        }
        AdmissionStatus oldStatus = admission.getStatus();
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        AdmissionForm saved = admissions.save(admission);
        saveHistory(saved, oldStatus, saved.getStatus(),
                AdmissionAction.STUDENT_SECTION_REVIEW_STARTED,
                "Review started by Student Section");
        return admissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse approveAdmission(Long admissionId, VerifyAdmissionRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensureVerifiable(admission, "approve");
        AdmissionStatus oldStatus = admission.getStatus();
        User currentUser = currentUserEntity();
        if (request.studentCategory() == null) {
            throw new BadRequestException("Student category must be verified before approval");
        }
        admission.setStudentCategory(request.studentCategory());
        admission.getStudent().setStudentCategory(request.studentCategory());
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        admission.setStudentSectionVerifiedAt(LocalDateTime.now());
        admission.setStudentSectionVerifiedBy(currentUser);
        admission.setStudentSectionRemarks(trimToNull(request.remarks()));
        admission.getStudent().setStatus(StudentStatus.UNDER_REVIEW);
        AdmissionForm saved = admissions.save(admission);
        if (feeService != null) feeService.createAccountForAdmission(saved);
        saveHistory(saved, oldStatus, saved.getStatus(),
                AdmissionAction.STUDENT_SECTION_APPROVED, trimToNull(request.remarks()));
        return admissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse rejectAdmission(Long admissionId, RejectAdmissionRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensureVerifiable(admission, "reject");
        AdmissionStatus oldStatus = admission.getStatus();
        User currentUser = currentUserEntity();
        String reason = request.rejectionReason().trim();
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_REJECTED);
        admission.setRejectionReason(reason);
        admission.setStudentSectionRejectedAt(LocalDateTime.now());
        admission.setStudentSectionRejectedBy(currentUser);
        admission.getStudent().setStatus(StudentStatus.ADMISSION_REJECTED);
        AdmissionForm saved = admissions.save(admission);
        saveHistory(saved, oldStatus, saved.getStatus(),
                AdmissionAction.STUDENT_SECTION_REJECTED, reason);
        return admissionMapper.toResponse(saved);
    }

    @Override
    public List<AdmissionStatusHistoryResponse> getAdmissionHistory(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        return histories.findByAdmissionFormIdOrderByCreatedAtAsc(admission.getId())
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    @Override
    public AdmissionPrintResponse getPrintData(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensurePrintable(admission);
        return printMapper.toResponse(admission);
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse markAdmissionPrinted(Long admissionId, MarkAdmissionPrintedRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensurePrintable(admission);
        admission.setLastPrintedAt(LocalDateTime.now());
        admission.setLastPrintedBy(currentUserEntity());
        admission.setPrintCount((admission.getPrintCount() == null ? 0 : admission.getPrintCount()) + 1);
        AdmissionForm saved = admissions.save(admission);
        saveHistory(saved, AdmissionStatus.STUDENT_SECTION_APPROVED, AdmissionStatus.STUDENT_SECTION_APPROVED,
                AdmissionAction.ADMISSION_FORM_PRINTED,
                trimToNull(request.remarks()) == null ? "Admission form printed" : request.remarks().trim());
        return admissionMapper.toResponse(saved);
    }

    protected Specification<AdmissionForm> buildSpec(
            String keyword, Long collegeId, Long departmentId, AdmissionStatus status) {
        Specification<AdmissionForm> spec = Specification.where(null);
        if (collegeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("college").get("id"), collegeId));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
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

    protected AdmissionForm findScopedAdmission(Long admissionId) {
        AdmissionForm admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        Long scopedCollegeId = scopedCollegeId();
        if (scopedCollegeId != null && !admission.getCollege().getId().equals(scopedCollegeId)) {
            throw new AccessDeniedException("Access denied");
        }
        return admission;
    }

    private Long scopedCollegeId() {
        if (SecurityUtils.isSuperAdmin()) {
            return null;
        }
        CustomUserDetails currentUser = SecurityUtils.requireCurrentUser();
        if (currentUser.getCollegeId() == null) {
            throw new AccessDeniedException("Access denied");
        }
        return currentUser.getCollegeId();
    }

    private void ensureVerifiable(AdmissionForm admission, String action) {
        if (!VERIFIABLE.contains(admission.getStatus())) {
            throw new BadRequestException("Admission cannot be " + action + "d in current status");
        }
    }

    private void ensurePrintable(AdmissionForm admission) {
        if (admission.getStatus() != AdmissionStatus.STUDENT_SECTION_APPROVED) {
            throw new BadRequestException("Admission form can be printed only after Student Section approval");
        }
    }

    private User currentUserEntity() {
        return users.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private void saveHistory(
            AdmissionForm admission, AdmissionStatus oldStatus, AdmissionStatus newStatus,
            AdmissionAction action, String remarks) {
        AdmissionStatusHistory history = new AdmissionStatusHistory();
        history.setAdmissionForm(admission);
        history.setChangedBy(currentUserEntity());
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setAction(action);
        history.setRemarks(trimToNull(remarks));
        histories.save(history);
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

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
