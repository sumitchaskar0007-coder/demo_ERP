package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.entity.AcademicModels.AcademicTerm;
import com.collegeerp.erp.academic.entity.AcademicModels.AcademicYear;
import com.collegeerp.erp.academic.entity.CurriculumSemester;
import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.entity.SemesterOffering;
import com.collegeerp.erp.academic.enums.AcademicTermStatus;
import com.collegeerp.erp.academic.repository.AcademicTermRepository;
import com.collegeerp.erp.academic.repository.AcademicYearRepository;
import com.collegeerp.erp.academic.repository.CurriculumSemesterRepository;
import com.collegeerp.erp.academic.repository.SemesterOfferingRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AcademicSessionResolver {
    private final AcademicYearRepository years;
    private final AcademicTermRepository terms;
    private final CurriculumSemesterRepository semesters;
    private final SemesterOfferingRepository offerings;

    public AcademicSessionResolver(AcademicYearRepository years, AcademicTermRepository terms,
            CurriculumSemesterRepository semesters, SemesterOfferingRepository offerings) {
        this.years = years;
        this.terms = terms;
        this.semesters = semesters;
        this.offerings = offerings;
    }

    public SemesterOffering requireActiveOffering(Section section) {
        AcademicYear year = years.findByCollegeIdAndNormalizedName(section.getCollege().getId(), section.getAcademicYear())
                .orElseThrow(() -> new BadRequestException(
                        "Academic year " + section.getAcademicYear() + " is not configured"));
        AcademicTerm activeTerm = terms.findByCollegeIdAndStatus(section.getCollege().getId(), AcademicTermStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Activate an academic semester before allocating students"));
        if (!Objects.equals(activeTerm.getAcademicYear().getId(), year.getId())) {
            throw new BadRequestException("The division academic year does not match the active semester");
        }
        CurriculumSemester semester = semesters.findByDepartmentIdAndYearNameAndTermType(
                        section.getDepartment().getId(), section.getAcademicClass().getYearName(), activeTerm.getTermType())
                .filter(CurriculumSemester::isActive)
                .orElseThrow(() -> new BadRequestException(
                        "Configure " + activeTerm.getTermType().name().toLowerCase() + " semester for "
                                + section.getAcademicClass().getYearName().name().toLowerCase().replace('_', ' ')));
        return offerings.findByAcademicTermIdAndCurriculumSemesterId(activeTerm.getId(), semester.getId())
                .orElseThrow(() -> new BadRequestException("The active semester offering is not configured"));
    }

    public CurriculumSemester requireActiveSemester(AcademicClass courseYear) {
        AcademicTerm activeTerm = terms.findByCollegeIdAndStatus(courseYear.getCollege().getId(), AcademicTermStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Activate an academic semester before managing subjects"));
        if (!normalize(activeTerm.getAcademicYear().getName()).equals(normalize(courseYear.getAcademicYear()))) {
            throw new BadRequestException("The course year does not match the active academic semester");
        }
        return semesters.findByDepartmentIdAndYearNameAndTermType(courseYear.getDepartment().getId(),
                        courseYear.getYearName(), activeTerm.getTermType())
                .filter(CurriculumSemester::isActive)
                .orElseThrow(() -> new BadRequestException("Configure the active semester for this department"));
    }

    public CurriculumSemester requireSemester(AcademicClass courseYear, Integer semesterNumber) {
        if (semesterNumber == null) {
            throw new BadRequestException("Select a semester");
        }
        CurriculumSemester semester = semesters.findByDepartmentIdAndSemesterNumber(
                        courseYear.getDepartment().getId(), semesterNumber)
                .filter(CurriculumSemester::isActive)
                .orElseThrow(() -> new BadRequestException(
                        "Semester " + semesterNumber + " is not configured for this department"));
        if (!Objects.equals(semester.getCollege().getId(), courseYear.getCollege().getId())
                || semester.getYearName() != courseYear.getYearName()) {
            throw new BadRequestException(
                    "Semester " + semesterNumber + " does not belong to the selected course year");
        }
        return semester;
    }

    private String normalize(String value) {
        return value.trim().replace('/', '-');
    }
}
