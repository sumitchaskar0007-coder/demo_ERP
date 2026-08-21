package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.dto.AcademicSessionDtos.*;
import java.util.List;

public interface AcademicSessionService {
    AcademicContext context();
    List<AcademicYearView> years();
    AcademicYearView createYear(CreateAcademicYearRequest request);
    AcademicYearView updateYear(Long id, UpdateAcademicYearRequest request);
    AcademicYearView activateYear(Long id);
    AcademicTermView updateTerm(Long id, UpdateAcademicTermRequest request);
    AcademicTermView activateTerm(Long id, ActivateAcademicTermRequest request);
    List<SemesterView> configureSemesters(ConfigureSemestersRequest request);
    List<SemesterView> semesters(Long departmentId);
    List<OfferingView> offerings(Long academicYearId);
    RolloverPreview preview(Long sourceTermId, Long targetTermId);
    RolloverResult execute(ExecuteRolloverRequest request);
}
