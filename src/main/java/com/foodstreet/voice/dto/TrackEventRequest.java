package com.foodstreet.voice.dto;

import com.foodstreet.voice.entity.UserActivity;
import jakarta.validation.constraints.AssertTrue;
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

    @Schema(description = "Session identifier", example = "session_abc123")
    private String sessionId;

    @Schema(description = "Device platform (android, ios, etc.)", example = "android")
    private String platform;

    @Schema(description = "ID of the stall the user is interacting with. Optional when action is IDLE.", example = "1")
    private Long stallId;

    @NotNull(message = "Action type is required")
    @Schema(description = "User interaction type. Use IDLE to indicate the user is online in the app.", example = "IDLE")
    private UserActivity.ActionType action;

    @AssertTrue(message = "Food Stall ID is required unless action is IDLE")
    @Schema(hidden = true)
    public boolean isStallIdValidForAction() {
        return action == null || action == UserActivity.ActionType.IDLE || stallId != null;
    }
}
