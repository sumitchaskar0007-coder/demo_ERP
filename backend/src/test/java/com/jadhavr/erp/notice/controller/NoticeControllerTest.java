package com.jadhavr.erp.notice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoticeControllerTest {
    @Test
    void sentEndpointRequiresNoticeSendPermission() throws NoSuchMethodException {
        Method method = NoticeController.class.getMethod("sent");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('PERM_NOTICE_SEND')", preAuthorize.value());

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/sent"}, getMapping.value());
    }

    @Test
    void markInboxSeenEndpointRequiresNoticeReadPermission() throws NoSuchMethodException {
        Method method = NoticeController.class.getMethod("markInboxSeen");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('PERM_NOTICE_READ')", preAuthorize.value());

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/inbox/seen"}, postMapping.value());
    }
}
