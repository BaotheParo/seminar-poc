package com.foodstreet.voice.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "user_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "platform")
    private String platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_stall_id")
    private FoodStall foodStall;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    private LocalDateTime eventTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        IDLE,
        PLAY_AUDIO, // Keeping for backward compatibility
        PLAY_AUDIO_MANUAL,
        PLAY_AUDIO_AUTO,
        SKIP_AUDIO,
        FINISH_AUDIO,
        ENTER_REGION;

        @JsonCreator
        public static ActionType fromValue(String value) {
            if (value == null) {
                return null;
            }
            return ActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }

        @JsonValue
        public String toValue() {
            return name();
        }
    }
}
