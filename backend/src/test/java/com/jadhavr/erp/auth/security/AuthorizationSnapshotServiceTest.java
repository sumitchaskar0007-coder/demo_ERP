package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
                List.of("ROLE_STUDENT", "PERM_NOTICE_READ"));
        String serialized = json.writeValueAsString(snapshot);
        when(redis.hasKey("college-erp:test:authz:invalidating:7")).thenReturn(false);
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
        verifyNoInteractions(users);
        assertThat(metrics.counter(
                "auth.authorization.snapshot", "result", "hit").count())
                .isEqualTo(1);
    }

    @Test
    void redisReadOutageFallsBackToCurrentPostgresAuthorization() {
        when(redis.hasKey(anyString()))
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
    void invalidationGuardBypassesAStaleSnapshot() {
        when(redis.hasKey("college-erp:test:authz:invalidating:11"))
                .thenReturn(true);
        when(users.findAuthorizationById(11L))
                .thenReturn(Optional.of(user(11L, RoleName.PRINCIPAL, 8)));
        AuthorizationSnapshotService service = service();

        CustomUserDetails details =
                service.load(11L, "user11@example.test");

        assertThat(details.getSessionVersion()).isEqualTo(8);
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_PRINCIPAL");
        verify(values, never()).get(anyString());
        verify(values, never()).set(anyString(), anyString(), any());
    }

    @Test
    void invalidationFailureFailsClosedInsteadOfLeavingStaleAuthorization() {
        when(redis.execute(any(), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
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
