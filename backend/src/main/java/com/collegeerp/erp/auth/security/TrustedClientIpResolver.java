package com.collegeerp.erp.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves a client address without trusting forwarding headers supplied by a
 * directly connected, untrusted peer.
 */
@Component
public class TrustedClientIpResolver {
    static final String X_FORWARDED_FOR = "X-Forwarded-For";
    static final String CLOUDFRONT_VIEWER_ADDRESS = "CloudFront-Viewer-Address";

    private final List<Cidr> trustedCidrs;
    private final Pattern trustedProxyPattern;
    private final boolean cloudFrontViewerAddressEnabled;

    public TrustedClientIpResolver(
            @Value("${app.security.trusted-proxy-cidrs:127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
            String trustedProxyCidrs,
            @Value("${app.security.trusted-proxy-regex:}") String trustedProxyRegex,
            @Value("${app.security.cloudfront-viewer-address-enabled:true}")
            boolean cloudFrontViewerAddressEnabled) {
        this.trustedCidrs = parseCidrs(trustedProxyCidrs);
        this.trustedProxyPattern = trustedProxyRegex == null || trustedProxyRegex.isBlank()
                ? null : Pattern.compile(trustedProxyRegex);
        this.cloudFrontViewerAddressEnabled = cloudFrontViewerAddressEnabled;
    }

    public String resolve(HttpServletRequest request) {
        InetAddress immediatePeer = parseIpLiteral(request.getRemoteAddr());
        if (immediatePeer == null) {
            return "unknown";
        }
        String peer = canonical(immediatePeer);
        if (!isTrusted(immediatePeer, peer)) {
            return peer;
        }

        if (cloudFrontViewerAddressEnabled) {
            InetAddress cloudFrontViewer = parseCloudFrontViewerAddress(
                    request.getHeader(CLOUDFRONT_VIEWER_ADDRESS));
            if (cloudFrontViewer != null) {
                return canonical(cloudFrontViewer);
            }
        }

        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return peer;
        }
        String[] hops = forwardedFor.split(",");
        for (int index = hops.length - 1; index >= 0; index--) {
            InetAddress hop = parseIpLiteral(hops[index].trim());
            if (hop == null) {
                continue;
            }
            String canonicalHop = canonical(hop);
            if (!isTrusted(hop, canonicalHop)) {
                return canonicalHop;
            }
        }
        return peer;
    }

    private boolean isTrusted(InetAddress address, String canonicalAddress) {
        return trustedCidrs.stream().anyMatch(cidr -> cidr.contains(address))
                || (trustedProxyPattern != null
                && trustedProxyPattern.matcher(canonicalAddress).matches());
    }

    private static InetAddress parseCloudFrontViewerAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.startsWith("[")) {
            int closingBracket = candidate.indexOf(']');
            if (closingBracket <= 1) {
                return null;
            }
            candidate = candidate.substring(1, closingBracket);
        } else {
            int firstColon = candidate.indexOf(':');
            int lastColon = candidate.lastIndexOf(':');
            if (firstColon > 0 && firstColon == lastColon) {
                candidate = candidate.substring(0, firstColon);
            }
        }
        return parseIpLiteral(candidate);
    }

    static InetAddress parseIpLiteral(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.contains("%")) {
            return null;
        }
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.indexOf(':') < 0) {
            String[] octets = candidate.split("\\.", -1);
            if (octets.length != 4) {
                return null;
            }
            for (String octet : octets) {
                if (octet.isEmpty() || octet.length() > 3
                        || !octet.chars().allMatch(Character::isDigit)) {
                    return null;
                }
                int number = Integer.parseInt(octet);
                if (number > 255) {
                    return null;
                }
            }
        } else if (!candidate.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static List<Cidr> parseCidrs(String configuredCidrs) {
        if (configuredCidrs == null || configuredCidrs.isBlank()) {
            return List.of();
        }
        List<Cidr> parsed = new ArrayList<>();
        Arrays.stream(configuredCidrs.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Cidr::parse)
                .forEach(parsed::add);
        return List.copyOf(parsed);
    }

    private static String canonical(InetAddress address) {
        return address.getHostAddress();
    }

    private record Cidr(byte[] network, int prefixLength) {
        private static Cidr parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR");
            }
            InetAddress address = parseIpLiteral(parts[0]);
            if (address == null) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR");
            }
            int prefix;
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR", exception);
            }
            int maximumPrefix = address.getAddress().length * 8;
            if (prefix < 0 || prefix > maximumPrefix) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR prefix");
            }
            byte[] network = address.getAddress().clone();
            for (int bit = prefix; bit < maximumPrefix; bit++) {
                network[bit / 8] &= (byte) ~(1 << (7 - bit % 8));
            }
            return new Cidr(network, prefix);
        }

        private boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int wholeBytes = prefixLength / 8;
            for (int index = 0; index < wholeBytes; index++) {
                if (candidate[index] != network[index]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % 8;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - remainingBits);
            return (candidate[wholeBytes] & mask) == (network[wholeBytes] & mask);
        }
    }
}
