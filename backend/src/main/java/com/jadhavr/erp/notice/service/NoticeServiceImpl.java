package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.notice.dto.CreateNoticeRequest;
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
import java.time.LocalDateTime;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final NoticeAcknowledgementRepository acknowledgements;
    private final NoticeViewRepository views;
    private final NoticeStreamService streams;

    public NoticeServiceImpl(NoticeRepository notices, UserRepository users, CollegeRepository colleges,
                             StaffProfileRepository staffProfiles, StudentProfileRepository studentProfiles,
                             NoticeAcknowledgementRepository acknowledgements,
                             NoticeViewRepository views,
                             NoticeStreamService streams) {
        this.notices = notices; this.users = users; this.colleges = colleges;
        this.staffProfiles = staffProfiles; this.studentProfiles = studentProfiles;
        this.acknowledgements = acknowledgements;
        this.views = views;
        this.streams = streams;
    }

    @Override @Transactional
    public NoticeResponse create(CreateNoticeRequest request) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        User sender = users.findById(current.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<RoleName> targets = EnumSet.copyOf(request.audienceRoles());
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setMessage(request.message().trim());
        notice.setPriority(request.priority());
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
        Notice saved = notices.save(notice);
        streams.publishAfterCommit(createdEvent(saved));
        return map(saved, activeCollegeIds(), Set.of(), Set.of());
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
        return NoticeStreamEvent.created(
                notice.getCreatedBy().getId(),
                notice.getId(),
                notice.getColleges().stream().map(College::getId).collect(Collectors.toSet()),
                notice.getDepartment() == null ? null : notice.getDepartment().getId(),
                notice.getAudienceRoles());
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
        return new NoticeResponse(n.getId(), n.getTitle(), n.getMessage(), n.getPriority(),
                acknowledgedIds.contains(n.getId()), seenIds.contains(n.getId()), n.getCreatedBy().getId(),
                n.getCreatedBy().getFullName(),
                noticeCollegeIds,
                n.getColleges().stream().map(College::getName).collect(Collectors.toSet()),
                allColleges,
                n.getDepartment() == null ? null : n.getDepartment().getId(),
                n.getDepartment() == null ? null : n.getDepartment().getName(),
                Set.copyOf(n.getAudienceRoles()), n.getActionPath(), n.getCreatedAt());
    }
}
