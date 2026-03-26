package com.foodstreet.voice.auth.service;

import com.foodstreet.voice.auth.config.AuthProperties;
import com.foodstreet.voice.auth.entity.User;
import com.foodstreet.voice.auth.entity.UserRole;
import com.foodstreet.voice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!authProperties.isBootstrapEnabled()) {
            log.info("Admin bootstrap seeding is disabled");
            return;
        }

        if (!userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
            log.info("Admin users already exist. Skipping bootstrap seeding");
            return;
        }

        String username = authProperties.getBootstrapUsername();
        String email = authProperties.getBootstrapEmail();
        String password = authProperties.getBootstrapPassword();

        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            log.warn("Bootstrap admin config is incomplete, skip creating default admin user");
            return;
        }

        User admin = User.builder()
                .username(username.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.warn("Created bootstrap admin user '{}' with email '{}'. Change password immediately.", admin.getUsername(), admin.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
