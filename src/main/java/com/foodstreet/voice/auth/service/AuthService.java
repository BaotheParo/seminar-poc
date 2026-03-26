package com.foodstreet.voice.auth.service;

import com.foodstreet.voice.auth.config.AuthProperties;
import com.foodstreet.voice.auth.dto.LoginRequest;
import com.foodstreet.voice.auth.dto.RegisterRequest;
import com.foodstreet.voice.auth.dto.StallOwnerRegistrationRequest;
import com.foodstreet.voice.auth.dto.TokenResponse;
import com.foodstreet.voice.auth.dto.UserSummary;
import com.foodstreet.voice.auth.entity.User;
import com.foodstreet.voice.auth.entity.UserRole;
import com.foodstreet.voice.auth.repository.UserRepository;
import com.foodstreet.voice.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public boolean canRegisterInitialAdmin() {
        return userRepository.findByRole(UserRole.ADMIN).isEmpty();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationConfiguration.getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse registerInitialAdmin(RegisterRequest request) {
        // Only allow if no admin exists
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (!admins.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account already exists");
        }

        String username = normalizeUsername(request.getUsername());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        User saved = userRepository.save(newUser);
        return issueTokens(saved);
    }

    @Transactional
    public TokenResponse registerStallOwner(StallOwnerRegistrationRequest request) {
        String username = normalizeUsername(request.getUsername());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User owner = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.RESTAURANT_OWNER)
                .enabled(true)
                .build();

        User saved = userRepository.save(owner);
        return issueTokens(saved);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        String tokenHash = sha256(refreshToken);
        User user = userRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse loginWithOAuth2(String provider, String providerUserId, String email, String name) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth2 account does not provide email");
        }

        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        String normalizedEmail = email.toLowerCase(Locale.ROOT).trim();

        User user = userRepository
                .findByOauthProviderAndOauthProviderUserId(normalizedProvider, providerUserId)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElseGet(() -> createOAuthUser(normalizedProvider, providerUserId, normalizedEmail, name));

        user.setOauthProvider(normalizedProvider);
        user.setOauthProviderUserId(providerUserId);
        user.setEnabled(true);

        return issueTokens(user);
    }

    private User createOAuthUser(String provider, String providerUserId, String email, String name) {
        Set<String> whitelist = authProperties.getAdminWhitelistEmailSet();
        if (!whitelist.isEmpty() && !whitelist.contains(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email is not allowed for admin access");
        }

        String usernameBase = (name == null || name.isBlank()) ? email.split("@")[0] : name;
        String username = normalizeUsername(usernameBase);

        int suffix = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = normalizeUsername(usernameBase) + suffix;
            suffix++;
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.ADMIN)
                .enabled(true)
                .oauthProvider(provider)
                .oauthProviderUserId(providerUserId)
                .build();

        return userRepository.save(newUser);
    }

    private String normalizeUsername(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.isBlank()) {
            return "admin";
        }
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String refreshTokenHash = sha256(rawRefreshToken);

        user.setRefreshTokenHash(refreshTokenHash);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(authProperties.getRefreshTokenExpirationDays()));
        userRepository.save(user);

        UserSummary userSummary = UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(List.of(user.getRole().toString()))
                .build();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(authProperties.getAccessTokenExpirationMinutes() * 60)
                .user(userSummary)
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot hash token", ex);
        }
    }
}
