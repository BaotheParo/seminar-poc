package com.foodstreet.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceUpdateRequest {
    private Double latitude;
    private Double longitude;
    private Integer triggerRadius;
}
