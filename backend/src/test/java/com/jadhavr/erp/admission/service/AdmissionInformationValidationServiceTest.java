package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.admission.dto.AcademicRecordDto;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AcademicGradingType;
import com.jadhavr.erp.admission.exception.AdmissionInformationValidationException;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeeStructureRepository;
import com.jadhavr.erp.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionInformationValidationServiceTest {
    @Mock AcademicClassRepository courseYears;
    @Mock FeeStructureRepository feeStructures;

    private AdmissionInformationValidationService service;
    private AdmissionForm admission;
    private AcademicClass courseYear;

    @BeforeEach
    void setUp() {
        service = new AdmissionInformationValidationService(courseYears, feeStructures);
        College college = new College();
        college.setId(1L);
        Department department = new Department();
        department.setId(10L);
        department.setCollege(college);
        User user = new User();
        user.setEmail("student@example.com");
        admission = new AdmissionForm();
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudentUser(user);
        admission.setAcademicYear("2026-2027");
        courseYear = new AcademicClass();
        courseYear.setId(20L);
        courseYear.setCollege(college);
        courseYear.setDepartment(department);
        courseYear.setName("BCA First Year");
        courseYear.setYearName(CourseYearName.FIRST_YEAR);
        courseYear.setStatus(AcademicStatus.ACTIVE);
    }

    @Test
    void acceptsInformationWhenCourseAndFeeCategoryAreConfigured() {
        when(courseYears.findById(20L)).thenReturn(Optional.of(courseYear));
        when(feeStructures.findConfiguredAssessments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new FeeStructure()));

        assertDoesNotThrow(() -> service.validate(admission, request(
                "student@example.com", "MALE", StudentCategory.OPEN, null,
                AcademicGradingType.PERCENTAGE, new BigDecimal("80"), null)));
    }

    @Test
    void returnsStableFieldPathsForBusinessValidationErrors() {
        when(courseYears.findById(20L)).thenReturn(Optional.of(courseYear));

        AdmissionInformationValidationException error = assertThrows(
                AdmissionInformationValidationException.class,
                () -> service.validate(admission, request(
                        "different@example.com", "OTHER", StudentCategory.OTHER, "",
                        AcademicGradingType.CGPA, new BigDecimal("80"), new BigDecimal("8"))));

        assertEquals("The login email cannot be changed from the admission form",
                error.getFieldErrors().get("email"));
        assertEquals("Gender must be Male or Female", error.getFieldErrors().get("gender"));
        assertEquals("Enter either Percentage or CGPA, not both",
                error.getFieldErrors().get("academicRecords[0].marksPercentage"));
        assertEquals(true, error.getFieldErrors().containsKey("customCategoryName"));
    }

    @Test
    void identifiesAnUnconfiguredCategoryAtTheCategoryField() {
        when(courseYears.findById(20L)).thenReturn(Optional.of(courseYear));
        when(feeStructures.findConfiguredAssessments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        AdmissionInformationValidationException error = assertThrows(
                AdmissionInformationValidationException.class,
                () -> service.validate(admission, request(
                        "student@example.com", "FEMALE", StudentCategory.SC, null,
                        AcademicGradingType.PERCENTAGE, null, null)));

        assertEquals("Select an active fee category for this course year and gender",
                error.getFieldErrors().get("studentCategory"));
    }

    private DetailedAdmissionRequest request(
            String email, String gender, StudentCategory category, String customCategory,
            AcademicGradingType gradingType, BigDecimal percentage, BigDecimal cgpa) {
        return new DetailedAdmissionRequest(
                20L, "Test Student", email, "9876543210", LocalDate.of(2005, 1, 1),
                gender, "Pune", "UNMARRIED", "123456789012", "", "Indian", "Hindu",
                "Test", category, customCategory, "Test Guardian", "9876500000", "",
                "Permanent address", "", "Pune", "411001", "Maharashtra", null, null,
                "Correspondence address", "Pune", "411001", "Maharashtra", null, null, null,
                List.of(new AcademicRecordDto("12TH", "College", "Board", "2024",
                        null, null, gradingType, percentage, cgpa)),
                List.of(), null, null, "", "");
    }
}
