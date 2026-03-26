package com.foodstreet.voice.dto.stall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StallOwnerUpsertRequest {
    private Long stallId;

    @NotBlank(message = "Stall name is required")
    private String name;

    private String description;
    private String address;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Integer minPrice;
    private Integer maxPrice;
    private Integer triggerRadius;
}
