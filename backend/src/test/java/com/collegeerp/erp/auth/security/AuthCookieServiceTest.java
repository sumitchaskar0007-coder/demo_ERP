package com.collegeerp.erp.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {
    private final AuthCookieService cookies = new AuthCookieService(true, "Strict");

    @Test
    void refreshCookieIsAvailableToRefreshAndLogoutOnly() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.setTokens(response, "access-token", "refresh-token");

        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(header ->
                header.startsWith("erp_refresh=refresh-token")
                        && header.contains("Path=/api/v1/auth;")
                        && header.contains("HttpOnly")
                        && header.contains("Secure")));
    }

    @Test
    void clearExpiresCurrentAndLegacyRefreshCookiePaths() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.clear(response);

        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(header ->
                header.startsWith("erp_refresh=") && header.contains("Path=/api/v1/auth;")));
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(header ->
                header.startsWith("erp_refresh=") && header.contains("Path=/api/v1/auth/refresh;")));
    }
}
