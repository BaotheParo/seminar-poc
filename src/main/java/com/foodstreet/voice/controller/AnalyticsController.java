package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.TrackEventRequest;
import com.foodstreet.voice.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public - Analytics", description = "APIs for tracking user listening events")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    @Operation(summary = "Track user activity (View, Play, Finish Audio)")
    public ResponseEntity<?> trackEvent(@Valid @RequestBody TrackEventRequest request) {
        analyticsService.trackEvent(request);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Event received"));
    }
}
