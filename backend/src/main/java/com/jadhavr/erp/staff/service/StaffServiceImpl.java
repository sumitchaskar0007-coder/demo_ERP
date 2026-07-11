package com.jadhavr.erp.staff.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.staff.dto.CreateStudentSectionStaffRequest;
import com.jadhavr.erp.staff.dto.CreateFeeSectionStaffRequest;
import com.jadhavr.erp.email.service.EmailNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.dto.CreateAcademicStaffRequest;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.mapper.StaffMapper;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Year;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StaffServiceImpl implements StaffService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "employeeCode", "fullName", "email", "staffType", "status", "createdAt", "updatedAt");

    private final StaffProfileRepository staffProfiles;
    private final UserRepository users;
    private final RoleRepository roles;
    private final CollegeRepository colleges;
    private DepartmentRepository departments;
    private final PasswordEncoder passwordEncoder;
    private final StaffMapper mapper;
    private final SecureRandom random = new SecureRandom();
    private EmailNotificationService emailNotifications;

    @Autowired(required = false)
    public void setEmailNotifications(EmailNotificationService service) { this.emailNotifications = service; }

    public StaffServiceImpl(
            StaffProfileRepository staffProfiles,
            UserRepository users,
            RoleRepository roles,
            CollegeRepository colleges,
            PasswordEncoder passwordEncoder,
            StaffMapper mapper) {
        this.staffProfiles = staffProfiles;
        this.users = users;
        this.roles = roles;
        this.colleges = colleges;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Autowired
    public void setDepartments(DepartmentRepository departments) { this.departments = departments; }

    @Override @Transactional
    public StaffResponse createAcademicStaff(CreateAcademicStaffRequest request, StaffType type) {
        if (type != StaffType.HOD && type != StaffType.CLASS_TEACHER && type != StaffType.SUBJECT_TEACHER) throw new BadRequestException("Invalid academic staff type");
        Department department = departments.findById(request.departmentId()).orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!department.getCollege().getId().equals(request.collegeId()) || department.getStatus() != DepartmentStatus.ACTIVE) throw new BadRequestException("Department must be active and belong to the college");
        if (type == StaffType.HOD && staffProfiles.existsByDepartmentIdAndStaffTypeAndStatus(department.getId(), type, StaffStatus.ACTIVE)) throw new DuplicateResourceException("Department already has an active HOD");
        RoleName role = switch(type){case HOD -> RoleName.HOD; case CLASS_TEACHER -> RoleName.CLASS_TEACHER; default -> RoleName.SUBJECT_TEACHER;};
        StaffResponse response = createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(), request.joiningDate(), role, type);
        StaffProfile profile = staffProfiles.findById(response.id()).orElseThrow(); profile.setDepartment(department); staffProfiles.save(profile);
        return mapper.toResponse(profile);
    }

    @Override
    @Transactional
    public StaffResponse createStudentSectionStaff(CreateStudentSectionStaffRequest request) {
        return createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(), request.joiningDate(), RoleName.STUDENT_SECTION, StaffType.STUDENT_SECTION);
    }

    @Override
    @Transactional
    public StaffResponse createFeeSectionStaff(CreateFeeSectionStaffRequest request) {
        return createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(), request.joiningDate(), RoleName.FEE_SECTION, StaffType.FEE_SECTION);
    }

    private StaffResponse createStaff(Long collegeId, String fullName, String requestedEmail, String phone,
                                      java.time.LocalDate joiningDate, RoleName roleName, StaffType staffType) {
        CustomUserDetails currentUser = SecurityUtils.requireCurrentUser();
        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isPrincipal()) {
            throw new AccessDeniedException("Access denied");
        }
        if (SecurityUtils.isPrincipal() && !collegeId.equals(currentUser.getCollegeId())) {
            throw new AccessDeniedException("Principal can create staff only for own college");
        }

        College college = colleges.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
        if (college.getStatus() != CollegeStatus.ACTIVE) {
            throw new BadRequestException("Cannot create staff for an inactive college");
        }
        String email = normalizeEmail(requestedEmail);
        if (users.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists with this email");
        }
        Role role = roles.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        User user = new User();
        user.setCollege(college);
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPhone(phone.trim());
        user.setPasswordHash(passwordEncoder.encode(phone.trim()));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));
        User savedUser = users.save(user);
        if (emailNotifications != null) emailNotifications.queueUserCreatedEmail(savedUser);

        StaffProfile profile = new StaffProfile();
        profile.setUser(savedUser);
        profile.setCollege(college);
        profile.setDepartment(null);
        profile.setEmployeeCode(generateEmployeeCode(college.getCode()));
        profile.setFullName(fullName.trim());
        profile.setEmail(email);
        profile.setPhone(phone.trim());
        profile.setStaffType(staffType);
        profile.setStatus(StaffStatus.ACTIVE);
        profile.setJoiningDate(joiningDate);
        return mapper.toResponse(staffProfiles.save(profile));
    }

    @Override
    public StaffResponse getStaffById(Long id) {
        StaffProfile profile = findStaff(id);
        ensureStaffVisible(profile);
        return mapper.toResponse(profile);
    }

    @Override
    public PageResponse<StaffResponse> searchStaff(
            String keyword, Long collegeId, StaffType staffType, StaffStatus status,
            int page, int size, String sortBy, String sortDir) {
        validatePage(page, size);
        Long scopedCollegeId = scopedCollegeId(collegeId);
        Sort.Direction direction = directionOrDefault(sortDir);
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";

        Specification<StaffProfile> spec = Specification.where(null);
        if (scopedCollegeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("college").get("id"), scopedCollegeId));
        }
        if (staffType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("staffType"), staffType));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("employeeCode")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("college").get("name")), pattern),
                    cb.like(cb.lower(root.get("college").get("code")), pattern)
            ));
        }
        return PageResponse.from(staffProfiles.findAll(
                spec, PageRequest.of(page, size, Sort.by(direction, safeSort))).map(mapper::toResponse));
    }

    @Override
    @Transactional
    public StaffResponse activateStaff(Long id) {
        StaffProfile profile = findStaff(id);
        ensureStaffVisible(profile);
        if (profile.getCollege().getStatus() != CollegeStatus.ACTIVE) {
            throw new BadRequestException("Cannot activate staff for an inactive college");
        }
        profile.setStatus(StaffStatus.ACTIVE);
        profile.getUser().setStatus(UserStatus.ACTIVE);
        users.save(profile.getUser());
        return mapper.toResponse(staffProfiles.save(profile));
    }

    @Override
    @Transactional
    public StaffResponse deactivateStaff(Long id) {
        StaffProfile profile = findStaff(id);
        ensureStaffVisible(profile);
        profile.setStatus(StaffStatus.INACTIVE);
        profile.getUser().setStatus(UserStatus.INACTIVE);
        users.save(profile.getUser());
        return mapper.toResponse(staffProfiles.save(profile));
    }

    private StaffProfile findStaff(Long id) {
        return staffProfiles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
    }

    private void ensureStaffVisible(StaffProfile profile) {
        if (!SecurityUtils.isSuperAdmin()
                && !profile.getCollege().getId().equals(SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private Long scopedCollegeId(Long requestedCollegeId) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestedCollegeId;
        }
        Long currentCollegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (requestedCollegeId != null && !requestedCollegeId.equals(currentCollegeId)) {
            throw new AccessDeniedException("Access denied");
        }
        return currentCollegeId;
    }

    private String generateEmployeeCode(String collegeCode) {
        String year = String.valueOf(Year.now().getValue());
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "EMP-%s-%s-%06d".formatted(collegeCode, year, random.nextInt(1_000_000));
            if (!staffProfiles.existsByEmployeeCode(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Could not generate employee code");
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new BadRequestException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new BadRequestException("Page size must be between 1 and 100");
    }

    private Sort.Direction directionOrDefault(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            return Sort.Direction.DESC;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
