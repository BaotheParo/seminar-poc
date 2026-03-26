package com.foodstreet.voice.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app.auth")
@Data
public class AuthProperties {
    private String jwtSecret;
    private long accessTokenExpirationMinutes = 15;
    private long refreshTokenExpirationDays = 7;
    private String oauth2SuccessRedirect = "http://localhost:5173/login/callback";
    private String adminWhitelistEmails = "";
    private boolean bootstrapEnabled = false;
    private String bootstrapUsername = "";
    private String bootstrapEmail = "";
    private String bootstrapPassword = "";
    private String bootstrapRole = "ADMIN";

    public Set<String> getAdminWhitelistEmailSet() {
        if (adminWhitelistEmails == null || adminWhitelistEmails.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(adminWhitelistEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
