package com.foodstreet.voice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "food_stalls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodStall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "trigger_radius", nullable = false)
    @Builder.Default
    private Integer triggerRadius = 15;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "max_price")
    private Integer maxPrice;

    @Column(name = "audio_duration")
    private Integer audioDuration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "featured_reviews", columnDefinition = "jsonb")
    private List<String> featuredReviews;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}