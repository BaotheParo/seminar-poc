package com.foodstreet.voice.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SessionStatsProjection {
    LocalDate getDay();

    Long getSessions();

    BigDecimal getAvgStallsPerSession();

    BigDecimal getAvgSessionDurationMinutes();
}
