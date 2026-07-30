package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthorizationSnapshotServiceTest {
    @Test
    void cacheHitDoesNotReadPostgresAndContainsNoPassword() throws Exception {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                7L, 2L, "student@example.test", "Student", UserStatus.ACTIVE,
                null, 3, false, List.of("ROLE_STUDENT", "PERM_NOTICE_READ"));
        String serialized = json.writeValueAsString(snapshot);
        assertThat(serialized).doesNotContain("password");
        when(values.get(anyString())).thenReturn(serialized);

        AuthorizationSnapshotService service = new AuthorizationSnapshotService(
                users, redis, json, new SimpleMeterRegistry(), true, 60, "test");
        CustomUserDetails details = service.load(7L, "student@example.test");

        assertThat(details.getId()).isEqualTo(7L);
        assertThat(details.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT", "PERM_NOTICE_READ");
        verifyNoInteractions(users);
    }

    @Test
    void redisFailureSafelyFallsBackToPostgres() {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        var user = new com.jadhavr.erp.user.entity.User();
        user.setId(9L);
        user.setEmail("staff@example.test");
        user.setFullName("Staff");
        user.setPasswordHash("not-cached");
        user.setStatus(UserStatus.ACTIVE);
        when(users.findById(9L)).thenReturn(java.util.Optional.of(user));

        AuthorizationSnapshotService service = new AuthorizationSnapshotService(
                users, redis, new ObjectMapper().registerModule(new JavaTimeModule()),
                new SimpleMeterRegistry(), true, 60, "test");

        assertThat(service.load(9L, "staff@example.test").getId()).isEqualTo(9L);
        verify(users).findById(9L);
    }
}
