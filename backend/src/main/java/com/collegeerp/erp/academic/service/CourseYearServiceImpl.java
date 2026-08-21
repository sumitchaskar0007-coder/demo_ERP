package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.dto.CreateCourseYearRequest;
import com.collegeerp.erp.academic.dto.CourseYearResponse;
import com.collegeerp.erp.academic.dto.UpdateCourseYearRequest;
import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.academic.mapper.CourseYearMapper;
import com.collegeerp.erp.academic.repository.AcademicClassRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.DuplicateResourceException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.department.entity.DepartmentStatus;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CourseYearServiceImpl implements CourseYearService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "code", "academicYear", "status");
    private final AcademicClassRepository courseYears;
    private final DepartmentRepository departments;
    private final CourseYearMapper mapper;

    public CourseYearServiceImpl(AcademicClassRepository courseYears,
            DepartmentRepository departments, CourseYearMapper mapper) {
        this.courseYears = courseYears;
        this.departments = departments;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCourseYears", allEntries = true)
    public CourseYearResponse create(CreateCourseYearRequest request) {
        requirePrincipal();
        Department department = departments.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        scope(department.getCollege().getId());
        if (department.getStatus() != DepartmentStatus.ACTIVE) {
            throw new BadRequestException("Course Year requires an active department");
        }
        String academicYear = request.academicYear().trim();
        if (courseYears.existsByCollegeIdAndDepartmentIdAndAcademicYearAndYearName(
                department.getCollege().getId(), department.getId(), academicYear, request.yearName())) {
            throw new DuplicateResourceException("This Course Year already exists for the department and academic year");
        }
        String code = normalizeCode(request.code());
        if (courseYears.existsByCollegeIdAndDepartmentIdAndAcademicYearAndCodeIgnoreCase(
                department.getCollege().getId(), department.getId(), academicYear, code)) {
            throw new DuplicateResourceException("Course Year code already exists");
        }
        AcademicClass entity = new AcademicClass();
        entity.setCollege(department.getCollege());
        entity.setDepartment(department);
        entity.setAcademicYear(academicYear);
        entity.setYearName(request.yearName());
        entity.setName(request.displayName().trim());
        entity.setCode(code);
        entity.setStatus(AcademicStatus.ACTIVE);
        return mapper.toResponse(courseYears.save(entity));
    }

    @Override
    public PageResponse<CourseYearResponse> search(String keyword, Long departmentId,
            String academicYear, CourseYearName yearName, AcademicStatus status, int page, int size,
            String sortBy, String sortDir) {
        validatePage(page, size);
        Long collegeId = currentCollege();
        Specification<AcademicClass> spec = (root, query, cb) ->
                cb.equal(root.get("college").get("id"), collegeId);
        if (departmentId != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId));
        if (academicYear != null && !academicYear.isBlank()) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academicYear"), academicYear.trim()));
        if (yearName != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("yearName"), yearName));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("department").get("name")), pattern)));
        }
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = courseYears.findAll(spec, PageRequest.of(page, size, Sort.by(direction, safeSort)));
        List<Long> ids = result.getContent().stream().map(AcademicClass::getId).toList();
        Map<Long, Long> divisionCounts = new HashMap<>();
        if (!ids.isEmpty()) {
            mapperDivisionCounts(ids).forEach(row -> divisionCounts.put((Long) row[0], (Long) row[1]));
        }
        return PageResponse.from(result.map(entity ->
                mapper.toResponse(entity, divisionCounts.getOrDefault(entity.getId(), 0L))));
    }

    @Override
    public CourseYearResponse get(Long id) {
        return mapper.toResponse(findScoped(id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCourseYears", allEntries = true)
    public CourseYearResponse update(Long id, UpdateCourseYearRequest request) {
        requirePrincipal();
        AcademicClass entity = findScoped(id);
        String code = normalizeCode(request.code());
        if (!entity.getCode().equalsIgnoreCase(code)
                && courseYears.existsByCollegeIdAndDepartmentIdAndAcademicYearAndCodeIgnoreCase(
                entity.getCollege().getId(), entity.getDepartment().getId(), entity.getAcademicYear(), code)) {
            throw new DuplicateResourceException("Course Year code already exists");
        }
        entity.setName(request.displayName().trim());
        entity.setCode(code);
        return mapper.toResponse(courseYears.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCourseYears", allEntries = true)
    public CourseYearResponse setStatus(Long id, AcademicStatus status) {
        requirePrincipal();
        AcademicClass entity = findScoped(id);
        if (status == AcademicStatus.ACTIVE && entity.getDepartment().getStatus() != DepartmentStatus.ACTIVE) {
            throw new BadRequestException("Cannot activate Course Year in an inactive department");
        }
        entity.setStatus(status);
        return mapper.toResponse(courseYears.save(entity));
    }

    private AcademicClass findScoped(Long id) {
        AcademicClass entity = courseYears.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course Year not found"));
        scope(entity.getCollege().getId());
        return entity;
    }

    private void requirePrincipal() {
        if (!SecurityUtils.isPrincipal()) throw new AccessDeniedException("Only Principal can modify Course Years");
    }

    private Long currentCollege() {
        Long collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (collegeId == null) throw new AccessDeniedException("College access required");
        return collegeId;
    }

    private void scope(Long collegeId) {
        if (!collegeId.equals(currentCollege())) throw new AccessDeniedException("Resource is outside Principal college");
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private List<Object[]> mapperDivisionCounts(List<Long> ids) {
        return mapper.countDivisionsByCourseYearIds(ids);
    }
}
