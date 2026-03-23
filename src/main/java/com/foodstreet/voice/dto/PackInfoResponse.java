package com.foodstreet.voice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PackInfoResponse {
    private String language;
    private Integer totalFiles;
    private Long totalSizeBytes;
    private Double estimatedSizeMb;
    private LocalDateTime lastUpdated;
}
