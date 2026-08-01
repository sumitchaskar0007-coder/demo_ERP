package com.jadhavr.erp.department.service;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.department.dto.CreateDepartmentRequest;
import com.jadhavr.erp.department.dto.DepartmentResponse;
import com.jadhavr.erp.department.dto.UpdateDepartmentRequest;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.mapper.DepartmentMapper;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CollegeRepository collegeRepository;

    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(
                departmentRepository,
                collegeRepository,
                new DepartmentMapper()
        );
    }

    @Test
    void createDepartmentSucceeds() {
        College college = college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE);
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));
        when(departmentRepository.existsByCollegeIdAndCode(1L, "BCA")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse result = service.createDepartment(createRequest(1L, "bca"));

        assertEquals("BCA", result.code());
        assertEquals(DepartmentStatus.ACTIVE, result.status());
        assertEquals(1L, result.collegeId());
    }

    @Test
    void createDepartmentFailsWhenCollegeNotFound() {
        when(collegeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createDepartment(createRequest(99L, "bca")));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartmentFailsWhenCollegeInactive() {
        College college = college(1L, "ABC College", "ABC001", CollegeStatus.INACTIVE);
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));

        assertThrows(BadRequestException.class,
                () -> service.createDepartment(createRequest(1L, "bca")));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void duplicateCodeInsideCollegeIsRejected() {
        College college = college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE);
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));
        when(departmentRepository.existsByCollegeIdAndCode(1L, "BCA")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createDepartment(createRequest(1L, "bca")));
    }

    @Test
    void sameCodeIsAllowedInDifferentColleges() {
        College first = college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE);
        College second = college(2L, "XYZ College", "XYZ001", CollegeStatus.ACTIVE);
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(first));
        when(collegeRepository.findById(2L)).thenReturn(Optional.of(second));
        when(departmentRepository.existsByCollegeIdAndCode(any(Long.class), any(String.class)))
                .thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse firstResult =
                service.createDepartment(createRequest(1L, "bca"));
        DepartmentResponse secondResult =
                service.createDepartment(createRequest(2L, "bca"));

        assertEquals("BCA", firstResult.code());
        assertEquals("BCA", secondResult.code());
        assertEquals(1L, firstResult.collegeId());
        assertEquals(2L, secondResult.collegeId());
        verify(departmentRepository).existsByCollegeIdAndCode(1L, "BCA");
        verify(departmentRepository).existsByCollegeIdAndCode(2L, "BCA");
    }

    @Test
    void departmentCodeIsConvertedToUppercase() {
        College college = college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE);
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));
        when(departmentRepository.existsByCollegeIdAndCode(1L, "BCA_01")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse result = service.createDepartment(createRequest(1L, "bca_01"));

        assertEquals("BCA_01", result.code());
    }

    @Test
    void updateDoesNotChangeCodeOrCollege() {
        Department department = department(10L, DepartmentStatus.ACTIVE);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentResponse result = service.updateDepartment(
                10L,
                new UpdateDepartmentRequest(
                        "Updated Department", "Updated description", new BigDecimal("500.00"))
        );

        assertEquals("BCA", result.code());
        assertEquals(1L, result.collegeId());
        assertEquals("Updated Department", result.name());
    }

    @Test
    void deactivateChangesStatusToInactive() {
        Department department = department(10L, DepartmentStatus.ACTIVE);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentResponse result = service.deactivateDepartment(10L);

        assertEquals(DepartmentStatus.INACTIVE, result.status());
    }

    @Test
    void activateChangesStatusToActive() {
        Department department = department(10L, DepartmentStatus.INACTIVE);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentResponse result = service.activateDepartment(10L);

        assertEquals(DepartmentStatus.ACTIVE, result.status());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsPaginatedResponse() {
        Department department = department(10L, DepartmentStatus.ACTIVE);
        when(departmentRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(department)));

        var result = service.searchDepartments(
                "computer", 1L, DepartmentStatus.ACTIVE,
                0, 10, "createdAt", "desc"
        );

        assertEquals(1, result.totalElements());
        assertEquals("BCA", result.content().get(0).code());
    }

    private CreateDepartmentRequest createRequest(Long collegeId, String code) {
        return new CreateDepartmentRequest(
                collegeId,
                "Bachelor of Computer Applications",
                code,
                "Computer applications department",
                new BigDecimal("500.00")
        );
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

    private Department department(Long id, DepartmentStatus status) {
        Department department = new Department();
        department.setId(id);
        department.setCollege(
                college(1L, "ABC College", "ABC001", CollegeStatus.ACTIVE));
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");
        department.setDescription("Computer applications department");
        department.setStatus(status);
        return department;
    }
}
