package com.jadhavr.erp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateAccessToken(CustomUserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList();
        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getUsername())
                .claim("sessionVersion", user.getSessionVersion())
                .claim("roles", roles);
        if (user.getCollegeId() != null) {
            builder.claim("collegeId", user.getCollegeId());
            builder.claim("institution_id", user.getCollegeId());
        }
        Date now = new Date();
        return builder.issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
    /** @deprecated use generateAccessToken; retained for source compatibility. */
    @Deprecated
    public String generateToken(CustomUserDetails user) { return generateAccessToken(user); }

    public String extractUsername(String token) { return claims(token).getSubject(); }
    public Long extractUserId(String token) {
        Object userId = claims(token).get("userId");
        return userId instanceof Number number ? number.longValue() : Long.valueOf(userId.toString());
    }
    public Date extractExpiration(String token) { return claims(token).getExpiration(); }
    public boolean isTokenValid(String token, UserDetails user) {
        Object versionClaim = claims(token).get("sessionVersion");
        boolean versionMatches = !(user instanceof CustomUserDetails custom)
                || (versionClaim instanceof Number number && number.longValue() == custom.getSessionVersion());
        return extractUsername(token).equals(user.getUsername()) && versionMatches
                && extractExpiration(token).after(new Date()) && user.isEnabled() && user.isAccountNonLocked();
    }
    public long getExpirationMs() { return expirationMs; }
    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
