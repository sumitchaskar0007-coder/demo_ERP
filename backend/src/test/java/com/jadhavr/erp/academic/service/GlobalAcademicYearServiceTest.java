package com.jadhavr.erp.academic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jadhavr.erp.academic.dto.GlobalAcademicYearDtos.SaveRequest;
import com.jadhavr.erp.academic.entity.GlobalAcademicYear;
import com.jadhavr.erp.academic.enums.AcademicYearStatus;
import com.jadhavr.erp.academic.repository.AcademicTermRepository;
import com.jadhavr.erp.academic.repository.AcademicYearRepository;
import com.jadhavr.erp.academic.repository.GlobalAcademicYearRepository;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalAcademicYearServiceTest {
    @Mock GlobalAcademicYearRepository globalYears;
    @Mock AcademicYearRepository collegeYears;
    @Mock AcademicTermRepository terms;
    @Mock CollegeRepository colleges;
    @Mock AuditLogService audit;
    private GlobalAcademicYearService service;

    @BeforeEach
    void setUp() {
        service = new GlobalAcademicYearService(globalYears, collegeYears, terms, colleges, audit);
    }

    @Test
    void createsRequestedAcademicYearAsDraft() {
        SaveRequest request = requestedRange();
        when(globalYears.countOverlapping(-1L, request.startDate(), request.endDate())).thenReturn(0L);
        when(globalYears.save(any(GlobalAcademicYear.class))).thenAnswer(invocation -> {
            GlobalAcademicYear year = invocation.getArgument(0);
            year.setId(7L);
            return year;
        });

        var created = service.create(request);

        assertEquals("2026-2027", created.name());
        assertEquals(LocalDate.of(2026, 6, 15), created.startDate());
        assertEquals(LocalDate.of(2027, 4, 30), created.endDate());
        assertEquals(AcademicYearStatus.DRAFT, created.status());
        verify(audit).log(any(), any(), any(), any(), any());
    }

    @Test
    void editsTheActiveAcademicYearDateRange() {
        GlobalAcademicYear year = year(7L, AcademicYearStatus.ACTIVE);
        when(globalYears.findById(7L)).thenReturn(Optional.of(year));
        when(globalYears.countOverlapping(7L, LocalDate.of(2026, 6, 15), LocalDate.of(2027, 4, 30)))
                .thenReturn(0L);
        when(collegeYears.findByGlobalAcademicYearId(7L)).thenReturn(List.of());

        var updated = service.update(7L, requestedRange());

        assertEquals(LocalDate.of(2026, 6, 15), updated.startDate());
        assertEquals(LocalDate.of(2027, 4, 30), updated.endDate());
        assertEquals(AcademicYearStatus.ACTIVE, updated.status());
    }

    @Test
    void activatesYearWhenThereAreNoCollegesYet() {
        GlobalAcademicYear year = year(7L, AcademicYearStatus.DRAFT);
        when(globalYears.findById(7L)).thenReturn(Optional.of(year));
        when(globalYears.findByStatus(AcademicYearStatus.ACTIVE)).thenReturn(Optional.empty());
        when(colleges.findByStatus(any())).thenReturn(List.of());

        var activated = service.activate(7L);

        assertEquals(AcademicYearStatus.ACTIVE, activated.status());
        verify(globalYears).flush();
    }

    @Test
    void rejectsANameThatDoesNotMatchTheDateRange() {
        SaveRequest request = new SaveRequest("2025-2026", LocalDate.of(2026, 6, 15),
                LocalDate.of(2027, 4, 30));

        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    private SaveRequest requestedRange() {
        return new SaveRequest("2026-2027", LocalDate.of(2026, 6, 15), LocalDate.of(2027, 4, 30));
    }

    private GlobalAcademicYear year(Long id, AcademicYearStatus status) {
        GlobalAcademicYear year = new GlobalAcademicYear();
        year.setId(id);
        year.setName("2026-2027");
        year.setStartDate(LocalDate.of(2026, 7, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setStatus(status);
        return year;
    }
}
