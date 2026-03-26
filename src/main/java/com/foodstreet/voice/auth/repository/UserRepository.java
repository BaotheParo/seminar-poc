package com.foodstreet.voice.auth.repository;

import com.foodstreet.voice.auth.entity.User;
import com.foodstreet.voice.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByOauthProviderAndOauthProviderUserId(String provider, String providerUserId);

    Optional<User> findByRefreshTokenHash(String refreshTokenHash);

    // Find all admins
    List<User> findByRole(UserRole role);

    // Find restaurant owner by restaurant ID
    Optional<User> findByRestaurantId(Long restaurantId);

    // Find all restaurant owners
    List<User> findAllByRole(UserRole role);
}
