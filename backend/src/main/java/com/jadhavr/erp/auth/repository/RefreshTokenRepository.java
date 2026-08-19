package com.jadhavr.erp.auth.repository;

import com.jadhavr.erp.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken t
            set t.revoked = true
            where t.tokenHash = :tokenHash
              and t.revoked = false
            """)
    int consume(String tokenHash);
    @Modifying
    @Query("""
            update RefreshToken t
            set t.revoked = true
            where t.user.id = :userId
              and t.tokenHash = :tokenHash
              and t.revoked = false
            """)
    int revokeCurrentSession(Long userId, String tokenHash);
    @Modifying @Query("update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
    int revokeAllForUser(Long userId);
}
