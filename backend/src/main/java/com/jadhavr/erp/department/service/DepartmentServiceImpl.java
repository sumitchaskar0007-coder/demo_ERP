package com.jadhavr.erp.department.service;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "code", "status", "createdAt", "updatedAt");

    private final DepartmentRepository departmentRepository;
    private final CollegeRepository collegeRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(
            DepartmentRepository departmentRepository,
            CollegeRepository collegeRepository,
            DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.collegeRepository = collegeRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        College college = findCollege(request.collegeId());
        if (college.getStatus() == CollegeStatus.INACTIVE) {
            throw new BadRequestException("Cannot create department for an inactive college");
        }

        String normalizedCode = normalizeCode(request.code());
        if (departmentRepository.existsByCollegeIdAndCode(college.getId(), normalizedCode)) {
            throw new DuplicateResourceException(
                    "Department already exists with code " + normalizedCode
                            + " in college: " + college.getCode());
        }

        Department department = new Department();
        department.setCollege(college);
        department.setName(request.name().trim());
        department.setCode(normalizedCode);
        department.setDescription(request.description());
        department.setAdmissionFormFee(request.admissionFormFee());
        department.setStatus(DepartmentStatus.ACTIVE);
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toResponse(findDepartment(id));
    }

    @Override
    public DepartmentResponse getDepartmentByCollegeIdAndCode(Long collegeId, String code) {
        String normalizedCode = normalizeCode(code);
        Department department = departmentRepository
                .findByCollegeIdAndCode(collegeId, normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with code " + normalizedCode
                                + " in college with id: " + collegeId));
        return departmentMapper.toResponse(department);
    }

    @Override
    public List<DepartmentResponse> getDepartmentsByCollege(Long collegeId) {
        findCollege(collegeId);
        return departmentRepository.findByCollegeId(collegeId).stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "activeDepartments", key = "#collegeId", sync = true)
    public List<DepartmentResponse> getActiveDepartmentsByCollege(Long collegeId) {
        findCollege(collegeId);
        return departmentRepository
                .findByCollegeIdAndStatus(collegeId, DepartmentStatus.ACTIVE).stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = findDepartment(id);
        department.setName(request.name().trim());
        department.setDescription(request.description());
        department.setAdmissionFormFee(request.admissionFormFee());
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public DepartmentResponse activateDepartment(Long id) {
        Department department = findDepartment(id);
        if (department.getCollege().getStatus() == CollegeStatus.INACTIVE) {
            throw new BadRequestException(
                    "Cannot activate department because its college is inactive");
        }
        department.setStatus(DepartmentStatus.ACTIVE);
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public DepartmentResponse deactivateDepartment(Long id) {
        Department department = findDepartment(id);
        department.setStatus(DepartmentStatus.INACTIVE);
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public PageResponse<DepartmentResponse> searchDepartments(
            String keyword,
            Long collegeId,
            DepartmentStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Sort direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));
        Page<DepartmentResponse> result = departmentRepository
                .findAll(buildSpecification(keyword, collegeId, status), pageable)
                .map(departmentMapper::toResponse);
        return PageResponse.from(result);
    }

    private Specification<Department> buildSpecification(
            String keyword, Long collegeId, DepartmentStatus status) {
        Specification<Department> specification = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("code")), pattern),
                    builder.like(builder.lower(root.get("college").get("name")), pattern),
                    builder.like(builder.lower(root.get("college").get("code")), pattern)
            ));
        }
        if (collegeId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("college").get("id"), collegeId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        return specification;
    }

    private College findCollege(Long id) {
        return collegeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("College not found with id: " + id));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found with id: " + id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
