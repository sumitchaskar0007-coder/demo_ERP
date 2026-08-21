package com.collegeerp.erp.student.controller;

import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.entity.StudentSectionEnrollment;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.student.service.AdminStudentService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStudentControllerTest {

    @Test
    void detailsAllowActiveEnrollmentWithoutRollNumber() {
        AdminStudentService studentService = mock(AdminStudentService.class);
        StudentProfileRepository students = mock(StudentProfileRepository.class);
        AdmissionFormRepository admissions = mock(AdmissionFormRepository.class);
        StudentSectionEnrollmentRepository enrollments = mock(StudentSectionEnrollmentRepository.class);
        WeeklyAttendanceRecordRepository attendance = mock(WeeklyAttendanceRecordRepository.class);
        StudentFeeAccountRepository feeAccounts = mock(StudentFeeAccountRepository.class);
        AdminStudentController controller = new AdminStudentController(
                studentService, students, admissions, enrollments, attendance, feeAccounts);

        StudentProfile student = mock(StudentProfile.class);
        StudentSectionEnrollment enrollment = mock(StudentSectionEnrollment.class);
        AcademicClass courseYear = mock(AcademicClass.class);
        Section division = mock(Section.class);

        when(students.findById(2L)).thenReturn(Optional.of(student));
        when(feeAccounts.findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.empty());
        when(admissions.findTopByStudentIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());
        when(enrollments.findFirstByStudentAndStatus(student, AcademicStatus.ACTIVE))
                .thenReturn(Optional.of(enrollment));
        when(attendance.findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(2L))
                .thenReturn(List.of());
        when(enrollment.getAcademicClass()).thenReturn(courseYear);
        when(enrollment.getSection()).thenReturn(division);
        when(enrollment.getAcademicYear()).thenReturn("2026-2027");
        when(enrollment.getRollNumber()).thenReturn(null);
        when(courseYear.getName()).thenReturn("MBA FY");
        when(division.getName()).thenReturn("Division A");

        Map<String, Object> details = controller.getStudentDetails(2L).data();
        @SuppressWarnings("unchecked")
        Map<String, Object> academic = (Map<String, Object>) details.get("academic");

        assertEquals("MBA FY", academic.get("courseYear"));
        assertEquals("Division A", academic.get("division"));
        assertEquals("2026-2027", academic.get("academicYear"));
        assertNull(academic.get("rollNumber"));
    }
}
