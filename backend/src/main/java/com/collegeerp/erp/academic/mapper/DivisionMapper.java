package com.collegeerp.erp.academic.mapper;

import com.collegeerp.erp.academic.dto.DivisionResponse;
import com.collegeerp.erp.academic.entity.Section;
import org.springframework.stereotype.Component;

@Component
public class DivisionMapper {
    public DivisionResponse toResponse(Section division) {
        var teacher = division.getClassTeacher();
        return new DivisionResponse(
                division.getId(),
                division.getCollege().getId(),
                division.getCollege().getName(),
                division.getCollege().getCode(),
                division.getDepartment().getId(),
                division.getDepartment().getName(),
                division.getDepartment().getCode(),
                division.getAcademicClass().getId(),
                division.getAcademicClass().getYearName(),
                division.getAcademicClass().getName(),
                division.getAcademicYear(),
                division.getName(),
                division.getCode(),
                division.getCapacity(),
                teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getFullName(),
                teacher == null ? null : teacher.getEmail(),
                division.getStatus(),
                division.getCreatedAt(),
                division.getUpdatedAt());
    }
}
