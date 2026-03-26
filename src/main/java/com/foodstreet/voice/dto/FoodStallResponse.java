package com.foodstreet.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FoodStallResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    @Schema(description = "Latitude of the food stall location", example = "10.762622")
    private Double latitude;
    @Schema(description = "Longitude of the food stall location", example = "106.700174")
    private Double longitude;
    private Integer triggerRadius;
    @Schema(description = "URL of the generated audio guide for offline playback", example = "https://storage.example.com/audio/stall_123.mp3")
    private String audioUrl;
    private String imageUrl;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer audioDuration;
    private List<String> featuredReviews;
    private Double rating;
    @Schema(description = "Language actually used for audio/name/description (vi=fallback)", example = "vi")
    private String usedLanguage;
    @Schema(description = "null if translation is available, 'FALLBACK_TO_VI' if the requested language had no localization", example = "FALLBACK_TO_VI")
    private String localizationStatus;
    private Integer priority;
    private String status;
    @Schema(description = "Khoảng cách thực tế (mét) tính từ tọa độ truyền lên", example = "25.0")
    private Double distance;
}
