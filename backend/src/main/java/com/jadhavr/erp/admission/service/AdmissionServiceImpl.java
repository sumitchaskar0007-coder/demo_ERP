package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDepartmentOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.entity.AdmissionForm;
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

    @Autowired(required = false)
    public void setEmailNotifications(EmailNotificationService service) { this.emailNotifications = service; }

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
                temporaryPassword,
                "/login",
                "Admission submitted successfully. Please save your login credentials."
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
