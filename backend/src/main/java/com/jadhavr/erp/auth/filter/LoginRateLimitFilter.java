package com.jadhavr.erp.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.TrustedClientIpResolver;
import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import com.jadhavr.erp.common.api.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter implements InitializingBean {
    private final ObjectProvider<DistributedRateLimiter> limiterProvider;
    private final ObjectProvider<TrustedClientIpResolver> clientIpProvider;
    private final ObjectMapper objectMapper;
    private final long loginIpLimit;
    private final long refreshIpLimit;
    private final long authenticatedUserLimit;
    private final long anonymousIpLimit;
    private final boolean limiterRequired;

    public LoginRateLimitFilter(
            ObjectProvider<DistributedRateLimiter> limiterProvider,
            ObjectProvider<TrustedClientIpResolver> clientIpProvider,
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.login-ip-per-minute:1000}") long loginIpLimit,
            @Value("${app.rate-limit.refresh-ip-per-minute:120}") long refreshIpLimit,
            @Value("${app.rate-limit.api-user-per-minute:600}") long authenticatedUserLimit,
            @Value("${app.rate-limit.api-anonymous-ip-per-minute:300}") long anonymousIpLimit,
            @Value("${app.rate-limit.required:false}") boolean limiterRequired) {
        this.limiterProvider = limiterProvider;
        this.clientIpProvider = clientIpProvider;
        this.objectMapper = objectMapper;
        this.loginIpLimit = positive(loginIpLimit);
        this.refreshIpLimit = positive(refreshIpLimit);
        this.authenticatedUserLimit = positive(authenticatedUserLimit);
        this.anonymousIpLimit = positive(anonymousIpLimit);
        this.limiterRequired = limiterRequired;
    }

    @Override
    public void afterPropertiesSet() {
        if (limiterRequired && (limiterProvider.getIfAvailable() == null
                || clientIpProvider.getIfAvailable() == null)) {
            throw new IllegalStateException(
                    "Rate limiting is required but its dependencies are unavailable");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        DistributedRateLimiter limiter = limiterProvider.getIfAvailable();
        if (limiter == null) {
            chain.doFilter(request, response);
            return;
        }
        TrustedClientIpResolver clientIps = clientIpProvider.getIfAvailable();
        if (clientIps == null) {
            chain.doFilter(request, response);
            return;
        }
        RequestLimit requestLimit = limitFor(request, clientIps);
        DistributedRateLimiter.Decision decision = limiter.check(
                requestLimit.namespace(),
                requestLimit.subject(),
                requestLimit.maximum(),
                Duration.ofMinutes(1));
        if (!decision.allowed()) {
            writeRejected(request, response, decision.retryAfterSeconds());
            return;
        }
        chain.doFilter(request, response);
    }

    private RequestLimit limitFor(
            HttpServletRequest request, TrustedClientIpResolver clientIps) {
        String clientIp = clientIps.resolve(request);
        if ("POST".equals(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI())) {
            return new RequestLimit("http:login-ip", clientIp, loginIpLimit);
        }
        if ("POST".equals(request.getMethod())
                && "/api/v1/auth/refresh".equals(request.getRequestURI())) {
            return new RequestLimit("http:refresh-ip", clientIp, refreshIpLimit);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails user) {
            return new RequestLimit(
                    "http:api-user",
                    Long.toString(user.getId()),
                    authenticatedUserLimit);
        }
        return new RequestLimit("http:api-anonymous-ip", clientIp, anonymousIpLimit);
    }

    private void writeRejected(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfterSeconds)));
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("Too many requests", request.getRequestURI()));
    }

    private static long positive(long value) {
        if (value < 1) {
            throw new IllegalArgumentException("Rate-limit thresholds must be positive");
        }
        return value;
    }

    private record RequestLimit(String namespace, String subject, long maximum) {
    }
}
