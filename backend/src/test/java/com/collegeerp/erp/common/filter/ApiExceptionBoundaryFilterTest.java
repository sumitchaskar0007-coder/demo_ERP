package com.collegeerp.erp.common.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiExceptionBoundaryFilterTest {

    @Test
    void hidesFilterStackDetailsAndLogsTheFullException() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionBoundaryFilter.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try {
            ApiExceptionBoundaryFilter filter =
                    new ApiExceptionBoundaryFilter(new ObjectMapper().findAndRegisterModules());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                throw new ServletException(
                        "C:\\private\\application.properties: SQLSTATE 23505");
            });

            assertEquals(500, response.getStatus());
            assertTrue(response.getContentType().startsWith("application/json"));
            assertNotNull(response.getHeader("X-Request-ID"));
            String body = response.getContentAsString();
            assertTrue(body.contains("The request could not be completed. Please try again."));
            assertFalse(body.contains("application.properties"));
            assertFalse(body.contains("SQLSTATE"));
            assertTrue(events.list.stream().anyMatch(event -> event.getThrowableProxy() != null));
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }
}
