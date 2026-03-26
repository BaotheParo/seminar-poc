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
    private final com.foodstreet.voice.service.LocalizationService localizationService;

    @org.springframework.web.bind.annotation.PatchMapping("/stores/{id}/geofence")
    @Operation(summary = "Update geofence for a stall", description = "Cập nhật `triggerRadius` (bán kính kích hoạt tính bằng mét) và/hoặc toạ độ của quán ăn. Dùng khi cần tinh chỉnh vung geofence trên bản đồ mà không cần sửa toàn bộ thông tin quán.")
    public ResponseEntity<com.foodstreet.voice.dto.FoodStallResponse> updateGeofence(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody com.foodstreet.voice.dto.GeofenceUpdateRequest request) {
        log.info("Received request to update geofence for stall id: {}", id);
        com.foodstreet.voice.dto.FoodStallResponse stall = foodStallService.updateGeofence(id, request);
        log.info("Updated geofence for stall: {}", stall.getName());
        return ResponseEntity.ok(stall);
    }

    /**
     * Quet toan bo FoodStall, tim cac quan chua co du ban dich (vi/en/ja/ko/zh)
     * va dong bo da ngon ngu trong background. API tra ve bao cao ngay lap tuc.
     */
    @PostMapping("/sync-localizations")
    @Operation(summary = "Backfill multi-language localizations for all stalls", description = "Queues background translation+TTS jobs for every stall that is missing any of the 5 supported languages (vi, en, ja, ko, zh). Returns immediately with a progress report.")
    public ResponseEntity<?> syncAllLocalizations() {
        log.info("[Admin] Nhan lenh dong bo da ngon ngu toan bo quan an");
        java.util.Map<String, Object> report = localizationService.syncAllMissingLocalizations();
        return ResponseEntity.accepted().body(report);
    }

    @PostMapping("/sync-vietmap")
    @Operation(summary = "Sync food stalls from VietMap API", description = "Gọi VietMap Places API để tìm kiếm địa điểm gần toạ độ `lat/lng` theo `keyword` và import các quán mới vào DB. "
            +
            "Quán trùng tên sẽ bỏ qua. Sau khi sync, gọi `POST /sync-localizations` để backfill bản dịch đa ngôn ngữ.")
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
