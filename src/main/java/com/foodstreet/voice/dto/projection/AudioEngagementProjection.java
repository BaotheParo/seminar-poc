package com.foodstreet.voice.dto.projection;

import java.math.BigDecimal;

public interface AudioEngagementProjection {
    Long getStallId();

    String getStallName();

    Long getEnters();

    Long getPlays();

    BigDecimal getEngagementRate();
}
