package com.foodstreet.voice.dto.vietmap;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VietMapPlaceResponse(
        @JsonProperty("ref_id") String refId,
        String name,
        String address,
        Double lat,
        Double lng) {
}
