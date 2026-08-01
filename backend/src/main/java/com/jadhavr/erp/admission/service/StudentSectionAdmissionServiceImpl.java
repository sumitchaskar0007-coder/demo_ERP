package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.admission.entity.AdmissionAcademicRecord;
import com.jadhavr.erp.admission.enums.AdmissionAction;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.mapper.AdmissionPrintMapper;
import com.jadhavr.erp.admission.mapper.AdmissionStatusHistoryMapper;
import com.jadhavr.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRequirementRepository;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.fee.service.FeeService;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.email.service.EmailNotificationService;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AcademicClassRepository courseYears;
    private final AdmissionDocumentRepository documents;
    private AdmissionDocumentRequirementRepository documentRequirements;
    private EmailNotificationService emailNotifications;

    @Autowired(required = false)
    public void setEmailNotifications(EmailNotificationService service) {
        this.emailNotifications = service;
    }

    @Autowired(required = false)
    public void setDocumentRequirements(AdmissionDocumentRequirementRepository repository) {
        this.documentRequirements = repository;
    }

    @Autowired
    public StudentSectionAdmissionServiceImpl(
            AdmissionFormRepository admissions,
            AdmissionStatusHistoryRepository histories,
            UserRepository users,
            StudentSectionAdmissionMapper admissionMapper,
            AdmissionStatusHistoryMapper historyMapper,
            AdmissionPrintMapper printMapper, FeeService feeService,
            AcademicClassRepository courseYears,
            AdmissionDocumentRepository documents) {
        this.admissions = admissions;
        this.histories = histories;
        this.users = users;
        this.admissionMapper = admissionMapper;
        this.historyMapper = historyMapper;
        this.printMapper = printMapper;
        this.feeService = feeService;
        this.courseYears = courseYears;
        this.documents = documents;
    }

    public StudentSectionAdmissionServiceImpl(AdmissionFormRepository admissions, AdmissionStatusHistoryRepository histories,
            UserRepository users, StudentSectionAdmissionMapper admissionMapper,
            AdmissionStatusHistoryMapper historyMapper, AdmissionPrintMapper printMapper) {
        this(admissions, histories, users, admissionMapper, historyMapper, printMapper,
                null, null, null);
    }

    @Override
    public PageResponse<StudentSectionAdmissionResponse> searchAdmissionsForStudentSection(
            String keyword, Long departmentId, AdmissionStatus status,
            int page, int size, String sortBy, String sortDir) {
        validatePage(page, size);
        Specification<AdmissionForm> spec = buildSpec(keyword, scopedCollegeId(), departmentId, status);
        spec = spec.and((root, query, cb) -> {
            var paidAccounts = query.subquery(Long.class);
            var account = paidAccounts.from(StudentFeeAccount.class);
            paidAccounts.select(account.get("admissionForm").get("id"))
                    .where(
                            cb.equal(account.get("admissionForm").get("id"), root.get("id")),
                            cb.greaterThanOrEqualTo(
                                    account.get("paidAmount"),
                                    account.get("minimumAmountForAdmission")));
            return cb.exists(paidAccounts);
        });
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
    public List<AdmissionCourseYearOptionResponse> getCourseYearOptions(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        return courseYears.findByCollegeIdAndDepartmentIdAndStatus(
                        admission.getCollege().getId(), admission.getDepartment().getId(), AcademicStatus.ACTIVE)
                .stream()
                .filter(year -> year.getYearName() == CourseYearName.FIRST_YEAR
                        || year.getYearName() == CourseYearName.SECOND_YEAR
                        || year.getYearName() == CourseYearName.THIRD_YEAR)
                .sorted(java.util.Comparator.comparing(AcademicClass::getYearName))
                .map(year -> new AdmissionCourseYearOptionResponse(
                        year.getId(), year.getYearName(), year.getName(), year.getAcademicYear()))
                .toList();
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse startReview(Long admissionId) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        if (admission.getStatus() != AdmissionStatus.SUBMITTED) {
            throw new BadRequestException("Review can be started only for submitted admissions");
        }
        if (admission.getDetailsCompletedAt() == null) {
            throw new BadRequestException("The student must complete and submit the detailed admission form first");
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
    public StudentSectionAdmissionResponse updateDetails(
            Long admissionId, DetailedAdmissionRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensureVerifiable(admission, "update");
        if (courseYears != null) admission.setCourseYear(requireCourseYear(admission, request.courseYearId()));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        users.findByEmail(email)
                .filter(existing -> !existing.getId().equals(admission.getStudentUser().getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Email address is already used by another account");
                });

        admission.setFullName(request.fullName().trim());
        admission.setEmail(email);
        admission.setPhone(request.phone().trim());
        admission.setDateOfBirth(request.dateOfBirth());
        admission.setGender(request.gender().trim());
        admission.setPlaceOfBirth(request.placeOfBirth().trim());
        admission.setMaritalStatus(request.maritalStatus().trim());
        admission.setAadhaarNumber(request.aadhaarNumber().trim());
        admission.setApaarId(trimToNull(request.apaarId()));
        admission.setNationality(request.nationality().trim());
        admission.setReligion(request.religion().trim());
        admission.setCaste(request.caste().trim());
        admission.setStudentCategory(request.studentCategory());
        admission.setParentName(request.parentName().trim());
        admission.setParentPhone(request.parentPhone().trim());
        admission.setParentEmail(trimToNull(request.parentEmail()));
        admission.setAddressLine1(request.addressLine1().trim());
        admission.setAddressLine2(trimToNull(request.addressLine2()));
        admission.setCity(request.city().trim());
        admission.setPincode(request.pincode().trim());
        admission.setState(request.state().trim());
        admission.setPermanentPhone(trimToNull(request.permanentPhone()));
        admission.setPermanentEmail(trimToNull(request.permanentEmail()));
        admission.setCorrespondenceAddress(request.correspondenceAddress().trim());
        admission.setCorrespondenceCity(request.correspondenceCity().trim());
        admission.setCorrespondencePincode(request.correspondencePincode().trim());
        admission.setCorrespondenceState(request.correspondenceState().trim());
        admission.setCorrespondencePhone(trimToNull(request.correspondencePhone()));
        admission.setCorrespondenceMobile(trimToNull(request.correspondenceMobile()));
        admission.setCorrespondenceEmail(trimToNull(request.correspondenceEmail()));
        admission.setQualifyingEntranceSeatNumber(trimToNull(request.qualifyingEntranceSeatNumber()));
        admission.setQualifyingEntranceTotalScore(request.qualifyingEntranceTotalScore());
        admission.setLastGraduationCollegeName(trimToNull(request.lastGraduationCollegeName()));
        admission.setLastGraduationCollegeAddress(trimToNull(request.lastGraduationCollegeAddress()));
          admission.setAcademicRecords(request.academicRecords() == null ? new java.util.ArrayList<>() :
                 request.academicRecords().stream()
                         .map(this::academicRecord)
                          .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        admission.setDetailsCompletedAt(LocalDateTime.now());

        var student = admission.getStudent();
        student.setFullName(admission.getFullName());
        student.setEmail(email);
        student.setPhone(admission.getPhone());
        student.setDateOfBirth(admission.getDateOfBirth());
        student.setGender(admission.getGender());
        student.setAddressLine1(admission.getAddressLine1());
        student.setAddressLine2(admission.getAddressLine2());
        student.setCity(admission.getCity());
        student.setState(admission.getState());
        student.setPincode(admission.getPincode());
        student.setParentName(admission.getParentName());
        student.setParentPhone(admission.getParentPhone());
        student.setParentEmail(admission.getParentEmail());
        student.setStudentCategory(admission.getStudentCategory());

        var studentUser = admission.getStudentUser();
        studentUser.setFullName(admission.getFullName());
        studentUser.setEmail(email);
        studentUser.setPhone(admission.getPhone());
        AdmissionForm saved = admissions.save(admission);
        saveHistory(saved, saved.getStatus(), saved.getStatus(), AdmissionAction.STATUS_UPDATED,
                "Detailed admission form completed or corrected by Student Section");
        return admissionMapper.toResponse(saved);
      }

    private AcademicClass requireCourseYear(AdmissionForm admission, Long courseYearId) {
        AcademicClass year = courseYears.findById(courseYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Course year not found"));
        if (year.getStatus() != AcademicStatus.ACTIVE
                || !year.getCollege().getId().equals(admission.getCollege().getId())
                || !year.getDepartment().getId().equals(admission.getDepartment().getId())
                || (year.getYearName() != CourseYearName.FIRST_YEAR
                    && year.getYearName() != CourseYearName.SECOND_YEAR
                    && year.getYearName() != CourseYearName.THIRD_YEAR)) {
            throw new BadRequestException("Select an active FY, SY, or TY from the admission department");
        }
        return year;
    }

    private AdmissionAcademicRecord academicRecord(com.jadhavr.erp.admission.dto.AcademicRecordDto record) {
        BigDecimal total = record.totalMarks();
        BigDecimal obtained = record.obtainedMarks();
        if ((total == null) != (obtained == null)) {
            throw new BadRequestException("Enter both total and obtained marks for " + record.qualification());
        }
        if (total != null && (total.signum() <= 0 || obtained.signum() < 0 || obtained.compareTo(total) > 0)) {
            throw new BadRequestException("Obtained marks must be between zero and total marks for " + record.qualification());
        }
        BigDecimal percentage = total == null ? null
                : obtained.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        return new AdmissionAcademicRecord(record.qualification(), trimToNull(record.instituteName()),
                trimToNull(record.boardUniversity()), trimToNull(record.yearOfPassing()), total, obtained, percentage);
    }
    @Transactional
    @Override
    public StudentSectionAdmissionResponse approveAdmission(Long admissionId, VerifyAdmissionRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensurePendingReview(admission, "approved");
        AdmissionStatus oldStatus = admission.getStatus();
        User currentUser = currentUserEntity();
        if (admission.getDetailsCompletedAt() == null) {
            throw new BadRequestException("Complete the detailed admission form before approval");
        }
        if (admission.getPhotoStorageName() == null) {
            throw new BadRequestException("Upload the passport-size photo before approval");
        }
        if (documents != null) {
            Set<String> missing = documentRequirements == null
                    ? AdmissionDocumentType.requiredTypes().stream().map(Enum::name)
                            .collect(java.util.stream.Collectors.toSet())
                    : documentRequirements.findRequiredKeys(admission.getDepartment().getId());
            missing.removeAll(documents.findTypesByAdmissionId(admission.getId()));
            if (!missing.isEmpty()) {
                throw new BadRequestException("All required admission documents must be uploaded before approval");
            }
        }
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
        if (feeService != null) {
            feeService.createAccountForAdmission(saved);
            feeService.createRegularFeeAccount(saved);
        }
        saveHistory(saved, oldStatus, saved.getStatus(),
                AdmissionAction.STUDENT_SECTION_APPROVED, trimToNull(request.remarks()));
        if (emailNotifications != null) {
            emailNotifications.queueAdmissionApprovedEmail(
                    saved.getStudentUser(),
                    saved.getAdmissionReferenceNumber(),
                    saved.getStudent().getAdmissionNumber());
        }
        return admissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse rejectAdmission(Long admissionId, RejectAdmissionRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        ensurePendingReview(admission, "rejected");
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
        return printMapper.toResponse(admission);
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse markAdmissionPrinted(Long admissionId, MarkAdmissionPrintedRequest request) {
        AdmissionForm admission = findScopedAdmission(admissionId);
        AdmissionStatus currentStatus = admission.getStatus();
        admission.setLastPrintedAt(LocalDateTime.now());
        admission.setLastPrintedBy(currentUserEntity());
        admission.setPrintCount((admission.getPrintCount() == null ? 0 : admission.getPrintCount()) + 1);
        AdmissionForm saved = admissions.save(admission);
        saveHistory(saved, currentStatus, currentStatus,
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

    private void ensurePendingReview(AdmissionForm admission, String action) {
        if (admission.getStatus() != AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING) {
            throw new BadRequestException("Admission can be " + action + " only while pending review");
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
