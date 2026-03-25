package com.foodstreet.voice.dto.projection;

import java.time.LocalDate;

public interface DailySummaryProjection {
    LocalDate getDay();

    Long getUsers();

    Long getVisits();

    Long getPlays();
}
