package com.foodstreet.voice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_stall_localizations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"food_stall_id", "language_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodStallLocalization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_stall_id", nullable = false)
    private FoodStall foodStall;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode; // vi, en, ja, ko, zh

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
