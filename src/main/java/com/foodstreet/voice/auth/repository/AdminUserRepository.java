package com.foodstreet.voice.auth.repository;

import com.foodstreet.voice.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    Optional<AdminUser> findByUsernameOrEmail(String username, String email);

    Optional<AdminUser> findByOauthProviderAndOauthProviderUserId(String provider, String providerUserId);

    Optional<AdminUser> findByRefreshTokenHash(String refreshTokenHash);
}
