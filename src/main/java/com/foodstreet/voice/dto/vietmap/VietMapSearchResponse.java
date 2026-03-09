package com.foodstreet.voice.dto.vietmap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

public record VietMapSearchResponse(
        @JsonProperty("ref_id") String refId,
        String name,
        String address,
        Double lat,
        Double lng) {
}
