package com.foodstreet.voice.controller;

import com.foodstreet.voice.service.VietMapSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Data Management", description = "Internal APIs for syncing and importing POI data")
public class AdminSyncController {

    private final VietMapSyncService vietMapSyncService;
    private final com.foodstreet.voice.service.FoodStallService foodStallService;

    @org.springframework.web.bind.annotation.PatchMapping("/stores/{id}/geofence")
    @Operation(summary = "Update geofence for a stall")
    public ResponseEntity<com.foodstreet.voice.dto.FoodStallResponse> updateGeofence(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody com.foodstreet.voice.dto.GeofenceUpdateRequest request) {
        log.info("Received request to update geofence for stall id: {}", id);
        com.foodstreet.voice.dto.FoodStallResponse stall = foodStallService.updateGeofence(id, request);
        log.info("Updated geofence for stall: {}", stall.getName());
        return ResponseEntity.ok(stall);
    }

    @PostMapping("/sync-vietmap")
    @Operation(summary = "Sync data from VietMap")
    public ResponseEntity<?> syncVietMapData(@RequestBody Map<String, Object> payload) {
        try {
            // Extract params with defaults
            double lat = payload.containsKey("lat") ? ((Number) payload.get("lat")).doubleValue() : 10.760;
            double lng = payload.containsKey("lng") ? ((Number) payload.get("lng")).doubleValue() : 106.700;
            String keyword = (String) payload.getOrDefault("keyword", "quán ăn");

            log.info("Received admin sync request: lat={}, lng={}, keyword={}", lat, lng, keyword);

            int count = vietMapSyncService.syncStallsFromVietMap(lat, lng, keyword);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "saved_count", count,
                    "message", "Synced " + count + " items from VietMap"));
        } catch (Exception e) {
            log.error("Sync failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }
}
