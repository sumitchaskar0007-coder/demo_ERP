package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDepartmentOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.StudentAdmissionAccessResponse;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionAcademicRecord;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.admission.enums.AdmissionAction;
import com.jadhavr.erp.admission.enums.AdmissionSource;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.mapper.AdmissionMapper;
import com.jadhavr.erp.admission.mapper.AdmissionPrintMapper;
import com.jadhavr.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRequirementRepository;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.util.TemporaryPasswordGenerator;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.jadhavr.erp.email.service.EmailNotificationService;
import com.jadhavr.erp.fee.service.FeeService;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdmissionServiceImpl implements AdmissionService {
    private static final List<AdmissionStatus> CLOSED_STATUSES = List.of(
            AdmissionStatus.STUDENT_SECTION_REJECTED,
            AdmissionStatus.PRINCIPAL_REJECTED,
            AdmissionStatus.CANCELLED
    );
    private static final String PASSWORD_SPECIALS = "@#$%!";

    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AdmissionFormRepository admissionFormRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdmissionMapper admissionMapper;
    private final StudentSectionAdmissionMapper detailedAdmissionMapper;
    private final AdmissionStatusHistoryRepository statusHistories;
    private final AcademicClassRepository courseYears;
    private final AdmissionDocumentRepository documents;
    private AdmissionDocumentRequirementRepository documentRequirements;
    private final SecureRandom random = new SecureRandom();
    private EmailNotificationService emailNotifications;
    private FeeService feeService;
    private AdmissionPrintMapper printMapper;
    @Autowired(required = false)
    public void setFeeService(FeeService service) { this.feeService = service; }

    @Autowired(required = false)
    public void setEmailNotifications(EmailNotificationService service) { this.emailNotifications = service; }

    @Autowired(required = false)
    public void setDocumentRequirements(AdmissionDocumentRequirementRepository repository) {
        this.documentRequirements = repository;
    }

    @Autowired
    public void setPrintMapper(AdmissionPrintMapper mapper) {
        this.printMapper = mapper;
    }

    @Autowired
    public AdmissionServiceImpl(
            CollegeRepository collegeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            AdmissionFormRepository admissionFormRepository,
            PasswordEncoder passwordEncoder,
            AdmissionMapper admissionMapper,
            StudentSectionAdmissionMapper detailedAdmissionMapper,
            AdmissionStatusHistoryRepository statusHistories,
            AcademicClassRepository courseYears,
            AdmissionDocumentRepository documents) {
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.admissionFormRepository = admissionFormRepository;
        this.passwordEncoder = passwordEncoder;
        this.admissionMapper = admissionMapper;
        this.detailedAdmissionMapper = detailedAdmissionMapper;
        this.statusHistories = statusHistories;
        this.courseYears = courseYears;
        this.documents = documents;
    }

    public AdmissionServiceImpl(
            CollegeRepository collegeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            AdmissionFormRepository admissionFormRepository,
            PasswordEncoder passwordEncoder,
            AdmissionMapper admissionMapper,
            StudentSectionAdmissionMapper detailedAdmissionMapper,
            AdmissionStatusHistoryRepository statusHistories) {
        this(collegeRepository, departmentRepository, userRepository, roleRepository,
                studentProfileRepository, admissionFormRepository, passwordEncoder, admissionMapper,
                detailedAdmissionMapper, statusHistories, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicAdmissionInfoResponse getPublicAdmissionInfo(String collegeCode) {
        College college = findCollegeByCode(collegeCode);
        ensureCollegeAcceptsAdmissions(college);

        List<AdmissionDepartmentOptionResponse> departments = departmentRepository
                .findByCollegeIdAndStatus(college.getId(), DepartmentStatus.ACTIVE)
                .stream()
                .map(department -> new AdmissionDepartmentOptionResponse(
                        department.getId(),
                        department.getName(),
                        department.getCode()
                ))
                .toList();

        return new PublicAdmissionInfoResponse(
                college.getId(),
                college.getName(),
                college.getCode(),
                college.getLogoUrl() == null ? null
                        : "/api/public/admissions/college/" + college.getCode() + "/logo",
                college.getContactEmail(),
                college.getContactPhone(),
                college.getAddress(),
                college.getCity(),
                college.getState(),
                academicYear(),
                departments
        );
    }

    @Override
    @Transactional
    public SubmitAdmissionResponse submitAdmission(String collegeCode, SubmitAdmissionRequest request) {
        College college = findCollegeByCode(collegeCode);
        ensureCollegeAcceptsAdmissions(college);

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!department.getCollege().getId().equals(college.getId())) {
            throw new BadRequestException("Department does not belong to this college");
        }
        if (department.getStatus() != DepartmentStatus.ACTIVE) {
            throw new BadRequestException("Department is not accepting admissions currently");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists with this email");
        }
        if (admissionFormRepository.existsByEmailAndCollegeIdAndStatusNotIn(
                email, college.getId(), CLOSED_STATUSES)) {
            throw new DuplicateResourceException(
                    "Admission already submitted for this email in this college");
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT role not found"));
        String fullName = buildFullName(
                request.firstName(), request.middleName(), request.lastName());
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        User user = new User();
        user.setCollege(college);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(trimToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(studentRole));
        User savedUser = userRepository.save(user);
        if (emailNotifications != null) {
            emailNotifications.queueUserCreatedEmail(savedUser, temporaryPassword);
        }

        StudentProfile profile = new StudentProfile();
        profile.setUser(savedUser);
        profile.setCollege(college);
        profile.setDepartment(department);
        profile.setAdmissionNumber(generateAdmissionNumber(college.getCode()));
        profile.setStudentCategory(request.studentCategory());
        copyStudentFields(profile, request, fullName, email);
        profile.setStatus(StudentStatus.ADMISSION_SUBMITTED);
        StudentProfile savedProfile = studentProfileRepository.save(profile);

        AdmissionForm admissionForm = new AdmissionForm();
        admissionForm.setAdmissionReferenceNumber(
                generateAdmissionReferenceNumber(college.getCode()));
        admissionForm.setCollege(college);
        admissionForm.setDepartment(department);
        admissionForm.setStudent(savedProfile);
        admissionForm.setStudentUser(savedUser);
        admissionForm.setAcademicYear(academicYear());
        admissionForm.setStudentCategory(request.studentCategory());
        copyAdmissionFields(admissionForm, request, fullName, email);
        admissionForm.setStatus(AdmissionStatus.SUBMITTED);
        admissionForm.setSource(AdmissionSource.PUBLIC_LINK);
        admissionForm.setSubmittedAt(LocalDateTime.now());
        AdmissionForm savedAdmission = admissionFormRepository.save(admissionForm);

        return new SubmitAdmissionResponse(
                savedAdmission.getAdmissionReferenceNumber(),
                savedProfile.getAdmissionNumber(),
                savedAdmission.getStatus(),
                savedUser.getId(),
                savedProfile.getId(),
                college.getName(),
                college.getCode(),
                department.getName(),
                department.getCode(),
                fullName,
                email,
                "/login",
                "Admission submitted successfully. Login credentials were sent to your email."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionResponse getMyLatestAdmission() {
        return admissionMapper.toResponse(findMyAdmission());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentSectionAdmissionResponse getMyDetailedAdmission() {
        return detailedAdmissionMapper.toResponse(findMyAdmission());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAdmissionAccessResponse getMyAdmissionAccess() {
        AdmissionForm admission = findMyAdmission();
        AdmissionStatus status = admission.getStatus();
        boolean completed = admission.getDetailsCompletedAt() != null;
        boolean editable = studentCanEdit(admission);
        boolean accessGranted = studentAccessGranted(status);
        boolean pending = completed && !editable && !accessGranted;
        return new StudentAdmissionAccessResponse(
                admission.getId(), status, completed, editable, pending, accessGranted,
                admission.getRejectionReason());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdmissionCourseYearOptionResponse> getMyCourseYearOptions() {
        AdmissionForm admission = findMyAdmission();
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
    public StudentSectionAdmissionResponse submitMyAdmissionDetails(DetailedAdmissionRequest request) {
        AdmissionForm admission = findMyAdmission();
        if (!studentCanEdit(admission)) {
            throw new BadRequestException("The admission form is read-only while it is pending or approved");
        }
        if (admission.getPhotoStorageName() == null) {
            throw new BadRequestException("Upload the passport-size photo before submitting the admission form");
        }
        if (documents != null) {
            Set<String> missingDocuments = documentRequirements == null
                    ? AdmissionDocumentType.requiredTypes().stream().map(Enum::name)
                            .collect(Collectors.toSet())
                    : documentRequirements.findRequiredKeys(admission.getDepartment().getId());
            missingDocuments.removeAll(documents.findTypesByAdmissionId(admission.getId()));
            if (!missingDocuments.isEmpty()) {
                throw new BadRequestException("Upload all required admission documents before submitting");
            }
        }

        String email = normalizeEmail(request.email());
        if (!email.equalsIgnoreCase(admission.getStudentUser().getEmail())) {
            throw new BadRequestException("The login email cannot be changed from the admission form");
        }

        AdmissionStatus oldStatus = admission.getStatus();
        copyDetailedFields(admission, request, email);
        admission.setDetailsCompletedAt(LocalDateTime.now());
        admission.setSubmittedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        admission.setRejectionReason(null);
        admission.setStudentSectionRejectedAt(null);
        admission.setStudentSectionRejectedBy(null);
        admission.setStudentSectionVerifiedAt(null);
        admission.setStudentSectionVerifiedBy(null);
        admission.setStudentSectionRemarks(null);
        admission.setPrincipalApprovedAt(null);
        admission.getStudent().setStatus(StudentStatus.ADMISSION_SUBMITTED);

        AdmissionForm saved = admissionFormRepository.save(admission);
        if (feeService != null) feeService.createAccountForAdmission(saved);
        saveStudentSubmissionHistory(saved, oldStatus,
                oldStatus == AdmissionStatus.SUBMITTED
                        ? "Detailed admission form submitted by student"
                        : "Rejected admission form corrected and resubmitted by student");
        if (emailNotifications != null) {
            emailNotifications.queueAdmissionCompletedEmail(
                    saved.getStudentUser(), saved.getAdmissionReferenceNumber());
        }
        return detailedAdmissionMapper.toResponse(saved);
    }

    private AdmissionForm findMyAdmission() {
        Long userId = currentUserId();
        AdmissionForm admission = admissionFormRepository
                .findTopByStudentUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (!admission.getStudentUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Admission does not belong to the current student");
        }
        return admission;
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionPrintResponse getMyAdmissionPrintData() {
        AdmissionForm admission = findMyAdmission();
        if (admission.getDetailsCompletedAt() == null) {
            throw new BadRequestException("Submit the detailed admission form before downloading it");
        }
        return printMapper.toResponse(admission);
    }

    private boolean studentCanEdit(AdmissionForm admission) {
        return (admission.getStatus() == AdmissionStatus.SUBMITTED
                && admission.getDetailsCompletedAt() == null)
                || admission.getStatus() == AdmissionStatus.STUDENT_SECTION_REJECTED
                || admission.getStatus() == AdmissionStatus.PRINCIPAL_REJECTED;
    }

    private boolean studentAccessGranted(AdmissionStatus status) {
        return status == AdmissionStatus.STUDENT_SECTION_APPROVED
                || status == AdmissionStatus.PRINCIPAL_REVIEW_PENDING
                || status == AdmissionStatus.PRINCIPAL_APPROVED;
    }

    private void copyDetailedFields(
            AdmissionForm admission, DetailedAdmissionRequest request, String email) {
        if (courseYears != null) admission.setCourseYear(requireCourseYear(admission, request.courseYearId()));
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
        admission.setParentEmail(normalizeOptionalEmail(request.parentEmail()));
        admission.setAddressLine1(request.addressLine1().trim());
        admission.setAddressLine2(trimToNull(request.addressLine2()));
        admission.setCity(request.city().trim());
        admission.setPincode(request.pincode().trim());
        admission.setState(request.state().trim());
        admission.setPermanentPhone(trimToNull(request.permanentPhone()));
        admission.setPermanentEmail(normalizeOptionalEmail(request.permanentEmail()));
        admission.setCorrespondenceAddress(request.correspondenceAddress().trim());
        admission.setCorrespondenceCity(request.correspondenceCity().trim());
        admission.setCorrespondencePincode(request.correspondencePincode().trim());
        admission.setCorrespondenceState(request.correspondenceState().trim());
        admission.setCorrespondencePhone(trimToNull(request.correspondencePhone()));
        admission.setCorrespondenceMobile(trimToNull(request.correspondenceMobile()));
        admission.setCorrespondenceEmail(normalizeOptionalEmail(request.correspondenceEmail()));
        admission.setQualifyingEntranceSeatNumber(trimToNull(request.qualifyingEntranceSeatNumber()));
        admission.setQualifyingEntranceTotalScore(request.qualifyingEntranceTotalScore());
        admission.setLastGraduationCollegeName(trimToNull(request.lastGraduationCollegeName()));
        admission.setLastGraduationCollegeAddress(trimToNull(request.lastGraduationCollegeAddress()));
        admission.setAcademicRecords(request.academicRecords() == null ? new java.util.ArrayList<>()
                : request.academicRecords().stream()
                        .map(this::academicRecord)
                        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));

        StudentProfile student = admission.getStudent();
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

        User user = admission.getStudentUser();
        user.setFullName(admission.getFullName());
        user.setEmail(email);
        user.setPhone(admission.getPhone());
    }

    private AcademicClass requireCourseYear(AdmissionForm admission, Long courseYearId) {
        AcademicClass courseYear = courseYears.findById(courseYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Course year not found"));
        if (courseYear.getStatus() != AcademicStatus.ACTIVE
                || !courseYear.getCollege().getId().equals(admission.getCollege().getId())
                || !courseYear.getDepartment().getId().equals(admission.getDepartment().getId())
                || (courseYear.getYearName() != CourseYearName.FIRST_YEAR
                    && courseYear.getYearName() != CourseYearName.SECOND_YEAR
                    && courseYear.getYearName() != CourseYearName.THIRD_YEAR)) {
            throw new BadRequestException("Select an active FY, SY, or TY from your department");
        }
        return courseYear;
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
                trimToNull(record.boardUniversity()), trimToNull(record.yearOfPassing()),
                total, obtained, percentage);
    }

    private void saveStudentSubmissionHistory(
            AdmissionForm admission, AdmissionStatus oldStatus, String remarks) {
        AdmissionStatusHistory history = new AdmissionStatusHistory();
        history.setAdmissionForm(admission);
        history.setChangedBy(admission.getStudentUser());
        history.setOldStatus(oldStatus);
        history.setNewStatus(admission.getStatus());
        history.setAction(AdmissionAction.SUBMITTED);
        history.setRemarks(remarks);
        statusHistories.save(history);
    }

    private College findCollegeByCode(String collegeCode) {
        String normalized = collegeCode == null ? "" : collegeCode.trim().toUpperCase(Locale.ROOT);
        return collegeRepository.findByCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
    }

    private void ensureCollegeAcceptsAdmissions(College college) {
        if (college.getStatus() != CollegeStatus.ACTIVE) {
            throw new BadRequestException("College is not accepting admissions currently");
        }
    }

    private String generateAdmissionReferenceNumber(String collegeCode) {
        return generateUniqueNumber("ADM", collegeCode,
                admissionFormRepository::existsByAdmissionReferenceNumber);
    }

    private String generateAdmissionNumber(String collegeCode) {
        return generateUniqueNumber("STU", collegeCode,
                studentProfileRepository::existsByAdmissionNumber);
    }

    private String generateUniqueNumber(String prefix, String collegeCode, UniqueChecker checker) {
        String year = String.valueOf(Year.now().getValue());
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "%s-%s-%s-%06d".formatted(
                    prefix, collegeCode, year, random.nextInt(1_000_000));
            if (!checker.exists(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Could not generate unique reference number");
    }


    private String academicYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        if (today.getMonthValue() >= 6) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        return Stream.of(firstName, middleName, lastName)
                .map(this::trimToNull)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        String value = trimToNull(email);
        return value == null ? null : normalizeEmail(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details.getId();
        }
        throw new BadRequestException("Authenticated user is invalid");
    }

    private void copyStudentFields(
            StudentProfile profile,
            SubmitAdmissionRequest request,
            String fullName,
            String email) {
        profile.setFirstName(request.firstName().trim());
        profile.setMiddleName(trimToNull(request.middleName()));
        profile.setLastName(request.lastName().trim());
        profile.setFullName(fullName);
        profile.setEmail(email);
        profile.setPhone(request.phone().trim());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender().trim());
        profile.setAddressLine1(trimToNull(request.addressLine1()));
        profile.setAddressLine2(trimToNull(request.addressLine2()));
        profile.setCity(trimToNull(request.city()));
        profile.setState(trimToNull(request.state()));
        profile.setPincode(trimToNull(request.pincode()));
        profile.setParentName(request.parentName() == null ? "" : request.parentName().trim());
        profile.setParentPhone(request.parentPhone() == null ? "" : request.parentPhone().trim());
        profile.setParentEmail(request.parentEmail() == null ? null : normalizeEmail(request.parentEmail()));
    }

    private void copyAdmissionFields(
            AdmissionForm admissionForm,
            SubmitAdmissionRequest request,
            String fullName,
            String email) {
        admissionForm.setFirstName(request.firstName().trim());
        admissionForm.setMiddleName(trimToNull(request.middleName()));
        admissionForm.setLastName(request.lastName().trim());
        admissionForm.setFullName(fullName);
        admissionForm.setEmail(email);
        admissionForm.setPhone(request.phone().trim());
        admissionForm.setDateOfBirth(request.dateOfBirth());
        admissionForm.setGender(request.gender().trim());
        admissionForm.setAddressLine1(trimToNull(request.addressLine1()));
        admissionForm.setAddressLine2(trimToNull(request.addressLine2()));
        admissionForm.setCity(trimToNull(request.city()));
        admissionForm.setState(trimToNull(request.state()));
        admissionForm.setPincode(trimToNull(request.pincode()));
        admissionForm.setParentName(request.parentName() == null ? "" : request.parentName().trim());
        admissionForm.setParentPhone(request.parentPhone() == null ? "" : request.parentPhone().trim());
        admissionForm.setParentEmail(request.parentEmail() == null ? null : normalizeEmail(request.parentEmail()));
        admissionForm.setPreviousSchoolName(trimToNull(request.previousSchoolName()));
        admissionForm.setPreviousClassName(trimToNull(request.previousClassName()));
        admissionForm.setPreviousPercentage(request.previousPercentage());
    }

    @FunctionalInterface
    private interface UniqueChecker {
        boolean exists(String value);
    }
}
