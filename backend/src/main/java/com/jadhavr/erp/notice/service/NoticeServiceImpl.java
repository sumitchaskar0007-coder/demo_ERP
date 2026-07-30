package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
import com.jadhavr.erp.notice.dto.NoticeDeliveryMode;
import com.jadhavr.erp.notice.dto.NoticeRecipientOption;
import com.jadhavr.erp.notice.dto.NoticeReceiptResponse;
import com.jadhavr.erp.notice.dto.NoticeResponse;
import com.jadhavr.erp.notice.entity.Notice;
import com.jadhavr.erp.notice.entity.NoticeAcknowledgement;
import com.jadhavr.erp.notice.entity.NoticePriority;
import com.jadhavr.erp.notice.entity.NoticeView;
import com.jadhavr.erp.notice.repository.NoticeAcknowledgementRepository;
import com.jadhavr.erp.notice.repository.NoticeRepository;
import com.jadhavr.erp.notice.repository.NoticeViewRepository;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {
    private static final Set<RoleName> SUPER_ADMIN_TARGETS = EnumSet.allOf(RoleName.class);
    private static final Set<RoleName> PRINCIPAL_TARGETS = EnumSet.of(RoleName.HOD,
            RoleName.STUDENT_SECTION, RoleName.FEE_SECTION, RoleName.CLASS_TEACHER,
            RoleName.SUBJECT_TEACHER, RoleName.GENERAL_STAFF, RoleName.STUDENT);
    private static final Set<RoleName> HOD_TARGETS = EnumSet.of(
            RoleName.STUDENT_SECTION, RoleName.FEE_SECTION, RoleName.CLASS_TEACHER,
            RoleName.SUBJECT_TEACHER, RoleName.GENERAL_STAFF, RoleName.STUDENT);
    private final NoticeRepository notices;
    private final UserRepository users;
    private final CollegeRepository colleges;
    private final DepartmentRepository departments;
    private final StaffProfileRepository staffProfiles;
    private final StudentProfileRepository studentProfiles;
    private final NoticeAcknowledgementRepository acknowledgements;
    private final NoticeViewRepository views;
    private final NoticeStreamService streams;

    public NoticeServiceImpl(NoticeRepository notices, UserRepository users, CollegeRepository colleges,
                             DepartmentRepository departments,
                             StaffProfileRepository staffProfiles, StudentProfileRepository studentProfiles,
                             NoticeAcknowledgementRepository acknowledgements,
                             NoticeViewRepository views,
                             NoticeStreamService streams) {
        this.notices = notices; this.users = users; this.colleges = colleges;
        this.departments = departments;
        this.staffProfiles = staffProfiles; this.studentProfiles = studentProfiles;
        this.acknowledgements = acknowledgements;
        this.views = views;
        this.streams = streams;
    }

    @Override @Transactional
    public NoticeResponse create(CreateNoticeRequest request) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        User sender = users.findById(current.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        NoticeDeliveryMode mode = request.deliveryMode() == null
                ? NoticeDeliveryMode.COMMON : request.deliveryMode();
        Set<RoleName> requestedRoles = request.audienceRoles() == null
                ? Set.of() : request.audienceRoles();
        Set<RoleName> targets = requestedRoles.isEmpty()
                ? EnumSet.noneOf(RoleName.class) : EnumSet.copyOf(requestedRoles);
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setMessage(request.message().trim());
        notice.setPriority(request.priority());
        notice.setCreatedBy(sender);
        notice.setDeliveryMode(mode);
        if (isSystemAdmin()) {
            if (mode == NoticeDeliveryMode.COMMON) ensureAllowedAndNotEmpty(targets, SUPER_ADMIN_TARGETS);
            if (request.collegeIds() != null) {
                Set<College> selected = new HashSet<>();
                request.collegeIds().forEach(id -> selected.add(findCollege(id)));
                notice.setColleges(selected);
            }
        } else if (SecurityUtils.isPrincipal()) {
            if (mode == NoticeDeliveryMode.COMMON) ensureAllowedAndNotEmpty(targets, HOD_TARGETS);
            notice.setColleges(Set.of(requireOwnCollege(current)));
        } else if (SecurityUtils.hasRole("HOD")) {
            StaffProfile profile = staffProfiles.findByUserId(current.getId())
                    .orElseThrow(() -> new BadRequestException("HOD staff profile not found"));
            if (profile.getDepartment() == null) throw new BadRequestException("HOD has no department assigned");
            notice.setColleges(Set.of(profile.getCollege()));
            notice.setDepartment(profile.getDepartment());
            if (mode == NoticeDeliveryMode.COMMON) ensureAllowedAndNotEmpty(targets, PRINCIPAL_TARGETS);
        } else {
            throw new AccessDeniedException("Your role cannot send notices");
        }
        if (request.departmentId() != null && !SecurityUtils.hasRole("HOD")) {
            Department department = departments.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            ensureDepartmentWithinScope(department, current, request.collegeIds());
            notice.setDepartment(department);
            if (notice.getColleges().isEmpty()) notice.setColleges(Set.of(department.getCollege()));
        }
        if (mode == NoticeDeliveryMode.INDIVIDUAL) {
            if (request.recipientUserIds() == null || request.recipientUserIds().isEmpty())
                throw new BadRequestException("Select at least one notice recipient");
            Set<User> selected = new HashSet<>(users.findAllById(request.recipientUserIds()));
            if (selected.size() != request.recipientUserIds().size())
                throw new BadRequestException("One or more selected users do not exist");
            Set<RoleName> allowedTargets = SecurityUtils.isPrincipal()
                    ? PRINCIPAL_TARGETS : SecurityUtils.hasRole("HOD") ? HOD_TARGETS : SUPER_ADMIN_TARGETS;
            selected.forEach(user -> {
                ensureRecipientWithinScope(user, current);
                ensureRecipientHasAllowedRole(user, allowedTargets);
            });
            notice.setRecipients(selected);
            targets = selected.stream().flatMap(user -> user.getRoles().stream())
                    .map(role -> role.getName())
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleName.class)));
            notice.setColleges(selected.stream().map(User::getCollege)
                    .filter(Objects::nonNull).collect(Collectors.toSet()));
            notice.setDepartment(null);
        }
        notice.setAudienceRoles(targets);
        Notice saved = notices.save(notice);
        publishCreated(saved);
        return map(saved, activeCollegeIds(), Set.of(), Set.of());
    }

    @Override
    public PageResponse<NoticeRecipientOption> searchRecipients(
            Long collegeId, Long departmentId, RoleName role, String query, int page, int size) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Long scopedCollegeId = collegeId;
        Long scopedDepartmentId = departmentId;
        if (SecurityUtils.isPrincipal()) {
            scopedCollegeId = current.getCollegeId();
            if (role == null) throw new BadRequestException("Select a recipient role");
            ensureAllowed(Set.of(role), PRINCIPAL_TARGETS);
        } else if (SecurityUtils.hasRole("HOD")) {
            StaffProfile hod = staffProfiles.findByUserId(current.getId())
                    .orElseThrow(() -> new BadRequestException("HOD staff profile not found"));
            if (hod.getDepartment() == null) throw new BadRequestException("HOD has no department assigned");
            scopedCollegeId = hod.getCollege().getId();
            scopedDepartmentId = hod.getDepartment().getId();
            if (role == null) throw new BadRequestException("Select a recipient role");
            ensureAllowed(Set.of(role), HOD_TARGETS);
        } else if (!isSystemAdmin()) {
            throw new AccessDeniedException("Your role cannot search notice recipients");
        }
        String keyword = query == null || query.isBlank() ? "" : query.trim();
        var result = users.searchNoticeRecipients(current.getId(), scopedCollegeId, scopedDepartmentId,
                role, keyword, PageRequest.of(safePage, safeSize, Sort.by("fullName").ascending()));
        List<Long> userIds = result.getContent().stream().map(User::getId).toList();
        Map<Long, StaffProfile> staffByUser = staffProfiles.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Map<Long, com.jadhavr.erp.student.entity.StudentProfile> studentByUser =
                studentProfiles.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        var options = result.map(user -> {
            Department department = studentByUser.containsKey(user.getId())
                    ? studentByUser.get(user.getId()).getDepartment()
                    : staffByUser.containsKey(user.getId()) ? staffByUser.get(user.getId()).getDepartment() : null;
            return new NoticeRecipientOption(user.getId(), user.getFullName(), user.getEmail(),
                    user.getCollege() == null ? null : user.getCollege().getId(),
                    user.getCollege() == null ? null : user.getCollege().getName(),
                    department == null ? null : department.getId(),
                    department == null ? null : department.getName(),
                    user.getRoles().stream().map(item -> item.getName()).collect(Collectors.toSet()));
        });
        return PageResponse.from(options);
    }

    @Override
    public NoticeReceiptResponse receipts(Long noticeId) {
        Notice notice = notices.findDetailedByIdIn(List.of(noticeId)).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!notice.getCreatedBy().getId().equals(currentUserId) && !SecurityUtils.isSuperAdmin())
            throw new AccessDeniedException("Only the sender can view notice receipts");

        Map<Long, NoticeView> viewByUser = views.findByNoticeIdOrderBySeenAtDesc(noticeId).stream()
                .collect(Collectors.toMap(view -> view.getUser().getId(), Function.identity(), (first, ignored) -> first));
        Map<Long, User> receiptUsers = new java.util.LinkedHashMap<>();
        if (notice.getDeliveryMode() == NoticeDeliveryMode.INDIVIDUAL) {
            notice.getRecipients().forEach(user -> receiptUsers.put(user.getId(), user));
            if (notice.getRecipient() != null)
                receiptUsers.put(notice.getRecipient().getId(), notice.getRecipient());
        } else {
            viewByUser.values().forEach(view -> receiptUsers.put(view.getUser().getId(), view.getUser()));
        }
        List<NoticeReceiptResponse.RecipientReceipt> receiptRows = receiptUsers.values().stream()
                .filter(user -> user.getRoles().stream().noneMatch(role ->
                        role.getName() == RoleName.SUPER_ADMIN || role.getName() == RoleName.ADMIN))
                .map(user -> {
                    NoticeView view = viewByUser.get(user.getId());
                    return new NoticeReceiptResponse.RecipientReceipt(
                            user.getId(), user.getFullName(), user.getEmail(),
                            view != null, view == null ? null : view.getSeenAt());
                })
                .sorted(java.util.Comparator.comparing(
                        NoticeReceiptResponse.RecipientReceipt::fullName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new NoticeReceiptResponse(noticeId, notice.getDeliveryMode(), receiptRows.size(),
                (int) receiptRows.stream().filter(NoticeReceiptResponse.RecipientReceipt::seen).count(),
                receiptRows);
    }

    @Override @Transactional
    public NoticeResponse createWorkflowNotice(String title, String message, NoticePriority priority,
                                               Set<RoleName> audienceRoles, College college,
                                               String actionPath) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        User sender = users.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Notice notice = new Notice();
        notice.setTitle(title.trim());
        notice.setMessage(message.trim());
        notice.setPriority(priority);
        notice.setCreatedBy(sender);
        notice.setAudienceRoles(Set.copyOf(audienceRoles));
        notice.setColleges(Set.of(college));
        notice.setActionPath(actionPath);
        Notice saved = notices.save(notice);
        streams.publishAfterCommit(createdEvent(saved));
        return map(saved, activeCollegeIds(), Set.of(), Set.of());
    }

    @Override @Transactional
    public NoticeResponse createUserWorkflowNotice(
            String title, String message, NoticePriority priority, RoleName audienceRole,
            College college, User recipient, String actionPath) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        User sender = users.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (recipient == null || recipient.getCollege() == null
                || !recipient.getCollege().getId().equals(college.getId())) {
            throw new BadRequestException("Notice recipient is outside the college");
        }
        Notice notice = new Notice();
        notice.setTitle(title.trim());
        notice.setMessage(message.trim());
        notice.setPriority(priority);
        notice.setCreatedBy(sender);
        notice.setRecipient(recipient);
        notice.setDeliveryMode(NoticeDeliveryMode.INDIVIDUAL);
        notice.setAudienceRoles(Set.of(audienceRole));
        notice.setColleges(Set.of(college));
        notice.setActionPath(actionPath);
        Notice saved = notices.save(notice);
        streams.publishAfterCommit(createdEvent(saved));
        return map(saved, activeCollegeIds(), Set.of(), Set.of());
    }

    @Override
    public List<NoticeResponse> inbox() {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        Set<RoleName> roles = resolveRoles(current);
        Long departmentId = currentDepartmentId(current, roles);
        if (roles.isEmpty()) return List.of();
        List<Long> ids = notices.findInboxIds(current.getId(), current.getCollegeId(), departmentId, roles,
                PageRequest.of(0, 100));
        return mapNotices(loadDetailedNotices(ids));
    }

    @Override
    public long unreadCount() {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        Set<RoleName> roles = resolveRoles(current);
        if (roles.isEmpty()) return 0;
        return notices.countUnread(current.getId(), current.getCollegeId(),
                currentDepartmentId(current, roles), roles);
    }

    @Override
    public SseEmitter stream() {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        Set<RoleName> roles = resolveRoles(current);
        Long departmentId = currentDepartmentId(current, roles);
        long count = roles.isEmpty() ? 0 : notices.countUnread(
                current.getId(), current.getCollegeId(), departmentId, roles);
        return streams.subscribe(new NoticeStreamSubscriber(
                current.getId(), current.getCollegeId(), departmentId, roles), count);
    }

    @Override
    public List<NoticeResponse> sent() {
        Long id = SecurityUtils.getCurrentUserId();
        return mapNotices(loadDetailedNotices(notices.findSentIds(id, PageRequest.of(0, 100))));
    }

    @Override @Transactional
    public void acknowledge(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoticeResponse visible = inbox().stream().filter(notice -> notice.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found in your inbox"));
        if (visible.priority() == NoticePriority.NORMAL)
            throw new BadRequestException("Normal notices do not require acknowledgement");
        if (acknowledgements.existsByNoticeIdAndUserId(id, userId)) return;
        NoticeAcknowledgement acknowledgement = new NoticeAcknowledgement();
        acknowledgement.setNotice(notices.getReferenceById(id));
        acknowledgement.setUser(users.getReferenceById(userId));
        acknowledgement.setAcknowledgedAt(LocalDateTime.now());
        acknowledgements.save(acknowledgement);
        streams.publishAfterCommit(NoticeStreamEvent.acknowledged(userId, id));
    }

    @Override @Transactional
    public void markInboxSeen() {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<Long> seenIds = views.findNoticeIdsByUserId(userId);
        User user = users.getReferenceById(userId);
        List<NoticeView> newViews = inbox().stream()
                .filter(notice -> !seenIds.contains(notice.id()))
                .map(notice -> {
                    NoticeView view = new NoticeView();
                    view.setNotice(notices.getReferenceById(notice.id()));
                    view.setUser(user);
                    view.setSeenAt(LocalDateTime.now());
                    return view;
                })
                .toList();
        if (!newViews.isEmpty()) {
            views.saveAll(newViews);
            streams.publishAfterCommit(NoticeStreamEvent.unreadCount(userId, unreadCount()));
        }
    }

    @Override @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.isSuperAdmin()) throw new AccessDeniedException("Only Super Admin can delete notices");
        Notice notice = notices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"));
        if (notice.getDeletedAt() != null) return;
        NoticeStreamEvent deleted = deletedEvent(notice, SecurityUtils.getCurrentUserId());
        User admin = users.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notice.setDeletedAt(LocalDateTime.now());
        notice.setDeletedBy(admin);
        notices.save(notice);
        streams.publishAfterCommit(deleted);
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
    private void ensureAllowedAndNotEmpty(Set<RoleName> requested, Set<RoleName> allowed) {
        if (requested.isEmpty()) throw new BadRequestException("Select at least one audience role");
        ensureAllowed(requested, allowed);
    }
    private boolean isSystemAdmin() {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.hasRole("ADMIN");
    }
    private void ensureDepartmentWithinScope(
            Department department, CustomUserDetails current, Set<Long> requestedCollegeIds) {
        if (SecurityUtils.isPrincipal() && !department.getCollege().getId().equals(current.getCollegeId()))
            throw new AccessDeniedException("Department is outside your college");
        if (isSystemAdmin() && requestedCollegeIds != null && !requestedCollegeIds.isEmpty()
                && !requestedCollegeIds.contains(department.getCollege().getId()))
            throw new BadRequestException("Department does not belong to a selected college");
    }
    private void ensureRecipientWithinScope(User recipient, CustomUserDetails current) {
        if (recipient.getStatus() != com.jadhavr.erp.user.entity.UserStatus.ACTIVE)
            throw new BadRequestException("Inactive users cannot receive notices");
        if (isSystemAdmin()) return;
        if (recipient.getCollege() == null || !recipient.getCollege().getId().equals(current.getCollegeId()))
            throw new AccessDeniedException("Selected user is outside your college");
        if (SecurityUtils.hasRole("HOD")) {
            StaffProfile hod = staffProfiles.findByUserId(current.getId())
                    .orElseThrow(() -> new BadRequestException("HOD staff profile not found"));
            Long departmentId = hod.getDepartment().getId();
            boolean studentMatches = studentProfiles.findByUserId(recipient.getId())
                    .map(profile -> departmentId.equals(profile.getDepartment().getId())).orElse(false);
            boolean staffMatches = staffProfiles.findByUserId(recipient.getId())
                    .map(profile -> profile.belongsToDepartment(departmentId)).orElse(false);
            if (!studentMatches && !staffMatches)
                throw new AccessDeniedException("Selected user is outside your department");
        }
    }
    private void ensureRecipientHasAllowedRole(User recipient, Set<RoleName> allowed) {
        boolean permitted = recipient.getRoles().stream().map(role -> role.getName()).anyMatch(allowed::contains);
        if (!permitted) throw new AccessDeniedException("Selected user role cannot receive this notice");
    }
    private College findCollege(Long id) { return colleges.findById(id).orElseThrow(() -> new ResourceNotFoundException("College not found")); }
    private College requireOwnCollege(CustomUserDetails user) {
        if (user.getCollegeId() == null) throw new BadRequestException("User has no college assigned");
        return findCollege(user.getCollegeId());
    }
    private Set<Long> activeCollegeIds() {
        return colleges.findByStatus(CollegeStatus.ACTIVE).stream()
                .map(College::getId)
                .collect(Collectors.toSet());
    }

    private List<NoticeResponse> mapNotices(List<Notice> source) {
        Set<Long> activeCollegeIds = activeCollegeIds();
        Set<Long> acknowledgedIds = acknowledgements.findNoticeIdsByUserId(SecurityUtils.getCurrentUserId());
        Set<Long> seenIds = views.findNoticeIdsByUserId(SecurityUtils.getCurrentUserId());
        return source.stream().map(notice -> map(notice, activeCollegeIds, acknowledgedIds, seenIds)).toList();
    }

    private List<Notice> loadDetailedNotices(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Notice> byId = notices.findDetailedByIdIn(ids).stream()
                .collect(Collectors.toMap(Notice::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private NoticeStreamEvent createdEvent(Notice notice) {
        if (notice.getRecipient() != null) {
            return NoticeStreamEvent.createdForUser(
                    notice.getCreatedBy().getId(), notice.getRecipient().getId(), notice.getId());
        }
        return NoticeStreamEvent.created(
                notice.getCreatedBy().getId(),
                notice.getId(),
                notice.getColleges().stream().map(College::getId).collect(Collectors.toSet()),
                notice.getDepartment() == null ? null : notice.getDepartment().getId(),
                notice.getAudienceRoles());
    }

    private void publishCreated(Notice notice) {
        if (!notice.getRecipients().isEmpty()) {
            notice.getRecipients().forEach(recipient -> streams.publishAfterCommit(
                    NoticeStreamEvent.createdForUser(
                            notice.getCreatedBy().getId(), recipient.getId(), notice.getId())));
            return;
        }
        streams.publishAfterCommit(createdEvent(notice));
    }

    private NoticeStreamEvent deletedEvent(Notice notice, Long actorUserId) {
        return NoticeStreamEvent.deleted(
                actorUserId,
                notice.getId(),
                notice.getColleges().stream().map(College::getId).collect(Collectors.toSet()),
                notice.getDepartment() == null ? null : notice.getDepartment().getId(),
                notice.getAudienceRoles());
    }

    private NoticeResponse map(Notice n, Set<Long> activeCollegeIds, Set<Long> acknowledgedIds,
                               Set<Long> seenIds) {
        Set<Long> noticeCollegeIds = n.getColleges().stream()
                .map(College::getId)
                .collect(Collectors.toSet());
        boolean allColleges = noticeCollegeIds.isEmpty()
                || (!activeCollegeIds.isEmpty() && noticeCollegeIds.containsAll(activeCollegeIds));
        Set<String> recipientNames = n.getRecipients().stream().map(User::getFullName)
                .limit(20).collect(Collectors.toSet());
        if (n.getRecipient() != null) recipientNames.add(n.getRecipient().getFullName());
        int recipientCount = n.getRecipients().size() + (n.getRecipient() == null ? 0 : 1);
        return new NoticeResponse(n.getId(), n.getTitle(), n.getMessage(), n.getPriority(),
                acknowledgedIds.contains(n.getId()), seenIds.contains(n.getId()), n.getCreatedBy().getId(),
                n.getCreatedBy().getFullName(),
                noticeCollegeIds,
                n.getColleges().stream().map(College::getName).collect(Collectors.toSet()),
                allColleges,
                n.getDepartment() == null ? null : n.getDepartment().getId(),
                n.getDepartment() == null ? null : n.getDepartment().getName(),
                Set.copyOf(n.getAudienceRoles()),
                n.getDeliveryMode(),
                recipientCount, recipientNames,
                n.getActionPath(), n.getCreatedAt());
    }
}
