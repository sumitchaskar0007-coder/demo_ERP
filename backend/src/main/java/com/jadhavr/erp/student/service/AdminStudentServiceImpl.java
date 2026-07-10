package com.jadhavr.erp.student.service;

import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.student.dto.StudentProfileResponse;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.mapper.StudentProfileMapper;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Specification<StudentProfile> spec = Specification.where(null);
        if (collegeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("college").get("id"), collegeId));
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
}
