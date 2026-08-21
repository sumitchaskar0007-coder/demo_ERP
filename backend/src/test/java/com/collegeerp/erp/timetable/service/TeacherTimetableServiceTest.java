package com.collegeerp.erp.timetable.service;

import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.timetable.repository.WeeklyPeriodRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.entity.WeeklyTimetable;
import com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherTimetableServiceTest {
    @Mock StaffProfileRepository staff;
    @Mock WeeklyTimetableEntryRepository entries;
    @Mock WeeklyPeriodRepository periods;
    @Mock EffectiveLectureService effectiveLectures;
    private TeacherTimetableService service;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(77L);
        user.setEmail("teacher@example.com");
        user.setFullName("Test Teacher");
        user.setPasswordHash("secret");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(RoleName.SUBJECT_TEACHER);
        user.setRoles(Set.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user), null, List.of(new SimpleGrantedAuthority("ROLE_SUBJECT_TEACHER"))));
        service = new TeacherTimetableService(staff, entries, periods, effectiveLectures,
                Clock.fixed(Instant.parse("2026-07-16T08:00:00Z"), ZoneId.of("Asia/Kolkata")));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nextAlwaysScopesEntriesToAuthenticatedTeacherProfile() {
        StaffProfile profile = new StaffProfile();
        profile.setId(12L);
        when(staff.findByUserId(77L)).thenReturn(Optional.of(profile));
        when(entries.findByTeacherId(12L)).thenReturn(List.of());

        var response = service.next();

        assertNull(response.lecture());
        verify(staff).findByUserId(77L);
        verify(entries).findByTeacherId(12L);
    }

    @Test
    void submittedTimetableIsHiddenUntilPrincipalApproval() {
        StaffProfile profile = new StaffProfile();
        profile.setId(12L);
        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setStatus(WeeklyTimetable.Status.ACTIVE);
        timetable.setReviewStatus(WeeklyTimetable.ReviewStatus.SUBMITTED);
        WeeklyTimetableEntry entry = new WeeklyTimetableEntry();
        entry.setTimetable(timetable);

        when(staff.findByUserId(77L)).thenReturn(Optional.of(profile));
        when(entries.findByTeacherId(12L)).thenReturn(List.of(entry));

        var response = service.next();

        assertNull(response.lecture());
    }
}
