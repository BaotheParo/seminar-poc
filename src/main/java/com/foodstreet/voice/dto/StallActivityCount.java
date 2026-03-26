package com.foodstreet.voice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StallActivityCount {

    @Schema(description = "ID of the food stall", example = "1")
    private Long stallId;

    @Schema(description = "Number of manual play events", example = "2")
    private int manualPlay;

    @Schema(description = "Number of automatic play events", example = "1")
    private int autoPlay;

    @Schema(description = "Number of skip events", example = "1")
    private int skip;

    @Schema(description = "Number of finish events", example = "1")
    private int finish;
}
