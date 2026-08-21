package com.collegeerp.erp.notice.service;

import com.collegeerp.erp.user.entity.RoleName;

import java.util.Collections;
import java.util.Set;

/**
 * Small, non-sensitive event shared between backend replicas.
 *
 * <p>The event carries only routing information. Notice content remains behind
 * the authenticated inbox endpoint.</p>
 */
public record NoticeStreamEvent(
        Type type,
        Long actorUserId,
        Long subjectUserId,
        Long noticeId,
        Set<Long> collegeIds,
        Long departmentId,
        Set<RoleName> audienceRoles,
        Long unreadCount) {

    public NoticeStreamEvent {
        collegeIds = collegeIds == null ? Collections.emptySet() : Set.copyOf(collegeIds);
        audienceRoles = audienceRoles == null ? Collections.emptySet() : Set.copyOf(audienceRoles);
    }

    public static NoticeStreamEvent created(Long actorUserId, Long noticeId, Set<Long> collegeIds,
                                            Long departmentId, Set<RoleName> audienceRoles) {
        return new NoticeStreamEvent(Type.NOTICE_CREATED, actorUserId, null, noticeId,
                collegeIds, departmentId, audienceRoles, null);
    }

    public static NoticeStreamEvent createdForUser(
            Long actorUserId, Long subjectUserId, Long noticeId) {
        return new NoticeStreamEvent(Type.NOTICE_CREATED, actorUserId, subjectUserId, noticeId,
                Set.of(), null, Set.of(), null);
    }

    public static NoticeStreamEvent deleted(Long actorUserId, Long noticeId, Set<Long> collegeIds,
                                            Long departmentId, Set<RoleName> audienceRoles) {
        return new NoticeStreamEvent(Type.NOTICE_DELETED, actorUserId, null, noticeId,
                collegeIds, departmentId, audienceRoles, null);
    }

    public static NoticeStreamEvent unreadCount(Long userId, long unreadCount) {
        return new NoticeStreamEvent(Type.UNREAD_COUNT, null, userId, null,
                Set.of(), null, Set.of(), unreadCount);
    }

    public static NoticeStreamEvent acknowledged(Long userId, Long noticeId) {
        return new NoticeStreamEvent(Type.NOTICE_ACKNOWLEDGED, null, userId, noticeId,
                Set.of(), null, Set.of(), null);
    }

    boolean matches(NoticeStreamSubscriber subscriber) {
        if (subjectUserId != null) return subjectUserId.equals(subscriber.userId());
        if (actorUserId != null && actorUserId.equals(subscriber.userId())) return false;
        if (audienceRoles.isEmpty()
                || Collections.disjoint(audienceRoles, subscriber.roles())) return false;
        if (!collegeIds.isEmpty()
                && (subscriber.collegeId() == null || !collegeIds.contains(subscriber.collegeId()))) {
            return false;
        }
        return departmentId == null || departmentId.equals(subscriber.departmentId());
    }

    public enum Type {
        NOTICE_CREATED,
        NOTICE_DELETED,
        UNREAD_COUNT,
        NOTICE_ACKNOWLEDGED
    }
}
