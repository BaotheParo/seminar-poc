package com.foodstreet.voice.dto;

import com.foodstreet.voice.entity.UserActivity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {
    @NotBlank(message = "Device ID is required")
    @Schema(description = "Unique device identifier for anonymous tracking", example = "fcm_token_123")
    private String deviceId;

    @Schema(description = "Session identifier", example = "session_abc123")
    private String sessionId;

    @Schema(description = "Device platform (android, ios, etc.)", example = "android")
    private String platform;

    @NotNull(message = "Food Stall ID is required")
    @Schema(description = "ID of the stall the user is interacting with", example = "1")
    private Long stallId;

    @NotNull(message = "Action type is required")
    @Schema(description = "User interaction type", example = "PLAY")
    private UserActivity.ActionType action;


}
