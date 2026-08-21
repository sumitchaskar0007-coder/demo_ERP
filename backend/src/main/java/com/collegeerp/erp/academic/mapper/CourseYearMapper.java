package com.collegeerp.erp.academic.mapper;

import com.collegeerp.erp.academic.dto.CourseYearResponse;
import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.repository.SectionRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class CourseYearMapper {
    private final SectionRepository sections;

    public CourseYearMapper(SectionRepository sections) {
        this.sections = sections;
    }

    public CourseYearResponse toResponse(AcademicClass courseYear) {
        return toResponse(courseYear, sections.countByAcademicClassId(courseYear.getId()));
    }

    public CourseYearResponse toResponse(AcademicClass courseYear, long divisionCount) {
        return new CourseYearResponse(
                courseYear.getId(),
                courseYear.getCollege().getId(),
                courseYear.getCollege().getName(),
                courseYear.getCollege().getCode(),
                courseYear.getDepartment().getId(),
                courseYear.getDepartment().getName(),
                courseYear.getDepartment().getCode(),
                courseYear.getAcademicYear(),
                courseYear.getYearName(),
                courseYear.getName(),
                courseYear.getCode(),
                courseYear.getStatus(),
                divisionCount,
                courseYear.getCreatedAt(),
                courseYear.getUpdatedAt());
    }

    public List<Object[]> countDivisionsByCourseYearIds(Collection<Long> ids) {
        return sections.countByAcademicClassIds(ids);
    }
}
