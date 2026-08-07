package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.admission.dto.AcademicRecordDto;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.exception.AdmissionInformationValidationException;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.enums.FeeStructureStatus;
import com.jadhavr.erp.fee.repository.FeeStructureRepository;
import com.jadhavr.erp.fee.service.FeeCategoryRules;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Business validation shared by the Information step and final submission. */
@Service
public class AdmissionInformationValidationService {
    private final AcademicClassRepository courseYears;
    private final FeeStructureRepository feeStructures;

    public AdmissionInformationValidationService(
            AcademicClassRepository courseYears,
            FeeStructureRepository feeStructures) {
        this.courseYears = courseYears;
        this.feeStructures = feeStructures;
    }

    public void validate(AdmissionForm admission, DetailedAdmissionRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (!email.equalsIgnoreCase(admission.getStudentUser().getEmail())) {
            errors.put("email", "The login email cannot be changed from the admission form");
        }

        AcademicClass courseYear = validateCourseYear(admission, request.courseYearId(), errors);
        String gender = validateGender(request.gender(), errors);
        String customCategory = validateCategoryName(request, errors);
        if (courseYear != null && gender != null && !errors.containsKey("customCategoryName")) {
            boolean configured = !feeStructures.findConfiguredAssessments(
                    admission.getCollege().getId(), admission.getDepartment().getId(),
                    FeeCategoryRules.academicYearVariants(admission.getAcademicYear()),
                    request.studentCategory(), customCategory, gender, courseYear.getName(),
                    FeeStructureStatus.ACTIVE).isEmpty();
            if (!configured) {
                errors.put("studentCategory",
                        "Select an active fee category for this course year and gender");
            }
        }

        if (request.academicRecords() != null) {
            for (int index = 0; index < request.academicRecords().size(); index++) {
                validateAcademicRecord(request.academicRecords().get(index), index, errors);
            }
        }

        if (!errors.isEmpty()) throw new AdmissionInformationValidationException(errors);
    }

    private AcademicClass validateCourseYear(
            AdmissionForm admission, Long courseYearId, Map<String, String> errors) {
        AcademicClass courseYear = courseYears.findById(courseYearId).orElse(null);
        if (courseYear == null
                || courseYear.getStatus() != AcademicStatus.ACTIVE
                || !courseYear.getCollege().getId().equals(admission.getCollege().getId())
                || !courseYear.getDepartment().getId().equals(admission.getDepartment().getId())
                || (courseYear.getYearName() != CourseYearName.FIRST_YEAR
                    && courseYear.getYearName() != CourseYearName.SECOND_YEAR
                    && courseYear.getYearName() != CourseYearName.THIRD_YEAR)) {
            errors.put("courseYearId", "Select an active FY, SY, or TY from your department");
            return null;
        }
        return courseYear;
    }

    private String validateGender(String value, Map<String, String> errors) {
        try {
            return FeeCategoryRules.normalizeGender(value);
        } catch (BadRequestException exception) {
            errors.put("gender", "Gender must be Male or Female");
            return null;
        }
    }

    private String validateCategoryName(
            DetailedAdmissionRequest request, Map<String, String> errors) {
        try {
            return FeeCategoryRules.normalizeCustomCategory(
                    request.studentCategory(), request.customCategoryName());
        } catch (BadRequestException exception) {
            errors.put(request.studentCategory() == com.jadhavr.erp.fee.enums.StudentCategory.OTHER
                    ? "customCategoryName" : "studentCategory", exception.getMessage());
            return null;
        }
    }

    private void validateAcademicRecord(
            AcademicRecordDto record, int index, Map<String, String> errors) {
        String prefix = "academicRecords[" + index + "]";
        if (record.gradingType() == com.jadhavr.erp.admission.enums.AcademicGradingType.PERCENTAGE
                && record.cgpa() != null) {
            errors.put(prefix + ".cgpa", "Enter either Percentage or CGPA, not both");
        }
        if (record.gradingType() == com.jadhavr.erp.admission.enums.AcademicGradingType.CGPA
                && record.marksPercentage() != null) {
            errors.put(prefix + ".marksPercentage", "Enter either Percentage or CGPA, not both");
        }
        if (record.gradingType() == com.jadhavr.erp.admission.enums.AcademicGradingType.CGPA
                && (record.totalMarks() != null || record.obtainedMarks() != null)) {
            errors.put(prefix + ".totalMarks", "Total and Obtained Marks apply only to Percentage");
        }
        if ((record.totalMarks() == null) != (record.obtainedMarks() == null)) {
            String missingField = record.totalMarks() == null ? ".totalMarks" : ".obtainedMarks";
            errors.put(prefix + missingField, "Enter both Total Marks and Obtained Marks");
        }
        if (record.totalMarks() != null && record.obtainedMarks() != null
                && record.obtainedMarks().compareTo(record.totalMarks()) > 0) {
            errors.put(prefix + ".obtainedMarks", "Obtained Marks cannot exceed Total Marks");
        }
    }
}
