package com.jadhavr.erp.notice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoticeRepositoryPaginationContractTest {

    @Test
    void pageableQueriesReturnIdsWithoutCollectionEntityGraph() throws NoSuchMethodException {
        Method inbox = NoticeRepository.class.getMethod(
                "findInboxIds", Long.class, Long.class, Long.class, Collection.class, Pageable.class);
        Method sent = NoticeRepository.class.getMethod("findSentIds", Long.class, Pageable.class);

        assertFalse(inbox.isAnnotationPresent(EntityGraph.class));
        assertFalse(sent.isAnnotationPresent(EntityGraph.class));
        assertTrue(inbox.getGenericReturnType().getTypeName().contains("java.lang.Long"));
        assertTrue(sent.getGenericReturnType().getTypeName().contains("java.lang.Long"));
    }

    @Test
    void detailFetchUsesEntityGraphWithoutPagination() throws NoSuchMethodException {
        Method details = NoticeRepository.class.getMethod("findDetailedByIdIn", Collection.class);

        EntityGraph graph = details.getAnnotation(EntityGraph.class);
        assertNotNull(graph);
        assertTrue(Arrays.asList(graph.attributePaths())
                .containsAll(List.of("createdBy", "colleges", "department", "audienceRoles")));
        assertTrue(Arrays.stream(details.getParameterTypes()).noneMatch(Pageable.class::equals));
    }
}
