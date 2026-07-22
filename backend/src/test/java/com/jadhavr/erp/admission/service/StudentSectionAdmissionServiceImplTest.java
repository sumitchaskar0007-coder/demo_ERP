package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionAcademicRecord;
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
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentSectionAdmissionServiceImplTest {
    @Mock private AdmissionFormRepository admissions;
    @Mock private AdmissionStatusHistoryRepository histories;
    @Mock private UserRepository users;

    private StudentSectionAdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentSectionAdmissionServiceImpl(
                admissions,
                histories,
                users,
                new StudentSectionAdmissionMapper(),
                new AdmissionStatusHistoryMapper(),
                new AdmissionPrintMapper()
        );
        authenticate(50L, 1L, RoleName.STUDENT_SECTION);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAdmissionDetailFailsForAnotherCollege() {
        when(admissions.findById(100L)).thenReturn(Optional.of(admission(100L, 2L, AdmissionStatus.SUBMITTED)));

        assertThrows(AccessDeniedException.class, () -> service.getAdmissionForStudentSection(100L));
    }

    @Test
    void startReviewSucceedsFromSubmitted() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.SUBMITTED);
        admission.setDetailsCompletedAt(LocalDateTime.now());
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(admissions.save(admission)).thenReturn(admission);
        when(users.findById(50L)).thenReturn(Optional.of(user(50L, 1L, RoleName.STUDENT_SECTION)));

        var result = service.startReview(100L);

        assertEquals(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING, result.status());
        verifyHistory(AdmissionAction.STUDENT_SECTION_REVIEW_STARTED);
    }

    @Test
    void startReviewFailsFromApprovedStatus() {
        when(admissions.findById(100L))
                .thenReturn(Optional.of(admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_APPROVED)));

        assertThrows(BadRequestException.class, () -> service.startReview(100L));
    }

    @Test
    void approveUpdatesAdmissionProfileAndHistory() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        admission.setDetailsCompletedAt(LocalDateTime.now());
        admission.setPhotoStorageName("student-photo.jpg");
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(admissions.save(admission)).thenReturn(admission);
        when(users.findById(50L)).thenReturn(Optional.of(user(50L, 1L, RoleName.STUDENT_SECTION)));

        var result = service.approveAdmission(100L, verificationRequest(StudentCategory.SC, "Verified"));

        assertEquals(AdmissionStatus.STUDENT_SECTION_APPROVED, result.status());
        assertEquals(StudentStatus.UNDER_REVIEW, admission.getStudent().getStatus());
        assertEquals(StudentCategory.SC, admission.getStudentCategory());
        assertEquals(StudentCategory.SC, admission.getStudent().getStudentCategory());
        assertEquals("Verified", admission.getStudentSectionRemarks());
        verifyHistory(AdmissionAction.STUDENT_SECTION_APPROVED);
    }

    @Test
    void rejectUpdatesAdmissionProfileAndHistory() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(admissions.save(admission)).thenReturn(admission);
        when(users.findById(50L)).thenReturn(Optional.of(user(50L, 1L, RoleName.STUDENT_SECTION)));

        var result = service.rejectAdmission(100L, new RejectAdmissionRequest("Parent phone number is invalid"));

        assertEquals(AdmissionStatus.STUDENT_SECTION_REJECTED, result.status());
        assertEquals(StudentStatus.ADMISSION_REJECTED, admission.getStudent().getStatus());
        assertEquals("Parent phone number is invalid", admission.getRejectionReason());
        verifyHistory(AdmissionAction.STUDENT_SECTION_REJECTED);
    }

    @Test
    void cannotApproveRejectedAdmission() {
        when(admissions.findById(100L))
                .thenReturn(Optional.of(admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_REJECTED)));

        assertThrows(BadRequestException.class,
                () -> service.approveAdmission(100L, verificationRequest(StudentCategory.OPEN, null)));
    }

    @Test
    void cannotRejectApprovedAdmission() {
        when(admissions.findById(100L))
                .thenReturn(Optional.of(admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_APPROVED)));

        assertThrows(BadRequestException.class,
                () -> service.rejectAdmission(100L, new RejectAdmissionRequest("Wrong data")));
    }

    private VerifyAdmissionRequest verificationRequest(StudentCategory category, String remarks) {
        return new VerifyAdmissionRequest(
                category,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                remarks
        );
    }

    @Test
    void printDataIsAvailableAsSoonAsAdmissionIsSubmitted() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.SUBMITTED);
        admission.setPhotoStorageName("student-photo.jpg");
        admission.setPlaceOfBirth("Pune");
        admission.setAadhaarNumber("123456789012");
        admission.setCorrespondenceAddress("Narhe Road");
        admission.setQualifyingEntranceSeatNumber("CET-101");
        admission.setAcademicRecords(List.of(new AdmissionAcademicRecord(
                "12TH", "ABC College", "State Board", "2025",
                new BigDecimal("100"), new BigDecimal("82.50"), new BigDecimal("82.50"))));
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));

        var result = service.getPrintData(100L);

        assertEquals("ADM-ABC001-2026-000001", result.admissionReferenceNumber());
        assertTrue(result.student().hasPhoto());
        assertEquals("Pune", result.student().placeOfBirth());
        assertEquals("123456789012", result.student().aadhaarNumber());
        assertEquals("Narhe Road", result.student().correspondenceAddress());
        assertEquals("CET-101", result.academic().qualifyingEntranceSeatNumber());
        assertEquals("ABC College", result.academic().academicRecords().get(0).instituteName());
    }

    @Test
    void printDataSucceedsAfterApproval() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_APPROVED);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));

        var result = service.getPrintData(100L);

        assertEquals("ADM-ABC001-2026-000001", result.admissionReferenceNumber());
        assertTrue(result.declarations().contains(
                "I understand that admission is subject to verification and approval."));
    }

    @Test
    void markPrintedIncrementsPrintCountAndCreatesHistory() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_APPROVED);
        admission.setPrintCount(1);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(admissions.save(admission)).thenReturn(admission);
        when(users.findById(50L)).thenReturn(Optional.of(user(50L, 1L, RoleName.STUDENT_SECTION)));

        var result = service.markAdmissionPrinted(
                100L, new MarkAdmissionPrintedRequest("Printed for physical verification"));

        assertEquals(2, result.printCount());
        assertEquals(AdmissionStatus.STUDENT_SECTION_APPROVED, result.status());
        verifyHistory(AdmissionAction.ADMISSION_FORM_PRINTED);
    }

    @Test
    void markDownloadedWorksBeforeApprovalAndKeepsCurrentStatus() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.SUBMITTED);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(admissions.save(admission)).thenReturn(admission);
        when(users.findById(50L)).thenReturn(Optional.of(user(50L, 1L, RoleName.STUDENT_SECTION)));

        var result = service.markAdmissionPrinted(100L, new MarkAdmissionPrintedRequest("PDF downloaded"));

        assertEquals(1, result.printCount());
        assertEquals(AdmissionStatus.SUBMITTED, result.status());
        verifyHistory(AdmissionAction.ADMISSION_FORM_PRINTED);
    }

    @Test
    void getHistoryReturnsOrderedList() {
        AdmissionForm admission = admission(100L, 1L, AdmissionStatus.STUDENT_SECTION_APPROVED);
        AdmissionStatusHistory history = new AdmissionStatusHistory();
        history.setId(1L);
        history.setAdmissionForm(admission);
        history.setNewStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        history.setAction(AdmissionAction.STUDENT_SECTION_APPROVED);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(histories.findByAdmissionFormIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(history));

        var result = service.getAdmissionHistory(100L);

        assertEquals(1, result.size());
        assertEquals(AdmissionAction.STUDENT_SECTION_APPROVED, result.get(0).action());
    }

    private void verifyHistory(AdmissionAction action) {
        ArgumentCaptor<AdmissionStatusHistory> captor = ArgumentCaptor.forClass(AdmissionStatusHistory.class);
        verify(histories).save(captor.capture());
        assertEquals(action, captor.getValue().getAction());
        assertEquals(50L, captor.getValue().getChangedBy().getId());
    }

    private AdmissionForm admission(Long id, Long collegeId, AdmissionStatus status) {
        College college = new College();
        college.setId(collegeId);
        college.setName("ABC College");
        college.setCode("ABC001");
        Department department = new Department();
        department.setId(10L);
        department.setCollege(college);
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        profile.setStatus(StudentStatus.ADMISSION_SUBMITTED);
        AdmissionForm admission = new AdmissionForm();
        admission.setId(id);
        admission.setAdmissionReferenceNumber("ADM-ABC001-2026-000001");
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(profile);
        admission.setStudentUser(user(60L, collegeId, RoleName.STUDENT));
        admission.setAcademicYear("2026-2027");
        admission.setFullName("Aarav Patil");
        admission.setEmail("aarav@example.com");
        admission.setPhone("9876543210");
        admission.setDateOfBirth(LocalDate.of(2007, 5, 14));
        admission.setGender("Male");
        admission.setParentName("Rajesh Patil");
        admission.setParentPhone("9876500001");
        admission.setStatus(status);
        admission.setPrintCount(0);
        return admission;
    }

    private User user(Long id, Long collegeId, RoleName roleName) {
        User user = new User();
        user.setId(id);
        College college = new College();
        college.setId(collegeId);
        user.setCollege(college);
        user.setFullName(roleName.name());
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private void authenticate(Long userId, Long collegeId, RoleName roleName) {
        User user = user(userId, collegeId, roleName);
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
