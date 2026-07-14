package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.AssignClassTeacherRequest;
import com.jadhavr.erp.academic.dto.CreateDivisionRequest;
import com.jadhavr.erp.academic.dto.DivisionResponse;
import com.jadhavr.erp.academic.dto.UpdateDivisionRequest;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.SectionStatus;
import com.jadhavr.erp.academic.mapper.DivisionMapper;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.audit.enums.AuditAction;
import com.jadhavr.erp.audit.enums.AuditModule;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.mapper.StaffMapper;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DivisionServiceImpl implements DivisionService {
    private static final Set<StaffType> CLASS_TEACHER_TYPES = Set.of(
            StaffType.TEACHER, StaffType.SUBJECT_TEACHER, StaffType.CLASS_TEACHER);
    private final SectionRepository divisions;
    private final AcademicClassRepository courseYears;
    private final StaffProfileRepository staffProfiles;
    private final RoleRepository roles;
    private final UserRepository users;
    private final DivisionMapper mapper;
    private final StaffMapper staffMapper;
    private AuditLogService auditLogs;

    @Autowired(required = false)
    public void setAuditLogs(AuditLogService auditLogs) {
        this.auditLogs = auditLogs;
    }

    public DivisionServiceImpl(SectionRepository divisions, AcademicClassRepository courseYears,
            StaffProfileRepository staffProfiles, RoleRepository roles, UserRepository users,
            DivisionMapper mapper, StaffMapper staffMapper) {
        this.divisions = divisions;
        this.courseYears = courseYears;
        this.staffProfiles = staffProfiles;
        this.roles = roles;
        this.users = users;
        this.mapper = mapper;
        this.staffMapper = staffMapper;
    }

    @Override
    @Transactional
    public DivisionResponse create(CreateDivisionRequest request) {
        requirePrincipal();
        AcademicClass courseYear = courseYears.findById(request.courseYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Course Year not found"));
        scope(courseYear.getCollege().getId());
        if (courseYear.getStatus() != AcademicStatus.ACTIVE) {
            throw new BadRequestException("Division requires an active Course Year");
        }
        String code = normalizeCode(request.code());
        if (divisions.existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(
                courseYear.getId(), courseYear.getAcademicYear(), code)) {
            throw new DuplicateResourceException("Division code already exists in this Course Year");
        }
        Section division = new Section();
        division.setCollege(courseYear.getCollege());
        division.setDepartment(courseYear.getDepartment());
        division.setAcademicClass(courseYear);
        division.setAcademicYear(courseYear.getAcademicYear());
        division.setName(request.name().trim());
        division.setCode(code);
        division.setCapacity(request.capacity());
        division.setStatus(SectionStatus.ACTIVE);
        return mapper.toResponse(divisions.save(division));
    }

    @Override
    public PageResponse<DivisionResponse> search(String keyword, Long departmentId,
            Long courseYearId, String academicYear, SectionStatus status, int page, int size) {
        validatePage(page, size);
        Long collegeId = currentCollege();
        Specification<Section> spec = (root, query, cb) -> cb.equal(root.get("college").get("id"), collegeId);
        if (departmentId != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId));
        if (courseYearId != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academicClass").get("id"), courseYearId));
        if (academicYear != null && !academicYear.isBlank()) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academicYear"), academicYear.trim()));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("academicClass").get("name")), pattern)));
        }
        return PageResponse.from(divisions.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(mapper::toResponse));
    }

    @Override
    public DivisionResponse get(Long id) {
        return mapper.toResponse(findScoped(id));
    }

    @Override
    @Transactional
    public DivisionResponse update(Long id, UpdateDivisionRequest request) {
        requirePrincipal();
        Section division = findScoped(id);
        String code = normalizeCode(request.code());
        if (!division.getCode().equalsIgnoreCase(code)
                && divisions.existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(
                division.getAcademicClass().getId(), division.getAcademicYear(), code)) {
            throw new DuplicateResourceException("Division code already exists in this Course Year");
        }
        division.setName(request.name().trim());
        division.setCode(code);
        division.setCapacity(request.capacity());
        return mapper.toResponse(divisions.save(division));
    }

    @Override
    @Transactional
    public DivisionResponse setStatus(Long id, SectionStatus status) {
        requirePrincipal();
        Section division = findScoped(id);
        if (status == SectionStatus.ACTIVE && division.getAcademicClass().getStatus() != AcademicStatus.ACTIVE) {
            throw new BadRequestException("Cannot activate Division in an inactive Course Year");
        }
        division.setStatus(status);
        return mapper.toResponse(divisions.save(division));
    }

    @Override
    @Transactional
    public DivisionResponse assignClassTeacher(Long id, AssignClassTeacherRequest request) {
        requirePrincipal();
        Section division = findScoped(id);
        if (division.getStatus() != SectionStatus.ACTIVE) throw new BadRequestException("Division is inactive");
        StaffProfile teacher = staffProfiles.findById(request.staffProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        validateTeacher(division, teacher);
        boolean alreadyHere = division.getClassTeacher() != null
                && division.getClassTeacher().getId().equals(teacher.getId());
        if (!alreadyHere && divisions.existsByClassTeacherIdAndAcademicYearAndStatus(
                teacher.getId(), division.getAcademicYear(), SectionStatus.ACTIVE)) {
            throw new DuplicateResourceException("Teacher already has an active Division in this academic year");
        }
        addClassTeacherRole(teacher);
        division.setClassTeacher(teacher);
        DivisionResponse response = mapper.toResponse(divisions.save(division));
        if (auditLogs != null) auditLogs.log(AuditModule.ACADEMIC, AuditAction.ASSIGN,
                "Division", division.getId(), "Assigned " + teacher.getFullName()
                        + " as Class Teacher for " + division.getName());
        return response;
    }

    @Override
    @Transactional
    public DivisionResponse removeClassTeacher(Long id) {
        requirePrincipal();
        Section division = findScoped(id);
        String teacherName = division.getClassTeacher() == null ? "Class Teacher"
                : division.getClassTeacher().getFullName();
        division.setClassTeacher(null);
        DivisionResponse response = mapper.toResponse(divisions.save(division));
        if (auditLogs != null) auditLogs.log(AuditModule.ACADEMIC, AuditAction.UPDATE,
                "Division", division.getId(), "Removed " + teacherName + " from " + division.getName());
        return response;
    }

    @Override
    public List<StaffResponse> eligibleClassTeachers(Long id) {
        Section division = findScoped(id);
        return staffProfiles.findByCollegeId(division.getCollege().getId()).stream()
                .filter(staff -> staff.getDepartment() != null
                        && staff.getDepartment().getId().equals(division.getDepartment().getId()))
                .filter(staff -> staff.getStatus() == StaffStatus.ACTIVE)
                .filter(staff -> CLASS_TEACHER_TYPES.contains(staff.getStaffType()))
                .map(staffMapper::toResponse)
                .toList();
    }

    private void validateTeacher(Section division, StaffProfile teacher) {
        if (teacher.getStatus() != StaffStatus.ACTIVE
                || !CLASS_TEACHER_TYPES.contains(teacher.getStaffType())
                || !teacher.getCollege().getId().equals(division.getCollege().getId())
                || teacher.getDepartment() == null
                || !teacher.getDepartment().getId().equals(division.getDepartment().getId())) {
            throw new BadRequestException("Class Teacher must be active and belong to the Division department");
        }
    }

    private void addClassTeacherRole(StaffProfile teacher) {
        boolean present = teacher.getUser().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.CLASS_TEACHER);
        if (present) return;
        Role role = roles.findByName(RoleName.CLASS_TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("CLASS_TEACHER role not found"));
        Set<Role> updated = new HashSet<>(teacher.getUser().getRoles());
        updated.add(role);
        teacher.getUser().setRoles(updated);
        users.save(teacher.getUser());
    }

    private Section findScoped(Long id) {
        Section division = divisions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Division not found"));
        scope(division.getCollege().getId());
        return division;
    }

    private void requirePrincipal() {
        if (!SecurityUtils.isPrincipal()) throw new AccessDeniedException("Only Principal can modify Divisions");
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
}
