package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.TrackEventRequest;
import com.foodstreet.voice.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Public - Analytics", description = "APIs for tracking user listening events")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/active-users")
    @Operation(summary = "Count unique active devices in the last N minutes")
    public ResponseEntity<?> getActiveUsers(
            @Parameter(description = "Lookback window in minutes", example = "5")
            @RequestParam(defaultValue = "5") @Min(1) int minutes) {
        long activeUsers = analyticsService.getActiveUsers(minutes);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "minutes", minutes,
                "activeUsers", activeUsers));
    }

    @GetMapping("/poi-ranking")
    @Operation(summary = "Rank POIs by ENTER_REGION and PLAY_AUDIO, with engagement rate (plays/visits)")
    public ResponseEntity<?> getPoiRanking(
            @Parameter(description = "Start date (inclusive), format yyyy-MM-dd", example = "2026-03-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive), format yyyy-MM-dd", example = "2026-03-23")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Maximum number of POIs returned", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int limit) {

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "'from' must be before or equal to 'to'"));
        }

        List<Map<String, Object>> ranking = analyticsService.getPoiRanking(from, to, limit);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "from", from,
                "to", to,
                "limit", limit,
                "count", ranking.size(),
                "data", ranking));
    }

            @GetMapping("/hourly-heatmap")
            @Operation(summary = "Hourly visit distribution by ENTER_REGION", description = "Returns 24 buckets (0-23). Optional stallId filters one POI.")
            public ResponseEntity<?> getHourlyHeatmap(
                @Parameter(description = "Optional stall id to filter a single POI", example = "1")
                @RequestParam(required = false) Long stallId) {

            List<Map<String, Object>> heatmap = analyticsService.getHourlyHeatmap(stallId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("stallId", stallId);
                response.put("count", heatmap.size());
                response.put("data", heatmap);
                return ResponseEntity.ok(response);
            }

            @GetMapping("/audio-engagement")
            @Operation(summary = "Audio engagement by POI (PLAY_AUDIO / ENTER_REGION)", description = "Optional stallId filters one POI.")
            public ResponseEntity<?> getAudioEngagement(
                @Parameter(description = "Optional stall id to filter a single POI", example = "1")
                @RequestParam(required = false) Long stallId) {

            List<Map<String, Object>> engagement = analyticsService.getAudioEngagement(stallId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("stallId", stallId);
                response.put("count", engagement.size());
                response.put("data", engagement);
                return ResponseEntity.ok(response);
            }

            @GetMapping("/session-stats")
            @Operation(summary = "Daily session quality stats", description = "Returns average stalls per session and average session duration (minutes) per day.")
            public ResponseEntity<?> getSessionStats() {
            List<Map<String, Object>> stats = analyticsService.getSessionStats();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", stats.size(),
                "data", stats));
            }

            @GetMapping("/daily-summary")
            @Operation(summary = "Daily summary: users, visits, plays", description = "Aggregates by day in [from, to] using ENTER_REGION for visits and PLAY_AUDIO for plays.")
            public ResponseEntity<?> getDailySummary(
                @Parameter(description = "Start date (inclusive), format yyyy-MM-dd", example = "2026-03-01")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                @Parameter(description = "End date (inclusive), format yyyy-MM-dd", example = "2026-03-23")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

            if (from.isAfter(to)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "'from' must be before or equal to 'to'"));
            }

            List<Map<String, Object>> summary = analyticsService.getDailySummary(from, to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "from", from,
                "to", to,
                "count", summary.size(),
                "data", summary));
            }

    @PostMapping("/track")
    @Operation(summary = "Track user activity (View, Play, Finish Audio)")
    public ResponseEntity<?> trackEvent(@Valid @RequestBody TrackEventRequest request) {
        analyticsService.trackEvent(request);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Event received"));
    }

    @PostMapping("/track/batch")
    @Operation(summary = "Sync multiple tracking events when device comes back online")
    public ResponseEntity<?> trackEventsBatch(@Valid @RequestBody java.util.List<TrackEventRequest> requests) {
        analyticsService.trackEventsBatch(requests);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Batch events received",
                "count", requests.size()));
    }
}
