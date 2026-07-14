package com.jadhavr.erp.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
public class AuthCookieService {
    public static final String ACCESS_COOKIE = "erp_access";
    public static final String REFRESH_COOKIE = "erp_refresh";
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(@Value("${app.auth.cookie-secure:true}") boolean secure,
                             @Value("${app.auth.cookie-same-site:Strict}") String sameSite) {
        this.secure = secure; this.sameSite = sameSite;
    }
    public void setTokens(HttpServletResponse response, String access, String refresh) {
        add(response, ACCESS_COOKIE, access, "/", Duration.ofMinutes(15));
        add(response, REFRESH_COOKIE, refresh, "/api/v1/auth/refresh", Duration.ofDays(7));
    }
    public void clear(HttpServletResponse response) {
        add(response, ACCESS_COOKIE, "", "/", Duration.ZERO);
        add(response, REFRESH_COOKIE, "", "/api/v1/auth/refresh", Duration.ZERO);
    }
    private void add(HttpServletResponse response, String name, String value, String path, Duration age) {
        ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(true).secure(secure)
                .sameSite(sameSite).path(path).maxAge(age).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
