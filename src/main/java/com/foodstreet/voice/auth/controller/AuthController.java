package com.foodstreet.voice.auth.controller;

import com.foodstreet.voice.auth.dto.LoginRequest;
import com.foodstreet.voice.auth.dto.RefreshTokenRequest;
import com.foodstreet.voice.auth.dto.RegisterRequest;
import com.foodstreet.voice.auth.dto.StallOwnerRegistrationRequest;
import com.foodstreet.voice.auth.dto.TokenResponse;
import com.foodstreet.voice.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication APIs for admin panel")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username/email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/register/status")
    @Operation(summary = "Check whether initial admin registration is still allowed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status fetched")
    })
    public ResponseEntity<Map<String, Object>> registerStatus() {
        boolean canRegister = authService.canRegisterInitialAdmin();
        return ResponseEntity.ok(Map.of(
                "canRegister", canRegister,
                "message", canRegister
                        ? "No admin exists yet. You can register now."
                        : "Admin already exists. Initial register endpoint is now locked."
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Register the first admin account (one-time setup)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "First admin registered"),
            @ApiResponse(responseCode = "403", description = "Admin already exists")
    })
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerInitialAdmin(request));
    }

    @PostMapping("/register-owner")
    @Operation(summary = "Register a new stall owner account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner account registered"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<TokenResponse> registerOwner(@Valid @RequestBody StallOwnerRegistrationRequest request) {
        return ResponseEntity.ok(authService.registerStallOwner(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT access token using refresh token")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user and revoke refresh token")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            authService.logout(authentication.getName());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<Map<String, String>> oauth2GoogleEntry() {
        return ResponseEntity.ok(Map.of("url", "/oauth2/authorization/google"));
    }
}
