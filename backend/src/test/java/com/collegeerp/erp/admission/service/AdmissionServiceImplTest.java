package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.dto.SubmitAdmissionRequest;
import com.collegeerp.erp.admission.dto.AcademicRecordDto;
import com.collegeerp.erp.admission.dto.DetailedAdmissionRequest;
import com.collegeerp.erp.admission.dto.EntranceExamDto;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.entity.AdmissionStatusHistory;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.admission.exception.AdmissionInformationValidationException;
import com.collegeerp.erp.admission.mapper.AdmissionMapper;
import com.collegeerp.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.entity.CollegeStatus;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.department.entity.DepartmentStatus;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.fee.enums.StudentCategory;
import com.collegeerp.erp.email.service.EmailNotificationService;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.enums.StudentStatus;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.repository.RoleRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplTest {

    @Mock private CollegeRepository collegeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private AdmissionFormRepository admissionFormRepository;
    @Mock private AdmissionStatusHistoryRepository admissionStatusHistoryRepository;
    @Mock private EmailNotificationService emailNotifications;
    @Mock private AdmissionInformationValidationService informationValidation;

    private BCryptPasswordEncoder passwordEncoder;
    private AdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AdmissionServiceImpl(
                collegeRepository,
                departmentRepository,
                userRepository,
                roleRepository,
                studentProfileRepository,
                admissionFormRepository,
                passwordEncoder,
                new AdmissionMapper(),
                new StudentSectionAdmissionMapper(),
                admissionStatusHistoryRepository
        );
        service.setEmailNotifications(emailNotifications);
        service.setInformationValidation(informationValidation);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPublicAdmissionInfoSucceedsWithOnlyActiveDepartments() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findByCollegeIdAndStatus(1L, DepartmentStatus.ACTIVE))
                .thenReturn(List.of(department(10L, college, DepartmentStatus.ACTIVE)));

        var result = service.getPublicAdmissionInfo("abc001");

        assertEquals("ABC001", result.collegeCode());
        assertEquals(1, result.departments().size());
        assertEquals("BCA", result.departments().get(0).code());
        verify(departmentRepository).findByCollegeIdAndStatus(1L, DepartmentStatus.ACTIVE);
    }

    @Test
    void getPublicAdmissionInfoFailsWhenCollegeNotFound() {
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPublicAdmissionInfo("ABC001"));
    }

    @Test
    void getPublicAdmissionInfoFailsWhenCollegeInactive() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.getPublicAdmissionInfo("ABC001"));
    }

    @Test
    void submitAdmissionSucceedsAndCreatesStudentUserProfileAndAdmission() {
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college, DepartmentStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.existsByEmail("aarav.patil@example.com")).thenReturn(false);
        when(admissionFormRepository.existsByEmailAndCollegeIdAndStatusNotIn(
                anyString(), any(), any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole()));
        when(studentProfileRepository.existsByAdmissionNumber(anyString())).thenReturn(false);
        when(admissionFormRepository.existsByAdmissionReferenceNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(20L);
            return user;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0);
            profile.setId(30L);
            return profile;
        });
        when(admissionFormRepository.save(any(AdmissionForm.class))).thenAnswer(invocation -> {
            AdmissionForm form = invocation.getArgument(0);
            form.setId(40L);
            return form;
        });

        var result = service.submitAdmission("abc001", request());

        assertEquals(AdmissionStatus.SUBMITTED, result.status());
        assertEquals(20L, result.studentUserId());
        assertEquals(30L, result.studentProfileId());
        assertTrue(result.admissionReferenceNumber().startsWith("ADM-ABC001-"));
        assertTrue(result.admissionNumber().startsWith("STU-ABC001-"));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("aarav.patil@example.com", savedUser.getEmail());
        assertEquals(RoleName.STUDENT, savedUser.getRoles().iterator().next().getName());
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotifications).queueUserCreatedEmail(
                org.mockito.ArgumentMatchers.eq(savedUser), passwordCaptor.capture());
        String temporaryPassword = passwordCaptor.getValue();
        assertTrue(temporaryPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%*\\-_=+]).{20}$"));
        assertTrue(passwordEncoder.matches(temporaryPassword, savedUser.getPasswordHash()));
        assertTrue(savedUser.isMustChangePassword());

        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        assertEquals(StudentStatus.ADMISSION_SUBMITTED, profileCaptor.getValue().getStatus());
        assertEquals(StudentCategory.SC, profileCaptor.getValue().getStudentCategory());

        ArgumentCaptor<AdmissionForm> admissionCaptor = ArgumentCaptor.forClass(AdmissionForm.class);
        verify(admissionFormRepository).save(admissionCaptor.capture());
        assertEquals(AdmissionStatus.SUBMITTED, admissionCaptor.getValue().getStatus());
        assertEquals(StudentCategory.SC, admissionCaptor.getValue().getStudentCategory());
    }

    @Test
    void submitAdmissionFailsWhenCollegeNotFound() {
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAdmission("ABC001", request()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitAdmissionFailsWhenCollegeInactive() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitAdmissionFailsWhenDepartmentNotFound() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.ACTIVE)));
        when(departmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenDepartmentBelongsToAnotherCollege() {
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college(2L, CollegeStatus.ACTIVE), DepartmentStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenDepartmentInactive() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department(10L, college, DepartmentStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenUserEmailAlreadyExists() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department(10L, college, DepartmentStatus.ACTIVE)));
        when(userRepository.existsByEmail("aarav.patil@example.com")).thenReturn(true);

        AdmissionInformationValidationException exception = assertThrows(
                AdmissionInformationValidationException.class,
                () -> service.submitAdmission("ABC001", request()));

        assertEquals(
                "This email is already registered. Use another email or sign in.",
                exception.getFieldErrors().get("email"));
    }

    @Test
    void submitAdmissionShowsEmailFieldErrorWhenAnActiveAdmissionAlreadyExists() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department(10L, college, DepartmentStatus.ACTIVE)));
        when(userRepository.existsByEmail("aarav.patil@example.com")).thenReturn(false);
        when(admissionFormRepository.existsByEmailAndCollegeIdAndStatusNotIn(
                anyString(), any(), any())).thenReturn(true);

        AdmissionInformationValidationException exception = assertThrows(
                AdmissionInformationValidationException.class,
                () -> service.submitAdmission("ABC001", request()));

        assertEquals(
                "An admission has already been submitted with this email for this college.",
                exception.getFieldErrors().get("email"));
    }

    @Test
    void getMyLatestAdmissionFailsWhenAdmissionNotFound() {
        authenticateStudent(20L);
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyLatestAdmission());
    }

    @Test
    void getMyLatestAdmissionFailsWhenStudentProfileNotFound() {
        authenticateStudent(20L);
        assertThrows(ResourceNotFoundException.class, () -> service.getMyLatestAdmission());
        verify(admissionFormRepository).findTopByStudentUserIdOrderByCreatedAtDesc(20L);
    }

    @Test
    void getMyLatestAdmissionSucceeds() {
        authenticateStudent(20L);
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college, DepartmentStatus.ACTIVE);
        User user = new User();
        user.setId(20L);
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        AdmissionForm admission = new AdmissionForm();
        admission.setId(40L);
        admission.setAdmissionReferenceNumber("ADM-ABC001-2026-000001");
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(profile);
        admission.setStudentUser(user);
        admission.setAcademicYear("2026-2027");
        admission.setFullName("Aarav Rajesh Patil");
        admission.setEmail("aarav.patil@example.com");
        admission.setPhone("9876543210");
        admission.setDateOfBirth(LocalDate.of(2007, 5, 14));
        admission.setGender("Male");
        admission.setParentName("Rajesh Patil");
        admission.setParentPhone("9876500001");
        admission.setStatus(AdmissionStatus.SUBMITTED);
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));

        var result = service.getMyLatestAdmission();

        assertEquals(40L, result.id());
        assertEquals("ADM-ABC001-2026-000001", result.admissionReferenceNumber());
        assertEquals("STU-ABC001-2026-000001", result.admissionNumber());
    }

    @Test
    void incompleteAdmissionIsEditableAndKeepsStudentFeaturesLocked() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.SUBMITTED);
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));

        var access = service.getMyAdmissionAccess();

        assertFalse(access.formCompleted());
        assertTrue(access.editable());
        assertFalse(access.accessGranted());
    }

    @Test
    void completedAdmissionWaitsForFeeVerificationBeforeStudentSectionReview() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.SUBMITTED);
        admission.setPhotoStorageName("student-photo.jpg");
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));
        when(admissionFormRepository.save(admission)).thenReturn(admission);

        var result = service.submitMyAdmissionDetails(detailedRequest());

        assertEquals(AdmissionStatus.SUBMITTED, result.status());
        ArgumentCaptor<AdmissionStatusHistory> history =
                ArgumentCaptor.forClass(AdmissionStatusHistory.class);
        verify(admissionStatusHistoryRepository).save(history.capture());
        assertEquals(AdmissionStatus.SUBMITTED, history.getValue().getNewStatus());
    }

    @Test
    void rejectedAdmissionCanBeCorrectedAndResubmittedUsingSameRecord() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.STUDENT_SECTION_REJECTED);
        admission.setDetailsCompletedAt(LocalDateTime.now().minusDays(1));
        admission.setPhotoStorageName("student-photo.jpg");
        admission.setRejectionReason("Correct the address");
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));
        when(admissionFormRepository.save(admission)).thenReturn(admission);

        var result = service.submitMyAdmissionDetails(detailedRequest());

        assertEquals(40L, result.id());
        assertEquals(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING, result.status());
        assertEquals("Updated Pune address", result.addressLine1());
        assertEquals(List.of("MH-CET", "CMAT"),
                result.entranceExams().stream().map(EntranceExamDto::examName).toList());
        assertEquals(List.of("92.5 percentile", "Rank 120"),
                result.entranceExams().stream().map(EntranceExamDto::result).toList());
        assertNull(result.rejectionReason());
        assertEquals(StudentStatus.ADMISSION_SUBMITTED, admission.getStudent().getStatus());
        ArgumentCaptor<AdmissionStatusHistory> history =
                ArgumentCaptor.forClass(AdmissionStatusHistory.class);
        verify(admissionStatusHistoryRepository).save(history.capture());
        assertEquals(AdmissionStatus.STUDENT_SECTION_REJECTED, history.getValue().getOldStatus());
        assertEquals(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING, history.getValue().getNewStatus());
        verify(informationValidation).validate(admission, detailedRequest());
    }

    @Test
    void informationStepUsesTheSameValidatorWithoutSubmittingTheAdmission() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.SUBMITTED);
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));

        var result = service.validateMyAdmissionDetails(detailedRequest());

        assertTrue(result.valid());
        verify(informationValidation).validate(admission, detailedRequest());
        verify(admissionFormRepository, never()).save(any());
    }

    @Test
    void detailedAdmissionPersistsConfiguredOtherCategory() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.STUDENT_SECTION_REJECTED);
        admission.setDetailsCompletedAt(LocalDateTime.now().minusDays(1));
        admission.setPhotoStorageName("student-photo.jpg");
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));
        when(admissionFormRepository.save(admission)).thenReturn(admission);

        var result = service.submitMyAdmissionDetails(
                detailedRequest(StudentCategory.OTHER, "NT-C"));

        assertEquals(StudentCategory.OTHER, admission.getStudentCategory());
        assertEquals("NT-C", admission.getCustomCategoryName());
        assertEquals(StudentCategory.OTHER, admission.getStudent().getStudentCategory());
        assertEquals("NT-C", admission.getStudent().getCustomCategoryName());
        assertEquals("NT-C", result.customCategoryName());
    }

    @Test
    void pendingAdmissionCannotBeEditedByStudent() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        admission.setDetailsCompletedAt(LocalDateTime.now());
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));

        assertThrows(BadRequestException.class,
                () -> service.submitMyAdmissionDetails(detailedRequest()));
        verify(admissionFormRepository, never()).save(any());
    }

    @Test
    void admissionFormCannotChangeTheAuthenticatedLoginEmail() {
        authenticateStudent(20L);
        AdmissionForm admission = studentAdmission(AdmissionStatus.SUBMITTED);
        admission.setPhotoStorageName("student-photo.jpg");
        when(admissionFormRepository.findTopByStudentUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(admission));
        DetailedAdmissionRequest valid = detailedRequest();
        DetailedAdmissionRequest changedEmail = new DetailedAdmissionRequest(
                valid.courseYearId(), valid.fullName(), "attacker@example.com", valid.phone(), valid.dateOfBirth(),
                valid.gender(), valid.placeOfBirth(), valid.maritalStatus(), valid.aadhaarNumber(),
                valid.apaarId(), valid.nationality(), valid.religion(), valid.caste(),
                valid.studentCategory(), valid.customCategoryName(), valid.parentName(), valid.parentPhone(), valid.parentEmail(),
                valid.addressLine1(), valid.addressLine2(), valid.city(), valid.pincode(), valid.state(),
                valid.permanentPhone(), valid.permanentEmail(), valid.correspondenceAddress(),
                valid.correspondenceCity(), valid.correspondencePincode(), valid.correspondenceState(),
                valid.correspondencePhone(), valid.correspondenceMobile(), valid.correspondenceEmail(),
                valid.academicRecords(), valid.entranceExams(), valid.qualifyingEntranceSeatNumber(),
                valid.qualifyingEntranceTotalScore(), valid.lastGraduationCollegeName(),
                valid.lastGraduationCollegeAddress());

        assertThrows(BadRequestException.class,
                () -> service.submitMyAdmissionDetails(changedEmail));
        verify(admissionFormRepository, never()).save(any());
    }

    private SubmitAdmissionRequest request() {
        return new SubmitAdmissionRequest(
                10L,
                StudentCategory.SC,
                "Aarav",
                "Rajesh",
                "Patil",
                "Aarav.Patil@Example.com",
                "9876543210",
                LocalDate.of(2007, 5, 14),
                "Male",
                "Shivaji Nagar",
                "Near Bus Stand",
                "Pune",
                "Maharashtra",
                "411001",
                "Rajesh Patil",
                "9876500001",
                "Rajesh.Patil@Example.com",
                "ABC Junior College",
                "12th Science",
                new BigDecimal("78.50")
        );
    }

    private DetailedAdmissionRequest detailedRequest() {
        return detailedRequest(StudentCategory.SC, null);
    }

    private DetailedAdmissionRequest detailedRequest(StudentCategory category, String customCategoryName) {
        return new DetailedAdmissionRequest(
                1L, "Aarav Rajesh Patil", "aarav.patil@example.com", "9876543210",
                LocalDate.of(2007, 5, 14), "MALE", "Pune", "UNMARRIED",
                "123456789012", "APAAR123", "Indian", "Hindu", "Patil",
                category, customCategoryName, "Rajesh Patil", "9876500001",
                "rajesh@example.com", "Updated Pune address", "Near Bus Stand", "Pune",
                "411001", "Maharashtra", "9876543210", "aarav.patil@example.com",
                "Updated Pune address", "Pune", "411001", "Maharashtra", null,
                "9876543210", "aarav.patil@example.com",
                List.of(new AcademicRecordDto("12TH", "ABC College", "State Board", "2025",
                        new BigDecimal("500"), new BigDecimal("392.50"),
                        com.collegeerp.erp.admission.enums.AcademicGradingType.PERCENTAGE,
                        new BigDecimal("78.50"), null)),
                List.of(new EntranceExamDto("MH-CET", "92.5 percentile"),
                        new EntranceExamDto("CMAT", "Rank 120")),
                "MHT123", new BigDecimal("82.00"), "ABC College", "Pune");
    }

    private AdmissionForm studentAdmission(AdmissionStatus status) {
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college, DepartmentStatus.ACTIVE);
        User user = new User();
        user.setId(20L);
        user.setEmail("aarav.patil@example.com");
        user.setFullName("Aarav Rajesh Patil");
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        AdmissionForm admission = new AdmissionForm();
        admission.setId(40L);
        admission.setAdmissionReferenceNumber("ADM-ABC001-2026-000001");
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(profile);
        admission.setStudentUser(user);
        admission.setAcademicYear("2026-2027");
        admission.setStudentCategory(StudentCategory.SC);
        admission.setFullName(user.getFullName());
        admission.setEmail(user.getEmail());
        admission.setPhone("9876543210");
        admission.setDateOfBirth(LocalDate.of(2007, 5, 14));
        admission.setGender("MALE");
        admission.setParentName("Rajesh Patil");
        admission.setParentPhone("9876500001");
        admission.setStatus(status);
        return admission;
    }

    private College college(Long id, CollegeStatus status) {
        College college = new College();
        college.setId(id);
        college.setName("ABC College of Computer Science");
        college.setCode("ABC001");
        college.setStatus(status);
        return college;
    }

    private Department department(Long id, College college, DepartmentStatus status) {
        Department department = new Department();
        department.setId(id);
        department.setCollege(college);
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");
        department.setStatus(status);
        return department;
    }

    private Role studentRole() {
        Role role = new Role();
        role.setId(7L);
        role.setName(RoleName.STUDENT);
        return role;
    }

    private void authenticateStudent(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Aarav Patil");
        user.setEmail("aarav.patil@example.com");
        user.setPasswordHash("hash");
        user.setStatus(com.collegeerp.erp.user.entity.UserStatus.ACTIVE);
        user.setRoles(Set.of(studentRole()));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
