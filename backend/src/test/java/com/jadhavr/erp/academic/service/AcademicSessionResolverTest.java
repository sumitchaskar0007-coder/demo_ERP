package com.jadhavr.erp.academic.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicTerm;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicYear;
import com.jadhavr.erp.academic.entity.CurriculumSemester;
import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.entity.SemesterOffering;
import com.jadhavr.erp.academic.enums.AcademicTermStatus;
import com.jadhavr.erp.academic.enums.AcademicTermType;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.repository.AcademicTermRepository;
import com.jadhavr.erp.academic.repository.AcademicYearRepository;
import com.jadhavr.erp.academic.repository.CurriculumSemesterRepository;
import com.jadhavr.erp.academic.repository.SemesterOfferingRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.department.entity.Department;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicSessionResolverTest {
    @Mock AcademicYearRepository years;
    @Mock AcademicTermRepository terms;
    @Mock CurriculumSemesterRepository semesters;
    @Mock SemesterOfferingRepository offerings;
    private AcademicSessionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AcademicSessionResolver(years, terms, semesters, offerings);
    }

    @Test
    void resolvesTheOfferingForTheActiveTermAndCourseYear() {
        College college = mock(College.class);
        Department department = mock(Department.class);
        AcademicClass courseYear = mock(AcademicClass.class);
        Section section = mock(Section.class);
        AcademicYear year = mock(AcademicYear.class);
        AcademicTerm term = mock(AcademicTerm.class);
        CurriculumSemester semester = mock(CurriculumSemester.class);
        SemesterOffering offering = mock(SemesterOffering.class);

        when(college.getId()).thenReturn(1L);
        when(section.getCollege()).thenReturn(college);
        when(section.getDepartment()).thenReturn(department);
        when(section.getAcademicClass()).thenReturn(courseYear);
        when(section.getAcademicYear()).thenReturn("2026-2027");
        when(department.getId()).thenReturn(2L);
        when(courseYear.getYearName()).thenReturn(CourseYearName.FIRST_YEAR);
        when(year.getId()).thenReturn(3L);
        when(term.getId()).thenReturn(4L);
        when(term.getAcademicYear()).thenReturn(year);
        when(term.getTermType()).thenReturn(AcademicTermType.ODD);
        when(semester.getId()).thenReturn(5L);
        when(semester.isActive()).thenReturn(true);
        when(years.findByCollegeIdAndNormalizedName(1L, "2026-2027")).thenReturn(Optional.of(year));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.of(term));
        when(semesters.findByDepartmentIdAndYearNameAndTermType(2L, CourseYearName.FIRST_YEAR,
                AcademicTermType.ODD)).thenReturn(Optional.of(semester));
        when(offerings.findByAcademicTermIdAndCurriculumSemesterId(4L, 5L)).thenReturn(Optional.of(offering));

        assertSame(offering, resolver.requireActiveOffering(section));
    }

    @Test
    void rejectsAllocationWhenTheDivisionYearIsNotTheActiveTermYear() {
        College college = mock(College.class);
        Section section = mock(Section.class);
        AcademicYear divisionYear = mock(AcademicYear.class);
        AcademicYear activeYear = mock(AcademicYear.class);
        AcademicTerm term = mock(AcademicTerm.class);
        when(college.getId()).thenReturn(1L);
        when(section.getCollege()).thenReturn(college);
        when(section.getAcademicYear()).thenReturn("2026-2027");
        when(divisionYear.getId()).thenReturn(10L);
        when(activeYear.getId()).thenReturn(11L);
        when(term.getAcademicYear()).thenReturn(activeYear);
        when(years.findByCollegeIdAndNormalizedName(1L, "2026-2027")).thenReturn(Optional.of(divisionYear));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.of(term));

        assertThrows(BadRequestException.class, () -> resolver.requireActiveOffering(section));
    }

    @Test
    void resolvesAConfiguredSemesterBelongingToTheSelectedCourseYear() {
        College college = new College();
        college.setId(1L);
        Department department = new Department();
        department.setId(2L);
        CurriculumSemester semester = new CurriculumSemester();
        semester.setCollege(college);
        semester.setDepartment(department);
        semester.setSemesterNumber(2);
        semester.setYearName(CourseYearName.FIRST_YEAR);
        semester.setActive(true);
        AcademicClass courseYear = mock(AcademicClass.class);
        when(courseYear.getCollege()).thenReturn(college);
        when(courseYear.getDepartment()).thenReturn(department);
        when(courseYear.getYearName()).thenReturn(CourseYearName.FIRST_YEAR);
        when(semesters.findByDepartmentIdAndSemesterNumber(2L, 2))
                .thenReturn(Optional.of(semester));

        assertSame(semester, resolver.requireSemester(courseYear, 2));
    }

    @Test
    void rejectsSemesterFromAnotherCourseYear() {
        College college = new College();
        college.setId(1L);
        Department department = new Department();
        department.setId(2L);
        CurriculumSemester semester = new CurriculumSemester();
        semester.setCollege(college);
        semester.setDepartment(department);
        semester.setSemesterNumber(4);
        semester.setYearName(CourseYearName.SECOND_YEAR);
        semester.setActive(true);
        AcademicClass firstYear = mock(AcademicClass.class);
        when(firstYear.getCollege()).thenReturn(college);
        when(firstYear.getDepartment()).thenReturn(department);
        when(firstYear.getYearName()).thenReturn(CourseYearName.FIRST_YEAR);
        when(semesters.findByDepartmentIdAndSemesterNumber(2L, 4))
                .thenReturn(Optional.of(semester));

        assertThrows(BadRequestException.class, () -> resolver.requireSemester(firstYear, 4));
    }
}
