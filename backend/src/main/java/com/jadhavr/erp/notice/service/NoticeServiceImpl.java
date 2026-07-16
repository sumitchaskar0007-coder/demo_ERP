package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
import com.jadhavr.erp.notice.dto.NoticeResponse;
import com.jadhavr.erp.notice.entity.Notice;
import com.jadhavr.erp.notice.repository.NoticeRepository;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@Service
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {
    private static final Set<RoleName> SUPER_ADMIN_TARGETS = EnumSet.of(RoleName.PRINCIPAL, RoleName.HOD,
            RoleName.STUDENT_SECTION, RoleName.FEE_SECTION, RoleName.CLASS_TEACHER,
            RoleName.SUBJECT_TEACHER, RoleName.STUDENT);
    private static final Set<RoleName> PRINCIPAL_TARGETS = EnumSet.of(RoleName.HOD, RoleName.STUDENT_SECTION,
            RoleName.FEE_SECTION, RoleName.CLASS_TEACHER, RoleName.SUBJECT_TEACHER, RoleName.STUDENT);
    private final NoticeRepository notices;
    private final UserRepository users;
    private final CollegeRepository colleges;
    private final StaffProfileRepository staffProfiles;
    private final StudentProfileRepository studentProfiles;

    public NoticeServiceImpl(NoticeRepository notices, UserRepository users, CollegeRepository colleges,
                             StaffProfileRepository staffProfiles, StudentProfileRepository studentProfiles) {
        this.notices = notices; this.users = users; this.colleges = colleges;
        this.staffProfiles = staffProfiles; this.studentProfiles = studentProfiles;
    }

    @Override @Transactional
    public NoticeResponse create(CreateNoticeRequest request) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        User sender = users.findById(current.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<RoleName> targets = EnumSet.copyOf(request.audienceRoles());
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setMessage(request.message().trim());
        notice.setCreatedBy(sender);
        if (SecurityUtils.isSuperAdmin()) {
            ensureAllowed(targets, SUPER_ADMIN_TARGETS);
            if (request.collegeIds() != null) {
                Set<College> selected = new HashSet<>();
                request.collegeIds().forEach(id -> selected.add(findCollege(id)));
                notice.setColleges(selected);
            }
        } else if (SecurityUtils.isPrincipal()) {
            ensureAllowed(targets, PRINCIPAL_TARGETS);
            notice.setColleges(Set.of(requireOwnCollege(current)));
        } else if (SecurityUtils.hasRole("HOD")) {
            if (!targets.equals(Set.of(RoleName.STUDENT)))
                throw new AccessDeniedException("HOD can send notices only to students");
            StaffProfile profile = staffProfiles.findByUserId(current.getId())
                    .orElseThrow(() -> new BadRequestException("HOD staff profile not found"));
            if (profile.getDepartment() == null) throw new BadRequestException("HOD has no department assigned");
            notice.setColleges(Set.of(profile.getCollege()));
            notice.setDepartment(profile.getDepartment());
        } else {
            throw new AccessDeniedException("Your role cannot send notices");
        }
        notice.setAudienceRoles(targets);
        return map(notices.save(notice));
    }

    @Override
    public List<NoticeResponse> inbox() {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        Set<RoleName> roles = resolveRoles(current);
        Long departmentId = currentDepartmentId(current, roles);
        if (roles.isEmpty()) return List.of();
        return notices.findInbox(current.getId(), current.getCollegeId(), departmentId, roles, PageRequest.of(0, 100))
                .stream().map(this::map).toList();
    }

    @Override
    public List<NoticeResponse> sent() {
        Long id = SecurityUtils.getCurrentUserId();
        return notices.findByCreatedByIdOrderByCreatedAtDesc(id, PageRequest.of(0, 100))
                .stream().map(this::map).toList();
    }

    private Set<RoleName> resolveRoles(CustomUserDetails current) {
        return current.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> RoleName.valueOf(authority.substring("ROLE_".length())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleName.class)));
    }

    private Long currentDepartmentId(CustomUserDetails current, Set<RoleName> roles) {
        if (roles.contains(RoleName.STUDENT)) return studentProfiles.findByUserId(current.getId())
                .map(p -> p.getDepartment().getId()).orElse(null);
        return staffProfiles.findByUserId(current.getId())
                .map(p -> p.getDepartment() == null ? null : p.getDepartment().getId()).orElse(null);
    }
    private void ensureAllowed(Set<RoleName> requested, Set<RoleName> allowed) {
        if (!allowed.containsAll(requested)) throw new AccessDeniedException("One or more audience roles are not allowed");
    }
    private College findCollege(Long id) { return colleges.findById(id).orElseThrow(() -> new ResourceNotFoundException("College not found")); }
    private College requireOwnCollege(CustomUserDetails user) {
        if (user.getCollegeId() == null) throw new BadRequestException("User has no college assigned");
        return findCollege(user.getCollegeId());
    }
    private NoticeResponse map(Notice n) {
        return new NoticeResponse(n.getId(), n.getTitle(), n.getMessage(), n.getCreatedBy().getId(),
                n.getCreatedBy().getFullName(),
                n.getColleges().stream().map(College::getId).collect(Collectors.toSet()),
                n.getColleges().stream().map(College::getName).collect(Collectors.toSet()),
                n.getDepartment() == null ? null : n.getDepartment().getId(),
                n.getDepartment() == null ? null : n.getDepartment().getName(), Set.copyOf(n.getAudienceRoles()), n.getCreatedAt());
    }
}
