package com.collegeerp.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.entity.UserStatus;
import com.collegeerp.erp.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorizationSnapshotServiceTest {
    private UserRepository users;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private ObjectMapper json;
    private SimpleMeterRegistry metrics;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        users = mock(UserRepository.class);
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        json = new ObjectMapper().registerModule(new JavaTimeModule());
        metrics = new SimpleMeterRegistry();
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void cacheHitAvoidsPostgresAndContainsNoPasswordOrProfileData() throws Exception {
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                7L,
                2L,
                "student@example.test",
                UserStatus.ACTIVE,
                null,
                3,
                true,
                List.of("ROLE_STUDENT", "PERM_NOTICE_READ"));
        String serialized = json.writeValueAsString(snapshot);
        when(values.get("college-erp:test:authz:7")).thenReturn(serialized);
        AuthorizationSnapshotService service = service();

        CustomUserDetails details =
                service.load(7L, "student@example.test");

        assertThat(serialized)
                .doesNotContain("password")
                .doesNotContain("fullName")
                .doesNotContain("phone");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT", "PERM_NOTICE_READ");
        assertThat(details.isMustChangePassword()).isTrue();
        verifyNoInteractions(users);
        assertThat(metrics.counter(
                "auth.authorization.snapshot", "result", "hit").count())
                .isEqualTo(1);
    }

    @Test
    void redisReadOutageFallsBackToCurrentPostgresAuthorization() {
        when(values.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
        when(users.findAuthorizationById(9L))
                .thenReturn(Optional.of(user(9L, RoleName.HOD, 4)));
        AuthorizationSnapshotService service = service();

        CustomUserDetails details = service.load(9L, "user9@example.test");

        assertThat(details.getSessionVersion()).isEqualTo(4);
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_HOD");
        verify(users).findAuthorizationById(9L);
        verify(values, never()).set(anyString(), anyString(), any());
        assertThat(metrics.counter(
                "auth.authorization.snapshot", "result", "redis-fallback").count())
                .isEqualTo(1);
    }

    @Test
    void legacySnapshotWithoutTemporaryPasswordStateIsRejected() {
        String legacy = """
                {"userId":10,"collegeId":2,"email":"user10@example.test",\
                "status":"ACTIVE","lockedUntil":null,"sessionVersion":3,\
                "authorities":["ROLE_STUDENT"]}
                """;
        User databaseUser = user(10L, RoleName.STUDENT, 3);
        databaseUser.setMustChangePassword(true);
        when(values.get("college-erp:test:authz:10")).thenReturn(legacy);
        when(users.findAuthorizationById(10L)).thenReturn(Optional.of(databaseUser));

        CustomUserDetails details = service().load(10L, "user10@example.test");

        assertThat(details.isMustChangePassword()).isTrue();
        verify(users).findAuthorizationById(10L);
    }

    @Test
    void invalidationMarkerBypassesAStaleSnapshot() {
        when(values.get("college-erp:test:authz:11"))
                .thenReturn(AuthorizationSnapshotService.INVALIDATING_MARKER);
        when(users.findAuthorizationById(11L))
                .thenReturn(Optional.of(user(11L, RoleName.PRINCIPAL, 8)));
        AuthorizationSnapshotService service = service();

        CustomUserDetails details =
                service.load(11L, "user11@example.test");

        assertThat(details.getSessionVersion()).isEqualTo(8);
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_PRINCIPAL");
        verify(values).get("college-erp:test:authz:11");
        verify(values, never()).set(anyString(), anyString(), any());
    }

    @Test
    void invalidationFailureFailsClosedInsteadOfLeavingStaleAuthorization() {
        doThrow(new RedisConnectionFailureException("unavailable"))
                .when(values).set(anyString(), anyString(), any(Duration.class));
        AuthorizationSnapshotService service = service();

        assertThrows(
                AuthorizationStateUnavailableException.class,
                () -> service.invalidateOrThrow(12L));
        assertThat(metrics.counter(
                "auth.authorization.snapshot",
                "result",
                "invalidation-failure").count())
                .isEqualTo(1);
    }

    @Test
    void invalidationUsesOneClusterSafeKeyAndMarker() {
        AuthorizationSnapshotService service = service();

        service.invalidateOrThrow(13L);

        verify(values).set(
                "college-erp:test:authz:13",
                AuthorizationSnapshotService.INVALIDATING_MARKER,
                Duration.ofSeconds(40));
        verify(redis, never()).delete(anyString());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void cacheWriteScriptUsesOnlyTheSnapshotKey() {
        when(values.get("college-erp:test:authz:14")).thenReturn(null);
        when(users.findAuthorizationById(14L))
                .thenReturn(Optional.of(user(14L, RoleName.STUDENT, 1)));
        AuthorizationSnapshotService service = service();

        service.load(14L, "user14@example.test");

        ArgumentCaptor<List> keys = ArgumentCaptor.forClass(List.class);
        verify(redis).execute(
                any(),
                keys.capture(),
                eq(AuthorizationSnapshotService.INVALIDATING_MARKER),
                anyString(),
                eq("30000"));
        assertThat(keys.getValue())
                .containsExactly("college-erp:test:authz:14");
    }

    private AuthorizationSnapshotService service() {
        return new AuthorizationSnapshotService(
                users, redis, json, metrics, true, 30, "test");
    }

    private User user(Long id, RoleName roleName, long sessionVersion) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.test");
        user.setFullName("User " + id);
        user.setPasswordHash("not-cached");
        user.setStatus(UserStatus.ACTIVE);
        user.setSessionVersion(sessionVersion);
        user.setRoles(Set.of(role));
        return user;
    }
}
