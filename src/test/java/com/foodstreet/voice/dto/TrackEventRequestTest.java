package com.foodstreet.voice.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodstreet.voice.entity.UserActivity;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackEventRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldParseIdleActionCaseInsensitively() throws Exception {
        TrackEventRequest request = objectMapper.readValue("""
                {
                  "deviceId": "device-1",
                  "action": "Idle"
                }
                """, TrackEventRequest.class);

        assertThat(request.getAction()).isEqualTo(UserActivity.ActionType.IDLE);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRequireStallIdForNonIdleAction() {
        TrackEventRequest request = TrackEventRequest.builder()
                .deviceId("device-1")
                .action(UserActivity.ActionType.PLAY_AUDIO)
                .build();

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().equals("Food Stall ID is required unless action is IDLE"));
    }
}
