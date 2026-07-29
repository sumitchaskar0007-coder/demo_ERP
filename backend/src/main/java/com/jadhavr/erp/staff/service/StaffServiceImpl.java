package com.jadhavr.erp.staff.service;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.SectionStatus;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceRecord;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceSession;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceSessionRepository;
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
import com.jadhavr.erp.staff.dto.StaffDetailResponse;
import com.jadhavr.erp.staff.dto.CreateAcademicStaffRequest;
import com.jadhavr.erp.staff.dto.CreateStaffRequest;
import com.jadhavr.erp.staff.dto.UpdateStaffAssignmentRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StaffServiceImpl implements StaffService {
    private static final Set<StaffType> DEPARTMENT_REQUIRED_TYPES = Set.of(
            StaffType.HOD, StaffType.TEACHER, StaffType.CLASS_TEACHER, StaffType.SUBJECT_TEACHER);
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "employeeCode", "fullName", "email", "staffType", "status", "createdAt", "updatedAt");

    private final StaffProfileRepository staffProfiles;
    private final UserRepository users;
    private final RoleRepository roles;
    private final CollegeRepository colleges;
    private DepartmentRepository departments;
    private SectionRepository sections;
    private SubjectTeacherAssignmentRepository subjectAssignments;
    private WeeklyAttendanceSessionRepository attendanceSessions;
    private WeeklyAttendanceRecordRepository attendanceRecords;
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

    @Autowired
    public void setStaffDetailRepositories(
            SectionRepository sections,
            SubjectTeacherAssignmentRepository subjectAssignments,
            WeeklyAttendanceSessionRepository attendanceSessions,
            WeeklyAttendanceRecordRepository attendanceRecords) {
        this.sections = sections;
        this.subjectAssignments = subjectAssignments;
        this.attendanceSessions = attendanceSessions;
        this.attendanceRecords = attendanceRecords;
    }

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        if (!SecurityUtils.isPrincipal()) {
            throw new AccessDeniedException("Only Principal can create staff");
        }
        CustomUserDetails currentUser = SecurityUtils.requireCurrentUser();
        Long collegeId = currentUser.getCollegeId();
        if (collegeId == null) throw new AccessDeniedException("Principal college is required");

        Set<StaffType> staffTypes = requestedStaffTypes(request);
        if (staffTypes.contains(StaffType.CLASS_TEACHER)) {
            throw new BadRequestException(
                    "Create the staff member as Teacher; HOD assigns the Class Teacher role with a Division");
        }
        validateRoleCombination(staffTypes);
        Set<Department> assignedDepartments = resolveDepartments(request, staffTypes, collegeId);
        StaffType staffType = primaryStaffType(request.staffType(), staffTypes);
        Department department = assignedDepartments.stream().findFirst().orElse(null);
        if (staffTypes.contains(StaffType.HOD) && assignedDepartments.size() != 1) {
            throw new BadRequestException("HOD accounts must have exactly one department");
        }
        if (staffTypes.contains(StaffType.HOD) && staffProfiles.existsByDepartmentIdAndStaffTypeAndStatus(
                department.getId(), StaffType.HOD, StaffStatus.ACTIVE)) {
            throw new DuplicateResourceException("Department already has an active HOD");
        }

        Set<RoleName> roleNames = staffTypes.stream().map(this::roleFor).collect(java.util.stream.Collectors.toSet());
        StaffResponse created = createStaff(collegeId, request.fullName(), request.email(),
                request.phone(), request.phone().trim(), request.joiningDate(), roleNames, staffType, true);
        StaffProfile profile = staffProfiles.findById(created.id()).orElseThrow();
        profile.setDepartment(department);
        profile.setDepartments(assignedDepartments);
        return mapper.toResponse(staffProfiles.save(profile));
    }

    @Override @Transactional
    public StaffResponse createAcademicStaff(CreateAcademicStaffRequest request, StaffType type) {
        if (type == StaffType.CLASS_TEACHER) throw new BadRequestException("Create the staff member as Teacher; HOD assigns the Class Teacher role with a Division");
        if (type != StaffType.HOD && type != StaffType.SUBJECT_TEACHER) throw new BadRequestException("Invalid academic staff type");
        Department department = departments.findById(request.departmentId()).orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!department.getCollege().getId().equals(request.collegeId()) || department.getStatus() != DepartmentStatus.ACTIVE) throw new BadRequestException("Department must be active and belong to the college");
        if (type == StaffType.HOD && staffProfiles.existsByDepartmentIdAndStaffTypeAndStatus(department.getId(), type, StaffStatus.ACTIVE)) throw new DuplicateResourceException("Department already has an active HOD");
        RoleName role = switch(type){case HOD -> RoleName.HOD; case CLASS_TEACHER -> RoleName.CLASS_TEACHER; default -> RoleName.SUBJECT_TEACHER;};
        StaffResponse response = createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(),
                request.phone(), request.joiningDate(), Set.of(role), type, true);
        StaffProfile profile = staffProfiles.findById(response.id()).orElseThrow(); profile.setDepartment(department); profile.setDepartments(Set.of(department)); staffProfiles.save(profile);
        return mapper.toResponse(profile);
    }

    @Override
    @Transactional
    public StaffResponse createStudentSectionStaff(CreateStudentSectionStaffRequest request) {
        return createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(), request.phone(),
                request.joiningDate(), Set.of(RoleName.STUDENT_SECTION), StaffType.STUDENT_SECTION, true);
    }

    @Override
    @Transactional
    public StaffResponse createFeeSectionStaff(CreateFeeSectionStaffRequest request) {
        return createStaff(request.collegeId(), request.fullName(), request.email(), request.phone(), request.phone(),
                request.joiningDate(), Set.of(RoleName.FEE_SECTION), StaffType.FEE_SECTION, true);
    }

    private StaffResponse createStaff(Long collegeId, String fullName, String requestedEmail, String phone,
                                      String rawPassword, java.time.LocalDate joiningDate,
                                       Set<RoleName> roleNames, StaffType staffType, boolean mustChangePassword) {
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
        Set<Role> assignedRoles = roleNames.stream().map(roleName -> roles.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(java.util.stream.Collectors.toSet());

        User user = new User();
        user.setCollege(college);
        user.setFullName(fullName.trim());
        user.setEmail(email);
        String normalizedPhone = trimToNull(phone);
        user.setPhone(normalizedPhone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setMustChangePassword(mustChangePassword);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(assignedRoles);
        User savedUser = users.save(user);
        if (emailNotifications != null) {
            emailNotifications.queueUserCreatedEmail(savedUser, rawPassword);
        }

        StaffProfile profile = new StaffProfile();
        profile.setUser(savedUser);
        profile.setCollege(college);
        profile.setDepartment(null);
        profile.setEmployeeCode(generateEmployeeCode(college.getCode()));
        profile.setFullName(fullName.trim());
        profile.setEmail(email);
        profile.setPhone(normalizedPhone);
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
    @Transactional
    public StaffResponse updateStaffAssignment(Long id, UpdateStaffAssignmentRequest request) {
        if (!SecurityUtils.isPrincipal()) {
            throw new AccessDeniedException("Only Principal can edit staff assignments");
        }
        StaffProfile profile = findStaff(id);
        ensureStaffVisible(profile);

        Set<StaffType> staffTypes = new LinkedHashSet<>(request.staffTypes());
        if (staffTypes.contains(StaffType.CLASS_TEACHER)) {
            throw new BadRequestException(
                    "Class Teacher is managed by the HOD through Division assignment");
        }
        validateRoleCombination(staffTypes);
        Set<Department> assignedDepartments = resolveDepartments(
                request.departmentIds(), staffTypes, profile.getCollege().getId());
        Department primaryDepartment = assignedDepartments.stream().findFirst().orElse(null);

        if (staffTypes.contains(StaffType.HOD)) {
            if (assignedDepartments.size() != 1) {
                throw new BadRequestException("HOD accounts must have exactly one department");
            }
            if (profile.getStatus() == StaffStatus.ACTIVE
                    && staffProfiles.existsByDepartmentIdAndStaffTypeAndStatusAndIdNot(
                    primaryDepartment.getId(), StaffType.HOD, StaffStatus.ACTIVE, profile.getId())) {
                throw new DuplicateResourceException("Department already has an active HOD");
            }
        }

        List<Section> classAssignments =
                sections.findByClassTeacherIdAndStatus(profile.getId(), SectionStatus.ACTIVE);
        var teachingAssignments =
                subjectAssignments.findByTeacherIdAndStatus(profile.getId(), AcademicStatus.ACTIVE);
        boolean teachingRole = staffTypes.stream().anyMatch(DEPARTMENT_REQUIRED_TYPES::contains);
        if (!teachingRole && (!classAssignments.isEmpty() || !teachingAssignments.isEmpty())) {
            throw new BadRequestException(
                    "Remove active class and subject assignments before changing to an operational role");
        }
        Set<Long> assignedDepartmentIds = assignedDepartments.stream()
                .map(Department::getId)
                .collect(Collectors.toSet());
        boolean classOutsideScope = classAssignments.stream()
                .anyMatch(section -> !assignedDepartmentIds.contains(section.getDepartment().getId()));
        boolean subjectOutsideScope = teachingAssignments.stream()
                .anyMatch(assignment -> !assignedDepartmentIds.contains(
                        assignment.getSubject().getDepartment().getId()));
        if (classOutsideScope || subjectOutsideScope) {
            throw new BadRequestException(
                    "A selected department cannot be removed while it has active teaching assignments");
        }

        Set<RoleName> roleNames = staffTypes.stream()
                .map(this::roleFor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!classAssignments.isEmpty()) roleNames.add(RoleName.CLASS_TEACHER);
        Set<Role> assignedRoles = roleNames.stream()
                .map(roleName -> roles.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        User user = profile.getUser();
        user.setRoles(assignedRoles);
        user.setSessionVersion(user.getSessionVersion() + 1);
        users.save(user);

        profile.setStaffType(primaryStaffType(null, staffTypes));
        profile.setDepartment(null);
        profile.getDepartments().clear();
        profile.setDepartments(assignedDepartments);
        profile.setDepartment(primaryDepartment);
        return mapper.toResponse(staffProfiles.save(profile));
    }

    @Override
    public StaffDetailResponse getStaffDetails(Long id) {
        StaffProfile profile = findStaff(id);
        ensureStaffVisible(profile);

        List<StaffDetailResponse.ClassAssignment> classAssignments =
                sections.findByClassTeacherIdAndStatus(id, SectionStatus.ACTIVE).stream()
                        .map(section -> new StaffDetailResponse.ClassAssignment(
                                section.getId(),
                                section.getDepartment().getName(),
                                section.getAcademicClass().getName(),
                                section.getName(),
                                section.getCode(),
                                section.getAcademicYear(),
                                section.getCapacity()))
                        .toList();

        List<StaffDetailResponse.SubjectAssignment> teachingAssignments =
                subjectAssignments.findByTeacherIdAndStatus(id, AcademicStatus.ACTIVE).stream()
                        .map(assignment -> new StaffDetailResponse.SubjectAssignment(
                                assignment.getSubject().getId(),
                                assignment.getSubject().getCode(),
                                assignment.getSubject().getName(),
                                assignment.getSubject().getAcademicClass().getName(),
                                assignment.getAcademicYear(),
                                assignment.getSections().stream()
                                        .map(Section::getName)
                                        .sorted()
                                        .toList()))
                        .toList();

        long totalSessions = attendanceSessions.countByTeacherId(id);
        long submittedSessions = attendanceSessions.countByTeacherIdAndStatus(
                id, WeeklyAttendanceSession.Status.SUBMITTED);
        StaffDetailResponse.AttendanceSummary summary = new StaffDetailResponse.AttendanceSummary(
                totalSessions,
                submittedSessions,
                totalSessions - submittedSessions,
                attendanceRecords.countBySessionTeacherId(id),
                attendanceRecords.countBySessionTeacherIdAndStatus(
                        id, WeeklyAttendanceRecord.Status.PRESENT),
                attendanceRecords.countBySessionTeacherIdAndStatus(
                        id, WeeklyAttendanceRecord.Status.ABSENT),
                attendanceRecords.countBySessionTeacherIdAndStatus(
                        id, WeeklyAttendanceRecord.Status.LATE),
                attendanceRecords.countBySessionTeacherIdAndStatus(
                        id, WeeklyAttendanceRecord.Status.LEAVE));

        List<WeeklyAttendanceSession> recentSessions =
                attendanceSessions.findTop20ByTeacherIdOrderByAttendanceDateDescStartTimeDesc(id);
        Map<Long, List<WeeklyAttendanceRecord>> recordsBySession = recentSessions.isEmpty()
                ? Map.of()
                : attendanceRecords.findBySessionIdIn(
                                recentSessions.stream().map(WeeklyAttendanceSession::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(record -> record.getSession().getId()));
        List<StaffDetailResponse.AttendanceSessionItem> recentAttendance = recentSessions.stream()
                .map(session -> attendanceSessionItem(
                        session, recordsBySession.getOrDefault(session.getId(), List.of())))
                .toList();

        return new StaffDetailResponse(
                mapper.toResponse(profile),
                classAssignments,
                teachingAssignments,
                summary,
                recentAttendance);
    }

    private StaffDetailResponse.AttendanceSessionItem attendanceSessionItem(
            WeeklyAttendanceSession session, List<WeeklyAttendanceRecord> records) {
        Map<WeeklyAttendanceRecord.Status, Long> totals = records.stream()
                .collect(Collectors.groupingBy(
                        WeeklyAttendanceRecord::getStatus, Collectors.counting()));
        return new StaffDetailResponse.AttendanceSessionItem(
                session.getId(),
                session.getAttendanceDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getLectureNumber(),
                session.getSubject().getName(),
                session.getSection().getAcademicClass().getName(),
                session.getSection().getName(),
                session.getStatus().name(),
                records.size(),
                totals.getOrDefault(WeeklyAttendanceRecord.Status.PRESENT, 0L),
                totals.getOrDefault(WeeklyAttendanceRecord.Status.ABSENT, 0L),
                totals.getOrDefault(WeeklyAttendanceRecord.Status.LATE, 0L),
                totals.getOrDefault(WeeklyAttendanceRecord.Status.LEAVE, 0L));
    }

    @Override
    public PageResponse<StaffResponse> searchStaff(
            String keyword, Long collegeId, Long departmentId, StaffType staffType, StaffStatus status,
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
            RoleName requestedRole = roleFor(staffType);
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var assignedRole = root.join("user").join("roles");
                return cb.or(cb.equal(root.get("staffType"), staffType),
                        cb.equal(assignedRole.get("name"), requestedRole));
            });
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var assigned = root.join("departments", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(cb.equal(root.get("department").get("id"), departmentId),
                        cb.equal(assigned.get("id"), departmentId));
            });
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
        if (profile.getStaffType() == StaffType.HOD && profile.getDepartment() != null
                && staffProfiles.existsByDepartmentIdAndStaffTypeAndStatusAndIdNot(
                profile.getDepartment().getId(), StaffType.HOD, StaffStatus.ACTIVE, profile.getId())) {
            throw new DuplicateResourceException("Department already has an active HOD");
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

    private Set<StaffType> requestedStaffTypes(CreateStaffRequest request) {
        Set<StaffType> requested = new LinkedHashSet<>();
        if (request.staffTypes() != null) requested.addAll(request.staffTypes());
        if (requested.isEmpty() && request.staffType() != null) requested.add(request.staffType());
        if (requested.isEmpty()) throw new BadRequestException("Select at least one staff role");
        return requested;
    }

    private void validateRoleCombination(Set<StaffType> staffTypes) {
        if (staffTypes.contains(StaffType.HOD) && staffTypes.size() > 1) {
            throw new BadRequestException("HOD must be selected as a standalone role");
        }
        boolean teaching = staffTypes.stream().anyMatch(DEPARTMENT_REQUIRED_TYPES::contains);
        boolean operational = staffTypes.stream().anyMatch(type -> !DEPARTMENT_REQUIRED_TYPES.contains(type));
        if (teaching && operational) {
            throw new BadRequestException("Teaching roles cannot be combined with operational staff roles");
        }
        if (operational && staffTypes.size() > 1) {
            throw new BadRequestException("Select only one operational staff role");
        }
    }

    private StaffType primaryStaffType(StaffType requestedPrimary, Set<StaffType> staffTypes) {
        if (requestedPrimary != null && staffTypes.contains(requestedPrimary)) return requestedPrimary;
        for (StaffType preferred : List.of(StaffType.CLASS_TEACHER, StaffType.HOD,
                StaffType.SUBJECT_TEACHER, StaffType.TEACHER)) {
            if (staffTypes.contains(preferred)) return preferred;
        }
        return staffTypes.iterator().next();
    }

    private RoleName roleFor(StaffType staffType) {
        return switch (staffType) {
            case STUDENT_SECTION -> RoleName.STUDENT_SECTION;
            case FEE_SECTION -> RoleName.FEE_SECTION;
            case HOD -> RoleName.HOD;
            case TEACHER, SUBJECT_TEACHER -> RoleName.SUBJECT_TEACHER;
            case CLASS_TEACHER -> RoleName.CLASS_TEACHER;
            case GENERAL_STAFF -> RoleName.GENERAL_STAFF;
        };
    }

    private Set<Department> resolveDepartments(CreateStaffRequest request, Set<StaffType> staffTypes,
                                               Long collegeId) {
        Set<Long> requestedIds = new LinkedHashSet<>();
        if (request.departmentId() != null) requestedIds.add(request.departmentId());
        if (request.departmentIds() != null) requestedIds.addAll(request.departmentIds());
        return resolveDepartments(requestedIds, staffTypes, collegeId);
    }

    private Set<Department> resolveDepartments(Set<Long> departmentIds, Set<StaffType> staffTypes,
                                               Long collegeId) {
        Set<Long> requestedIds =
                departmentIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(departmentIds);
        if (staffTypes.stream().anyMatch(DEPARTMENT_REQUIRED_TYPES::contains) && requestedIds.isEmpty()) {
            throw new BadRequestException("Select at least one department for teaching staff");
        }
        Set<Department> result = new LinkedHashSet<>();
        for (Long departmentId : requestedIds) {
            Department department = departments.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
            if (!department.getCollege().getId().equals(collegeId)) {
                throw new AccessDeniedException("Department is outside Principal college");
            }
            if (department.getStatus() != DepartmentStatus.ACTIVE) {
                throw new BadRequestException("Department must be active: " + department.getName());
            }
            result.add(department);
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
