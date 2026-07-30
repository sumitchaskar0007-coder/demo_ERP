package com.jadhavr.erp.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedClientIpResolverTest {
    private final TrustedClientIpResolver resolver =
            new TrustedClientIpResolver(
                    "10.0.0.0/8,192.168.0.0/16,127.0.0.1/32,::1/128",
                    "",
                    true);

    @Test
    void ignoresSpoofedForwardingHeadersFromUntrustedPeer() {
        MockHttpServletRequest request = request("198.51.100.20");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("CloudFront-Viewer-Address", "5.6.7.8:443");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void choosesRightmostUntrustedHopInsteadOfSpoofedLeftmostValue() {
        MockHttpServletRequest request = request("10.0.4.15");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 198.51.100.77");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.77");
    }

    @Test
    void walksRightToLeftAcrossMultipleTrustedProxyHops() {
        MockHttpServletRequest request = request("10.0.4.15");
        request.addHeader(
                "X-Forwarded-For",
                "203.0.113.42, 192.168.20.9, 10.0.8.30");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.42");
    }

    @Test
    void acceptsCloudFrontViewerAddressOnlyBehindTrustedImmediatePeer() {
        MockHttpServletRequest request = request("10.0.4.15");
        request.addHeader(
                "CloudFront-Viewer-Address",
                "[2001:db8:85a3::8a2e:370:7334]:443");
        request.addHeader("X-Forwarded-For", "198.51.100.77");

        assertThat(resolver.resolve(request))
                .isEqualTo("2001:db8:85a3:0:0:8a2e:370:7334");
    }

    @Test
    void supportsExplicitTrustedProxyRegex() {
        TrustedClientIpResolver regexResolver =
                new TrustedClientIpResolver("", "127\\.0\\.0\\.1", false);
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(regexResolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
