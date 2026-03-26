package com.foodstreet.voice.auth.security;

import com.foodstreet.voice.auth.config.AuthProperties;
import com.foodstreet.voice.auth.dto.TokenResponse;
import com.foodstreet.voice.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Lazy
    private final AuthService authService;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String providerUserId = String.valueOf(principal.getAttributes().getOrDefault("sub", principal.getName()));
        String email = String.valueOf(principal.getAttributes().getOrDefault("email", ""));
        String name = String.valueOf(principal.getAttributes().getOrDefault("name", ""));

        TokenResponse tokenResponse = authService.loginWithOAuth2(provider, providerUserId, email, name);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(authProperties.getOauth2SuccessRedirect())
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("refreshToken", tokenResponse.getRefreshToken())
                .queryParam("tokenType", tokenResponse.getTokenType())
                .queryParam("expiresIn", tokenResponse.getExpiresIn())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
