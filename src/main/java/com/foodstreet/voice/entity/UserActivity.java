package com.foodstreet.voice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_stall_id", nullable = false)
    private FoodStall foodStall;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        VIEW_DETAILS,
        PLAY_AUDIO,
        STOP_AUDIO,
        FINISH_AUDIO,
        ENTER_REGION,
        AUTO_PLAY
    }
}
