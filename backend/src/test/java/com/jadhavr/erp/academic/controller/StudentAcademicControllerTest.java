package com.jadhavr.erp.academic.controller;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.entity.StudentSectionEnrollment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.academic.service.AcademicService;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.timetable.service.WeeklyTimetableService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class StudentAcademicControllerTest {

    @Test
    void accessStateReflectsActiveDivisionEnrollment() {
        StudentProfileRepository students = mock(StudentProfileRepository.class);
        StudentSectionEnrollmentRepository enrollments = mock(StudentSectionEnrollmentRepository.class);
        StudentAcademicController controller = new StudentAcademicController(
                students, enrollments, mock(AcademicService.class), mock(WeeklyTimetableService.class));
        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        StudentProfile student = mock(StudentProfile.class);
        when(currentUser.getId()).thenReturn(7L);
        when(students.findByUserId(7L)).thenReturn(Optional.of(student));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireCurrentUser).thenReturn(currentUser);
            when(enrollments.findFirstByStudentAndStatus(student, AcademicStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertFalse(controller.accessState().data().divisionAllocated());

            StudentSectionEnrollment enrollment = mock(StudentSectionEnrollment.class);
            AcademicClass courseYear = mock(AcademicClass.class);
            Section division = mock(Section.class);
            when(enrollment.getAcademicClass()).thenReturn(courseYear);
            when(enrollment.getSection()).thenReturn(division);
            when(enrollment.getAcademicYear()).thenReturn("2026-27");
            when(enrollment.getRollNumber()).thenReturn("BCA-001");
            when(courseYear.getId()).thenReturn(11L);
            when(courseYear.getName()).thenReturn("BCA FY");
            when(division.getId()).thenReturn(21L);
            when(division.getName()).thenReturn("Division A");
            when(enrollments.findFirstByStudentAndStatus(student, AcademicStatus.ACTIVE))
                    .thenReturn(Optional.of(enrollment));

            var access = controller.accessState().data();
            assertTrue(access.divisionAllocated());
            assertEquals("Division A", access.division());
            assertEquals("BCA-001", access.rollNumber());
        }
    }
}
