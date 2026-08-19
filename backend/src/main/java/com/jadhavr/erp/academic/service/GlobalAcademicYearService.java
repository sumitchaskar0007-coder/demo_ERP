package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.GlobalAcademicYearDtos.SaveRequest;
import com.jadhavr.erp.academic.dto.GlobalAcademicYearDtos.View;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicTerm;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicYear;
import com.jadhavr.erp.academic.entity.GlobalAcademicYear;
import com.jadhavr.erp.academic.enums.*;
import com.jadhavr.erp.academic.repository.*;
import com.jadhavr.erp.audit.enums.AuditAction;
import com.jadhavr.erp.audit.enums.AuditModule;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GlobalAcademicYearService {
    private final GlobalAcademicYearRepository globalYears;
    private final AcademicYearRepository collegeYears;
    private final AcademicTermRepository terms;
    private final CollegeRepository colleges;
    private final AuditLogService audit;

    public GlobalAcademicYearService(GlobalAcademicYearRepository globalYears,
            AcademicYearRepository collegeYears, AcademicTermRepository terms,
            CollegeRepository colleges, AuditLogService audit) {
        this.globalYears = globalYears;
        this.collegeYears = collegeYears;
        this.terms = terms;
        this.colleges = colleges;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<View> list() {
        return globalYears.findAllByOrderByStartDateDesc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public View active() {
        return globalYears.findByStatus(AcademicYearStatus.ACTIVE).map(this::view).orElse(null);
    }

    public View create(SaveRequest request) {
        String name = normalizeAndValidate(request);
        if (globalYears.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Academic year already exists");
        }
        ensureNoOverlap(-1L, request);
        GlobalAcademicYear year = new GlobalAcademicYear();
        year.setName(name);
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(AcademicYearStatus.DRAFT);
        year = globalYears.save(year);
        audit.log(AuditModule.ACADEMIC, AuditAction.CREATE, "GlobalAcademicYear", year.getId(),
                "Created global academic year " + name);
        return view(year);
    }

    public View update(Long id, SaveRequest request) {
        GlobalAcademicYear year = requireYear(id);
        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new BadRequestException("Closed academic years cannot be edited");
        }
        String name = normalizeAndValidate(request);
        if (!year.getName().equalsIgnoreCase(name) && globalYears.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Academic year already exists");
        }
        ensureNoOverlap(id, request);
        List<AcademicYear> linkedYears = collegeYears.findByGlobalAcademicYearId(id);
        for (AcademicYear linked : linkedYears) {
            boolean termOutside = terms.findByAcademicYearIdOrderByStartDate(linked.getId()).stream()
                    .anyMatch(term -> term.getStartDate().isBefore(request.startDate())
                            || term.getEndDate().isAfter(request.endDate()));
            if (termOutside) {
                throw new BadRequestException("A college semester is outside the requested range. Adjust its semester dates before shrinking the global year");
            }
        }
        year.setName(name);
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        linkedYears.forEach(linked -> {
            linked.setName(name);
            linked.setStartDate(request.startDate());
            linked.setEndDate(request.endDate());
        });
        audit.log(AuditModule.ACADEMIC, AuditAction.UPDATE, "GlobalAcademicYear", id,
                "Updated global academic year " + name + " date range");
        return view(year);
    }

    public View activate(Long id) {
        GlobalAcademicYear target = requireYear(id);
        globalYears.findByStatus(AcademicYearStatus.ACTIVE)
                .filter(current -> !current.getId().equals(id))
                .ifPresent(current -> current.setStatus(AcademicYearStatus.CLOSED));
        globalYears.flush();

        for (College college : colleges.findByStatus(CollegeStatus.ACTIVE)) {
            provisionYearForCollege(target, college);
        }

        target.setStatus(AcademicYearStatus.ACTIVE);
        target.setActivatedAt(LocalDateTime.now());
        audit.log(AuditModule.ACADEMIC, AuditAction.ACTIVATE, "GlobalAcademicYear", id,
                "Activated " + target.getName() + " for all active colleges");
        return view(target);
    }

    public void provisionActiveYearForCollege(College college) {
        globalYears.findByStatus(AcademicYearStatus.ACTIVE)
                .ifPresent(year -> provisionYearForCollege(year, college));
    }

    private void provisionYearForCollege(GlobalAcademicYear target, College college) {
        AcademicYear collegeYear = collegeYears.findByCollegeIdAndNormalizedName(college.getId(), target.getName())
                .orElseGet(AcademicYear::new);
        Long targetCollegeYearId = collegeYear.getId();
        collegeYears.findByCollegeIdAndStatus(college.getId(), AcademicYearStatus.ACTIVE)
                .filter(current -> current.getId() != null && !current.getId().equals(targetCollegeYearId))
                .ifPresent(current -> current.setStatus(AcademicYearStatus.CLOSED));
        collegeYears.flush();
        collegeYear.setCollege(college);
        collegeYear.setGlobalAcademicYear(target);
        collegeYear.setName(target.getName());
        collegeYear.setStartDate(target.getStartDate());
        collegeYear.setEndDate(target.getEndDate());
        collegeYear.setStatus(AcademicYearStatus.ACTIVE);
        collegeYear = collegeYears.save(collegeYear);
        provisionTerms(collegeYear);
    }

    private void provisionTerms(AcademicYear year) {
        List<AcademicTerm> existing = terms.findByAcademicYearIdOrderByStartDate(year.getId());
        if (!existing.isEmpty()) {
            boolean outside = existing.stream().anyMatch(term -> term.getStartDate().isBefore(year.getStartDate())
                    || term.getEndDate().isAfter(year.getEndDate()));
            if (outside) throw new BadRequestException("Existing semester dates are outside " + year.getName());
            return;
        }
        long days = ChronoUnit.DAYS.between(year.getStartDate(), year.getEndDate());
        var oddEnd = year.getStartDate().plusDays(days / 2);
        createTerm(year, AcademicTermType.ODD, "Odd Semester", year.getStartDate(), oddEnd);
        createTerm(year, AcademicTermType.EVEN, "Even Semester", oddEnd.plusDays(1), year.getEndDate());
    }

    private void createTerm(AcademicYear year, AcademicTermType type, String name,
            java.time.LocalDate start, java.time.LocalDate end) {
        AcademicTerm term = new AcademicTerm();
        term.setCollege(year.getCollege());
        term.setAcademicYear(year);
        term.setTermType(type);
        term.setName(name);
        term.setStartDate(start);
        term.setEndDate(end);
        term.setStatus(AcademicTermStatus.PLANNED);
        terms.save(term);
    }

    private String normalizeAndValidate(SaveRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException("Academic year start date must be before its end date");
        }
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        if (days < 240 || days > 430) {
            throw new BadRequestException("Academic year must be between 240 and 430 days");
        }
        String name = request.name().trim().replace('/', '-');
        String[] parts = name.split("-");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        if (second != first + 1 || request.startDate().getYear() != first || request.endDate().getYear() != second) {
            throw new BadRequestException("Academic year name must match its start and end years");
        }
        return name;
    }

    private void ensureNoOverlap(Long excludedId, SaveRequest request) {
        if (globalYears.countOverlapping(excludedId, request.startDate(), request.endDate()) > 0) {
            throw new BadRequestException("Academic year dates overlap another configured year");
        }
    }

    private GlobalAcademicYear requireYear(Long id) {
        return globalYears.findById(id).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private View view(GlobalAcademicYear year) {
        return new View(year.getId(), year.getName(), year.getStartDate(), year.getEndDate(),
                year.getStatus(), year.getActivatedAt(),
                Math.toIntExact(collegeYears.countByGlobalAcademicYearId(year.getId())));
    }
}
