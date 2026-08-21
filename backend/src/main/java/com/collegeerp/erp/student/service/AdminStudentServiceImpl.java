package com.collegeerp.erp.student.service;

import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.student.dto.StudentProfileResponse;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.enums.StudentStatus;
import com.collegeerp.erp.student.mapper.StudentProfileMapper;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AdminStudentServiceImpl implements AdminStudentService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "admissionNumber", "fullName", "email", "status", "createdAt", "updatedAt");

    private final StudentProfileRepository studentProfiles;
    private final StudentProfileMapper mapper;

    public AdminStudentServiceImpl(
            StudentProfileRepository studentProfiles,
            StudentProfileMapper mapper) {
        this.studentProfiles = studentProfiles;
        this.mapper = mapper;
    }

    @Override
    public StudentProfileResponse getStudentById(Long id) {
        StudentProfile student = studentProfiles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        ensureVisible(student);
        return mapper.toResponse(student);
    }

    @Override
    public PageResponse<StudentProfileResponse> searchStudents(
            String keyword,
            Long collegeId,
            Long departmentId,
            StudentStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (page < 0) throw new BadRequestException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new BadRequestException("Page size must be between 1 and 100");
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            direction = Sort.Direction.DESC;
        }

        Long scopedCollegeId = scopedCollegeId(collegeId);
        Specification<StudentProfile> spec = Specification.where(null);
        if (scopedCollegeId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("college").get("id"), scopedCollegeId));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("admissionNumber")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("college").get("name")), pattern),
                    cb.like(cb.lower(root.get("college").get("code")), pattern),
                    cb.like(cb.lower(root.get("department").get("name")), pattern),
                    cb.like(cb.lower(root.get("department").get("code")), pattern)
            ));
        }

        return PageResponse.from(studentProfiles.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(direction, safeSort))
        ).map(mapper::toResponse));
    }

    private Long scopedCollegeId(Long requestedCollegeId) {
        if (SecurityUtils.isSuperAdmin()) return requestedCollegeId;
        Long currentCollegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (currentCollegeId == null) {
            throw new AccessDeniedException("Principal college is required");
        }
        if (requestedCollegeId != null && !requestedCollegeId.equals(currentCollegeId)) {
            throw new AccessDeniedException("Students are outside your college");
        }
        return currentCollegeId;
    }

    private void ensureVisible(StudentProfile student) {
        if (SecurityUtils.isSuperAdmin()) return;
        Long currentCollegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (currentCollegeId == null || !currentCollegeId.equals(student.getCollege().getId())) {
            throw new AccessDeniedException("Student is outside your college");
        }
    }
}
