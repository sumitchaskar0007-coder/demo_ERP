package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDepartmentOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionAcademicRecord;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.enums.AdmissionAction;
import com.jadhavr.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.jadhavr.erp.admission.enums.AdmissionSource;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.mapper.AdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
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
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.math.BigDecimal;
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
    private final SecureRandom random = new SecureRandom();
    private EmailNotificationService emailNotifications;
    private StudentSectionAdmissionMapper detailedAdmissionMapper;
    private AdmissionStatusHistoryRepository admissionHistories;

    @Autowired(required = false)
    public void setEmailNotifications(EmailNotificationService service) { this.emailNotifications = service; }

    @Autowired(required = false)
    public void setStudentOnboardingDependencies(StudentSectionAdmissionMapper mapper,
            AdmissionStatusHistoryRepository histories) {
        this.detailedAdmissionMapper = mapper;
        this.admissionHistories = histories;
    }

    public AdmissionServiceImpl(
            CollegeRepository collegeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            AdmissionFormRepository admissionFormRepository,
            PasswordEncoder passwordEncoder,
            AdmissionMapper admissionMapper) {
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.admissionFormRepository = admissionFormRepository;
        this.passwordEncoder = passwordEncoder;
        this.admissionMapper = admissionMapper;
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
                college.getLogoUrl(),
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
        String temporaryPassword = request.phone().trim();

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
        if (emailNotifications != null) emailNotifications.queueUserCreatedEmail(savedUser);

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
        admissionForm.setStatus(AdmissionStatus.STUDENT_DETAILS_PENDING);
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
                temporaryPassword,
                "/login",
                "Registration completed. Log in with these credentials and complete the detailed admission form."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionResponse getMyLatestAdmission() {
        Long userId = currentUserId();
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        AdmissionForm admissionForm = admissionFormRepository
                .findTopByStudentIdOrderByCreatedAtDesc(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return admissionMapper.toResponse(admissionForm);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentSectionAdmissionResponse getMyDetailedAdmission() {
        return detailedAdmissionMapper.toResponse(myAdmission());
    }

    @Override
    @Transactional
    public StudentSectionAdmissionResponse submitMyDetailedAdmission(DetailedAdmissionRequest request) {
        AdmissionForm admission = myAdmission();
        boolean correctionResubmission = admission.getStatus() == AdmissionStatus.STUDENT_SECTION_REJECTED;
        if (admission.getStatus() != AdmissionStatus.STUDENT_DETAILS_PENDING && !correctionResubmission) {
            throw new BadRequestException("The detailed admission form cannot be edited in its current status");
        }
        if (admission.getPhotoStorageName() == null
                || admission.getTenthMarksheetStorageName() == null
                || admission.getTwelfthMarksheetStorageName() == null
                || admission.getLeavingCertificateStorageName() == null
                || admission.getAadhaarCardStorageName() == null) {
            throw new BadRequestException("Upload your passport photo, 10th marksheet, 12th marksheet, leaving certificate, and Aadhaar card before submitting");
        }
        boolean tenthPresent = request.academicRecords() != null && request.academicRecords().stream()
                .anyMatch(record -> "10TH".equalsIgnoreCase(record.qualification()) && validMarks(record));
        boolean twelfthPresent = request.academicRecords() != null && request.academicRecords().stream()
                .anyMatch(record -> "12TH".equalsIgnoreCase(record.qualification()) && validMarks(record));
        if (!tenthPresent || !twelfthPresent) {
            throw new BadRequestException("Enter both 10th and 12th academic marks before submitting");
        }
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(admission.getStudentUser().getId()))
                .ifPresent(existing -> { throw new DuplicateResourceException("Email address is already used by another account"); });

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
        admission.setAcademicRecords(request.academicRecords().stream().map(this::academicRecord)
                .collect(Collectors.toCollection(java.util.ArrayList::new)));
        admission.setDetailsCompletedAt(LocalDateTime.now());
        AdmissionStatus oldStatus = admission.getStatus();
        admission.setStatus(AdmissionStatus.SUBMITTED);
        admission.setSubmittedAt(LocalDateTime.now());
        admission.setRejectionReason(null);
        admission.setStudentSectionRejectedAt(null);
        admission.setStudentSectionRejectedBy(null);
        admission.setStudentSectionVerifiedAt(null);
        admission.setStudentSectionVerifiedBy(null);
        admission.setStudentSectionRemarks(null);
        admission.setPhotoVerified(false);
        admission.setTenthMarksheetVerified(false);
        admission.setTwelfthMarksheetVerified(false);
        admission.setLeavingCertificateVerified(false);
        admission.setAadhaarCardVerified(false);
        admission.setGraduationPgCertificateVerified(false);
        admission.setMigrationCertificateVerified(false);
        admission.setGapAffidavitVerified(false);
        admission.setCasteCertificateVerified(false);
        admission.setIncomeProofVerified(false);
        admission.setNameChangeCertificateVerified(false);

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
        student.setStatus(StudentStatus.ADMISSION_SUBMITTED);
        User studentUser = admission.getStudentUser();
        studentUser.setFullName(admission.getFullName());
        studentUser.setEmail(email);
        studentUser.setPhone(admission.getPhone());
        AdmissionForm saved = admissionFormRepository.save(admission);
        if (admissionHistories != null) {
            AdmissionStatusHistory history = new AdmissionStatusHistory();
            history.setAdmissionForm(saved);
            history.setChangedBy(studentUser);
            history.setOldStatus(oldStatus);
            history.setNewStatus(saved.getStatus());
            history.setAction(AdmissionAction.STUDENT_DETAILS_SUBMITTED);
            history.setRemarks(correctionResubmission
                    ? "Corrected admission form and documents resubmitted by student"
                    : "Detailed form and required documents submitted by student");
            admissionHistories.save(history);
        }
        return detailedAdmissionMapper.toResponse(saved);
    }

    private AdmissionForm myAdmission() {
        StudentProfile profile = studentProfileRepository.findByUserId(currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return admissionFormRepository.findTopByStudentIdOrderByCreatedAtDesc(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
    }

    private boolean validMarks(com.jadhavr.erp.admission.dto.AcademicRecordDto record) {
        return record.totalMarks() != null && record.totalMarks().signum() > 0
                && record.obtainedMarks() != null && record.obtainedMarks().signum() >= 0
                && record.obtainedMarks().compareTo(record.totalMarks()) <= 0;
    }

    private AdmissionAcademicRecord academicRecord(com.jadhavr.erp.admission.dto.AcademicRecordDto record) {
        BigDecimal percentage = null;
        if (record.totalMarks() != null && record.totalMarks().signum() > 0 && record.obtainedMarks() != null) {
            if (record.obtainedMarks().compareTo(record.totalMarks()) > 0) throw new BadRequestException("Obtained marks cannot exceed total marks");
            percentage = record.obtainedMarks().multiply(BigDecimal.valueOf(100))
                    .divide(record.totalMarks(), 2, java.math.RoundingMode.HALF_UP);
        }
        return new AdmissionAcademicRecord(record.qualification(), trimToNull(record.instituteName()),
                trimToNull(record.boardUniversity()), trimToNull(record.yearOfPassing()),
                record.totalMarks(), record.obtainedMarks(), percentage);
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
