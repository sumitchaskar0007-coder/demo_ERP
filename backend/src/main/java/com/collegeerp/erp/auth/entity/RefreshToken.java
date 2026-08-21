package com.collegeerp.erp.auth.entity;

import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_user_active", columnList = "user_id, revoked")
})
public class RefreshToken {
    @Id @Column(length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "institution_id") private College institution;
    @Column(name = "issued_at") private LocalDateTime issuedAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private boolean revoked;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void beforeInsert() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = LocalDateTime.now(); }
    public String getId() { return id; } public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; } public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public College getInstitution() { return institution; } public void setInstitution(College institution) { this.institution = institution; }
    public LocalDateTime getIssuedAt() { return issuedAt; } public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; } public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
