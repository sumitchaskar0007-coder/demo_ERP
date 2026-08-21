package com.collegeerp.erp.auth.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.auth.security.AuthCookieService;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.auth.security.TrustedClientIpResolver;
import com.collegeerp.erp.auth.service.DistributedRateLimiter;
import com.collegeerp.erp.common.api.ErrorResponse;
import com.collegeerp.erp.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter implements InitializingBean {
    private final ObjectProvider<DistributedRateLimiter> limiterProvider;
    private final ObjectProvider<TrustedClientIpResolver> clientIpProvider;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final boolean limiterRequired;

    public LoginRateLimitFilter(
            ObjectProvider<DistributedRateLimiter> limiterProvider,
            ObjectProvider<TrustedClientIpResolver> clientIpProvider,
            ObjectMapper objectMapper,
            RateLimitProperties properties,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.rate-limit.required:false}") boolean limiterRequired) {
        this.limiterProvider = limiterProvider;
        this.clientIpProvider = clientIpProvider;
        this.objectMapper = objectMapper;
        this.properties = properties;
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
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        DistributedRateLimiter limiter = limiterProvider.getIfAvailable();
        TrustedClientIpResolver clientIps = clientIpProvider.getIfAvailable();
        if (limiter == null || clientIps == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = clientIps.resolve(request);
        AuthRule authRule = authRule(request);
        if (authRule != null) {
            applyAuthLimits(request, response, chain, limiter, clientIp, authRule);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails user) {
            checkSingleAndContinue(request, response, chain, limiter,
                    "http:authenticated-user", Long.toString(user.getId()),
                    properties.getAuthenticated().getPerAccount());
            return;
        }
        checkSingleAndContinue(request, response, chain, limiter,
                "http:public-ip", clientIp, properties.getPublicEndpoints().getPerIp());
    }

    private void applyAuthLimits(HttpServletRequest original, HttpServletResponse response,
            FilterChain chain, DistributedRateLimiter limiter, String clientIp, AuthRule rule)
            throws IOException, ServletException {
        HttpServletRequest request = original;
        String account;
        if (rule.accountField != null) {
            BufferedRequest buffered = buffer(original, response);
            if (buffered == null) return;
            request = buffered;
            account = accountFromJson(buffered.body, rule.accountField, clientIp);
        } else {
            account = accountFromCookie(original, clientIp);
        }

        String namespace = "http:auth:" + rule.name;
        DistributedRateLimiter.Decision ipRate = limiter.check(
                namespace + ":ip", clientIp, rule.policy.getPerIp(), properties.getWindow());
        DistributedRateLimiter.Decision accountRate = limiter.check(
                namespace + ":account", account, rule.policy.getPerAccount(), properties.getWindow());
        DistributedRateLimiter.Decision ipBackoff = rule.exponentialBackoff
                ? limiter.checkBackoff(namespace + ":backoff-ip", clientIp)
                : new DistributedRateLimiter.Decision(true, 0);
        DistributedRateLimiter.Decision accountBackoff = rule.exponentialBackoff
                ? limiter.checkBackoff(namespace + ":backoff-account", account)
                : new DistributedRateLimiter.Decision(true, 0);
        long retryAfter = maximumRetry(ipRate, accountRate, ipBackoff, accountBackoff);
        if (retryAfter > 0) {
            writeRejected(request, response, retryAfter, "Too many authentication attempts");
            return;
        }

        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            boolean failed = rule.backoffAfterSuccess
                    || !completed || isAuthenticationFailure(response.getStatus());
            RateLimitProperties.Backoff backoff = properties.getBackoff();
            if (failed && rule.exponentialBackoff) {
                limiter.recordFailure(namespace + ":backoff-ip", clientIp,
                        backoff.getBaseDelay(), backoff.getMaxDelay(), backoff.getResetAfter());
                limiter.recordFailure(namespace + ":backoff-account", account,
                        backoff.getBaseDelay(), backoff.getMaxDelay(), backoff.getResetAfter());
            } else if (rule.exponentialBackoff && response.getStatus() < 400) {
                limiter.resetBackoff(namespace + ":backoff-account", account);
            }
        }
    }

    private void checkSingleAndContinue(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, DistributedRateLimiter limiter, String namespace, String subject,
            long maximum) throws IOException, ServletException {
        DistributedRateLimiter.Decision decision =
                limiter.check(namespace, subject, maximum, properties.getWindow());
        if (!decision.allowed()) {
            writeRejected(request, response, decision.retryAfterSeconds(), "Too many requests");
            return;
        }
        chain.doFilter(request, response);
    }

    private BufferedRequest buffer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int maximum = properties.getMaxAuthBodyBytes();
        byte[] body = request.getInputStream().readNBytes(maximum + 1);
        if (body.length > maximum) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Authentication request body is too large",
                            request.getRequestURI()));
            return null;
        }
        return new BufferedRequest(request, body);
    }

    private String accountFromJson(byte[] body, String field, String clientIp) {
        try {
            JsonNode value = objectMapper.readTree(body).path(field);
            if (value.isTextual() && !value.textValue().isBlank()) {
                return value.textValue().trim().toLowerCase(Locale.ROOT);
            }
        } catch (IOException ignored) {
            // Validation will produce the client-facing malformed JSON response.
        }
        return "invalid-payload:" + clientIp + ":" + Integer.toHexString(Arrays.hashCode(body));
    }

    private String accountFromCookie(HttpServletRequest request, String clientIp) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AuthCookieService.REFRESH_COOKIE.equals(cookie.getName())
                        && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return "missing-refresh-token:" + clientIp;
    }

    private AuthRule authRule(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        String path = request.getRequestURI();
        if ("/api/v1/auth/login".equals(path)) {
            // Login uses the fixed six-attempt window so attempts 1-6 can return
            // an accurate invalid-credentials response. Attempt 7 is throttled.
            // Applying exponential backoff here would reject attempt 2 after a
            // quickly repeated first failure and make the configured boundary
            // impossible to observe.
            return new AuthRule("login", properties.getLogin(), "email", false, false);
        }
        if ("/api/v1/auth/refresh".equals(path)) {
            return new AuthRule("refresh", properties.getRefresh(), null, false, true);
        }
        if ("/api/auth/password/forgot".equals(path)) {
            // The endpoint is deliberately enumeration-safe and always returns 200,
            // so every resend is an attempt for exponential backoff purposes.
            return new AuthRule("password-forgot", properties.getPassword(), "email", true, true);
        }
        if ("/api/auth/password/reset".equals(path)) {
            return new AuthRule("password-reset", properties.getPassword(), "token", false, true);
        }
        if ("/api/auth/email-verification/confirm".equals(path)) {
            return new AuthRule("email-confirm", properties.getOtherAuth(), "token", false, true);
        }
        if (path.matches("/api/public/admissions/college/[^/]+/submit")) {
            return new AuthRule("signup", properties.getSignup(), "email", false, true);
        }
        return null;
    }

    private long maximumRetry(DistributedRateLimiter.Decision... decisions) {
        long maximum = 0;
        for (DistributedRateLimiter.Decision decision : decisions) {
            if (!decision.allowed()) maximum = Math.max(maximum, decision.retryAfterSeconds());
        }
        return maximum;
    }

    private boolean isAuthenticationFailure(int status) {
        return status >= 400 && status < 500 && status != HttpStatus.TOO_MANY_REQUESTS.value();
    }

    private void writeRejected(HttpServletRequest request, HttpServletResponse response,
            long retryAfterSeconds, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfterSeconds)));
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(message, request.getRequestURI()));
    }

    private record AuthRule(String name, RateLimitProperties.Policy policy,
            String accountField, boolean backoffAfterSuccess, boolean exponentialBackoff) {
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        private BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
                @Override public int read() { return input.read(); }
            };
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
