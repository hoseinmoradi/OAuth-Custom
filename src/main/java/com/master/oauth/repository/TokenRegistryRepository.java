package com.master.oauth.repository;

import com.master.oauth.entity.TokenRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRegistryRepository extends JpaRepository<TokenRegistry, Long> {

    Optional<TokenRegistry> findByJti(String jti);

    List<TokenRegistry> findByUsernameOrderByIssuedAtDesc(String username);

    List<TokenRegistry> findByUsernameAndRevokedFalseOrderByIssuedAtDesc(String username);

    @Modifying
    @Query("update TokenRegistry t set t.revoked = true, t.revokedAt = :now where t.jti = :jti and t.revoked = false")
    int revokeByJti(@Param("jti") String jti, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update TokenRegistry t set t.revoked = true, t.revokedAt = :now where t.username = :username and t.revoked = false")
    int revokeAllByUsername(@Param("username") String username, @Param("now") LocalDateTime now);

    boolean existsByJtiAndRevokedTrue(String jti);
}
