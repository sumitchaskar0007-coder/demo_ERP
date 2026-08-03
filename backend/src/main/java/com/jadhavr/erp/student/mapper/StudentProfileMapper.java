package com.jadhavr.erp.student.mapper;

import com.jadhavr.erp.student.dto.StudentProfileResponse;
import com.jadhavr.erp.student.entity.StudentProfile;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileMapper {

    public StudentProfileResponse toResponse(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getAdmissionNumber(),
                profile.getStudentCategory(),
                profile.getCustomCategoryName(),
                profile.getCollege().getId(),
                profile.getCollege().getName(),
                profile.getCollege().getCode(),
                profile.getDepartment().getId(),
                profile.getDepartment().getName(),
                profile.getDepartment().getCode(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getParentName(),
                profile.getParentPhone(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
