package com.jadhavr.erp.notice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

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
}
