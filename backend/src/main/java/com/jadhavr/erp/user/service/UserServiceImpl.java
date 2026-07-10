package com.jadhavr.erp.user.service;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.user.dto.CreatePrincipalRequest;
import com.jadhavr.erp.user.dto.UserResponse;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "fullName", "email", "status", "lastLoginAt", "createdAt", "updatedAt");

    private final UserRepository users;
    private final RoleRepository roles;
    private final CollegeRepository colleges;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public UserServiceImpl(UserRepository users, RoleRepository roles,
                           CollegeRepository colleges, PasswordEncoder passwordEncoder,
                           UserMapper mapper) {
        this.users = users;
        this.roles = roles;
        this.colleges = colleges;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserResponse createPrincipal(CreatePrincipalRequest request) {
        College college = colleges.findById(request.collegeId()).orElseThrow(() ->
                new ResourceNotFoundException("College not found with id: " + request.collegeId()));
        if (college.getStatus() == CollegeStatus.INACTIVE) {
            throw new BadRequestException("Cannot create principal for an inactive college");
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists with email: " + email);
        }
        if (users.existsByCollegeIdAndRolesNameAndStatus(
                college.getId(), RoleName.PRINCIPAL, UserStatus.ACTIVE)) {
            throw new DuplicateResourceException("Active principal already exists for this college");
        }
        Role principalRole = roles.findByName(RoleName.PRINCIPAL).orElseThrow(() ->
                new ResourceNotFoundException("Role not found: PRINCIPAL"));
        User user = new User();
        user.setCollege(college);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.phone().trim()));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(principalRole));
        return mapper.toResponse(users.save(user));
    }

    @Override
    public java.util.List<UserResponse> getUsersByCollege(Long collegeId) {
        if (!colleges.existsById(collegeId)) {
            throw new ResourceNotFoundException("College not found with id: " + collegeId);
        }
        return users.findByCollegeId(collegeId).stream().map(mapper::toResponse).toList();
    }

    @Override
    public UserResponse getUserById(Long id) { return mapper.toResponse(findUser(id)); }

    @Override
    @Transactional
    public UserResponse activateUser(Long id) {
        User user = findUser(id);
        if (hasRole(user, RoleName.PRINCIPAL)) {
            if (user.getCollege() == null || user.getCollege().getStatus() == CollegeStatus.INACTIVE) {
                throw new BadRequestException("Cannot activate principal for an inactive college");
            }
            if (users.existsByCollegeIdAndRolesNameAndStatus(
                    user.getCollege().getId(), RoleName.PRINCIPAL, UserStatus.ACTIVE)) {
                throw new DuplicateResourceException("Active principal already exists for this college");
            }
        }
        user.setStatus(UserStatus.ACTIVE);
        return mapper.toResponse(users.save(user));
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = findUser(id);
        user.setStatus(UserStatus.INACTIVE);
        return mapper.toResponse(users.save(user));
    }

    @Override
    public PageResponse<UserResponse> searchUsers(
            String keyword, Long collegeId, RoleName role, UserStatus status,
            int page, int size, String sortBy, String sortDir) {
        if (page < 0) throw new BadRequestException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new BadRequestException("Page size must be between 1 and 100");
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction;
        try { direction = Sort.Direction.fromString(sortDir); }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Sort direction must be asc or desc");
        }
        Specification<User> spec = Specification.where(null);
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("college").get("name")), pattern),
                    cb.like(cb.lower(root.get("college").get("code")), pattern)));
        }
        if (collegeId != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("college").get("id"), collegeId));
        if (status != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), status));
        if (role != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.join("roles").get("name"), role));
        return PageResponse.from(users.findAll(
                spec, PageRequest.of(page, size, Sort.by(direction, safeSort))).map(mapper::toResponse));
    }

    private User findUser(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + id));
    }
    private boolean hasRole(User user, RoleName role) {
        return user.getRoles().stream().anyMatch(item -> item.getName() == role);
    }
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
