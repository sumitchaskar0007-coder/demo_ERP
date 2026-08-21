package com.collegeerp.erp.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.user.entity.RoleName;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NoticeStreamEventTest {

    @Test
    void audienceRoutingDoesNotCrossCollegeDepartmentRoleOrSender() {
        NoticeStreamEvent event = NoticeStreamEvent.created(
                99L, 500L, Set.of(10L), 20L, Set.of(RoleName.STUDENT));

        assertTrue(event.matches(subscriber(1L, 10L, 20L, RoleName.STUDENT)));
        assertFalse(event.matches(subscriber(2L, 11L, 20L, RoleName.STUDENT)));
        assertFalse(event.matches(subscriber(3L, 10L, 21L, RoleName.STUDENT)));
        assertFalse(event.matches(subscriber(4L, 10L, 20L, RoleName.HOD)));
        assertFalse(event.matches(subscriber(99L, 10L, 20L, RoleName.STUDENT)));
    }

    @Test
    void serviceSendsOnlyToMatchingRegistrations() throws Exception {
        TaskExecutor sameThread = Runnable::run;
        NoticeStreamService service = new NoticeStreamService(
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                sameThread,
                new SimpleMeterRegistry(),
                false,
                30_000,
                100,
                3);
        SseEmitter intended = mock(SseEmitter.class);
        SseEmitter otherCollege = mock(SseEmitter.class);
        service.register(subscriber(1L, 10L, 20L, RoleName.STUDENT), intended);
        service.register(subscriber(2L, 11L, 20L, RoleName.STUDENT), otherCollege);

        service.deliver(NoticeStreamEvent.created(
                99L, 500L, Set.of(10L), 20L, Set.of(RoleName.STUDENT)));

        verify(intended).send(any(SseEmitter.SseEventBuilder.class));
        verify(otherCollege, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    private NoticeStreamSubscriber subscriber(
            Long userId, Long collegeId, Long departmentId, RoleName role) {
        return new NoticeStreamSubscriber(userId, collegeId, departmentId, Set.of(role));
    }
}
