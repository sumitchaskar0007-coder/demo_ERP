package com.jadhavr.erp.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.common.api.ErrorResponse;
import com.jadhavr.erp.common.error.RequestCorrelation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Last-resort API boundary for failures raised by servlet/security filters before
 * Spring MVC can invoke {@code GlobalExceptionHandler}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionBoundaryFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionBoundaryFilter.class);
    private static final String GENERIC_MESSAGE =
            "The request could not be completed. Please try again.";

    private final ObjectMapper objectMapper;

    public ApiExceptionBoundaryFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = RequestCorrelation.getOrCreate(request);
        response.setHeader(RequestCorrelation.HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            log.error("Unhandled filter-chain failure correlationId={} method={} path={}",
                    correlationId, request.getMethod(), request.getRequestURI(), exception);
            if (response.isCommitted()) {
                if (exception instanceof IOException ioException) throw ioException;
                if (exception instanceof ServletException servletException) throw servletException;
                throw new ServletException("Request processing failed", exception);
            }
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(RequestCorrelation.HEADER, correlationId);
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                    false,
                    GENERIC_MESSAGE,
                    LocalDateTime.now(),
                    Map.of("correlationId", correlationId),
                    request.getRequestURI()));
        }
    }
}
