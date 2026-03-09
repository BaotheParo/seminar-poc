package com.foodstreet.voice.dto;

import com.foodstreet.voice.entity.UserActivity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
public class TrackEventRequest {
    @NotBlank(message = "Device ID is required")
    @Schema(description = "Unique device identifier for anonymous tracking", example = "fcm_token_123")
    private String deviceId;

    @NotNull(message = "Food Stall ID is required")
    @Schema(description = "ID of the stall the user is interacting with", example = "1")
    private Long stallId;

    @NotNull(message = "Action type is required")
    @Schema(description = "User interaction type", example = "PLAY")
    private UserActivity.ActionType action;

    @Schema(description = "Duration of play event in seconds", example = "15")
    private Integer duration; // Optional, in seconds
}
