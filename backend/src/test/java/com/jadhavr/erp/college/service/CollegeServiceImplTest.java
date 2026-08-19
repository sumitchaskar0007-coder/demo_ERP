package com.jadhavr.erp.college.service;

import com.jadhavr.erp.academic.service.GlobalAcademicYearService;
import com.jadhavr.erp.college.dto.CollegeResponse;
import com.jadhavr.erp.college.dto.CreateCollegeRequest;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.common.dto.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollegeServiceImplTest {

    @Mock
    private CollegeRepository repository;
    @Mock
    private CollegeImageStorageService imageStorage;
    @Mock
    private GlobalAcademicYearService globalAcademicYears;

    private CollegeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CollegeServiceImpl(repository, imageStorage, globalAcademicYears);
    }

    @Test
    void createCollegeNormalizesCodeAndDefaultsStatus() {
        CreateCollegeRequest request = new CreateCollegeRequest(
                "ABC College", " abc001 ", null, "Pune", "Maharashtra",
                "411001", "admin@abc.com", null, null, null, null
        );
        when(repository.existsByCode("ABC001")).thenReturn(false);
        when(repository.saveAndFlush(any(College.class))).thenAnswer(invocation -> {
            College college = invocation.getArgument(0);
            if (college.getId() == null) college.setId(1L);
            return college;
        });

        CollegeResponse response = service.createCollege(request);

        assertEquals("ABC001", response.code());
        assertEquals(CollegeStatus.ACTIVE, response.status());
        verify(globalAcademicYears).provisionActiveYearForCollege(any(College.class));
    }

    @Test
    void createCollegeRejectsDuplicateCode() {
        CreateCollegeRequest request = new CreateCollegeRequest(
                "ABC College", "abc001", null, null, null,
                null, null, null, null, null, null
        );
        when(repository.existsByCode("ABC001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createCollege(request));
    }

    @Test
    void getCollegeByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getCollegeById(99L)
        );
        assertEquals("College not found with id: 99", exception.getMessage());
    }

    @Test
    void getActiveCollegesReturnsOnlyRepositoryResults() {
        College active = college(1L, "Active College", "ACTIVE01", CollegeStatus.ACTIVE);
        when(repository.findByStatus(CollegeStatus.ACTIVE)).thenReturn(List.of(active));

        var result = service.getActiveColleges();

        assertEquals(1, result.size());
        assertEquals(CollegeStatus.ACTIVE, result.get(0).status());
        verify(repository).findByStatus(CollegeStatus.ACTIVE);
    }

    @Test
    void deactivateCollegeChangesOnlyStatus() {
        College college = college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(college));
        when(repository.save(college)).thenReturn(college);

        CollegeResponse response = service.deactivateCollege(1L);

        assertEquals(CollegeStatus.INACTIVE, response.status());
        assertEquals("ABC001", response.code());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchCollegesReturnsPaginatedResponse() {
        College college = college(1L, "Pune College", "PUNE01", CollegeStatus.ACTIVE);
        when(repository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(college)));

        PageResponse<CollegeResponse> result =
                service.searchColleges("Pune", CollegeStatus.ACTIVE, 0, 10, "createdAt", "desc");

        assertEquals(1, result.totalElements());
        assertEquals("PUNE01", result.content().get(0).code());
        assertEquals(0, result.page());
    }

    private College college(
            Long id, String name, String code, CollegeStatus status) {
        College college = new College();
        college.setId(id);
        college.setName(name);
        college.setCode(code);
        college.setStatus(status);
        return college;
    }
}
