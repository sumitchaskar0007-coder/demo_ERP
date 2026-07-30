package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.notice.entity.Notice;
import com.jadhavr.erp.notice.entity.NoticePriority;
import com.jadhavr.erp.notice.repository.NoticeAcknowledgementRepository;
import com.jadhavr.erp.notice.repository.NoticeRepository;
import com.jadhavr.erp.notice.repository.NoticeViewRepository;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {
    @Mock private NoticeRepository notices;
    @Mock private UserRepository users;
    @Mock private CollegeRepository colleges;
    @Mock private StaffProfileRepository staffProfiles;
    @Mock private StudentProfileRepository studentProfiles;
    @Mock private NoticeAcknowledgementRepository acknowledgements;
    @Mock private NoticeViewRepository views;
    @Mock private NoticeStreamService streams;

    private NoticeServiceImpl service;
    private User currentUser;

    @BeforeEach
    void setUp() {
        service = new NoticeServiceImpl(notices, users, colleges, staffProfiles, studentProfiles,
                acknowledgements, views, streams);
        currentUser = authenticatedHod();
        lenient().when(colleges.findByStatus(com.jadhavr.erp.college.entity.CollegeStatus.ACTIVE))
                .thenReturn(List.of());
        lenient().when(acknowledgements.findNoticeIdsByUserId(currentUser.getId())).thenReturn(Set.of());
        lenient().when(views.findNoticeIdsByUserId(currentUser.getId())).thenReturn(Set.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sentPagesIdsBeforeFetchingCollectionsAndPreservesPageOrder() {
        Notice newer = notice(12L);
        Notice older = notice(11L);
        when(notices.findSentIds(currentUser.getId(), PageRequest.of(0, 100)))
                .thenReturn(List.of(12L, 11L));
        when(notices.findDetailedByIdIn(List.of(12L, 11L)))
                .thenReturn(List.of(older, newer));

        var result = service.sent();

        assertEquals(List.of(12L, 11L), result.stream().map(response -> response.id()).toList());
        verify(notices).findSentIds(currentUser.getId(), PageRequest.of(0, 100));
        verify(notices).findDetailedByIdIn(List.of(12L, 11L));
    }

    @Test
    void inboxPagesIdsBeforeFetchingCollections() {
        Notice notice = notice(21L);
        when(staffProfiles.findByUserId(currentUser.getId())).thenReturn(Optional.empty());
        when(notices.findInboxIds(currentUser.getId(), null, null, Set.of(RoleName.HOD),
                PageRequest.of(0, 100))).thenReturn(List.of(21L));
        when(notices.findDetailedByIdIn(List.of(21L))).thenReturn(List.of(notice));

        var result = service.inbox();

        assertEquals(List.of(21L), result.stream().map(response -> response.id()).toList());
        verify(notices).findInboxIds(currentUser.getId(), null, null, Set.of(RoleName.HOD),
                PageRequest.of(0, 100));
        verify(notices).findDetailedByIdIn(List.of(21L));
    }

    @Test
    void unreadCountUsesTenantAndRoleScopedDatabaseQuery() {
        when(staffProfiles.findByUserId(currentUser.getId())).thenReturn(Optional.empty());
        when(notices.countUnread(currentUser.getId(), null, null, Set.of(RoleName.HOD)))
                .thenReturn(17L);

        assertEquals(17L, service.unreadCount());

        verify(notices).countUnread(currentUser.getId(), null, null, Set.of(RoleName.HOD));
    }

    private User authenticatedHod() {
        Role role = new Role();
        role.setId(1L);
        role.setName(RoleName.HOD);
        User user = new User();
        user.setId(7L);
        user.setFullName("Test HOD");
        user.setEmail("hod@example.com");
        user.setPasswordHash("not-used");
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        return user;
    }

    private Notice notice(Long id) {
        Notice notice = mock(Notice.class);
        when(notice.getId()).thenReturn(id);
        when(notice.getTitle()).thenReturn("Notice " + id);
        when(notice.getMessage()).thenReturn("Message");
        when(notice.getPriority()).thenReturn(NoticePriority.NORMAL);
        when(notice.getCreatedBy()).thenReturn(currentUser);
        when(notice.getColleges()).thenReturn(Set.of());
        when(notice.getAudienceRoles()).thenReturn(Set.of(RoleName.STUDENT));
        when(notice.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 10, 0));
        return notice;
    }
}
