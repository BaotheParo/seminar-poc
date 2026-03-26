package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.DeviceActivityBatchRequest;
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
        @Operation(summary = "Count unique active devices in the last N minutes", description = "Đếm số thiết bị (deviceId) duy nhất đã gửi sự kiện trong vòng `minutes` phút vừa qua. Dùng để theo dõi lượng người dùng đang hoạt động realtime.")
        public ResponseEntity<?> getActiveUsers(
                        @Parameter(description = "Lookback window in minutes", example = "5") @RequestParam(defaultValue = "5") @Min(1) int minutes) {
                long activeUsers = analyticsService.getActiveUsers(minutes);

                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "minutes", minutes,
                                "activeUsers", activeUsers));
        }

        @GetMapping("/poi-ranking")
        @Operation(summary = "Rank POIs by play-audio count", description = "Xếp hạng các quán ăn (POI) theo tổng số lượt phát audio (`PLAY_AUDIO`, `PLAY_AUDIO_MANUAL`, `PLAY_AUDIO_AUTO`) trong khoảng thời gian `[from, to]`.")
        public ResponseEntity<?> getPoiRanking(
                        @Parameter(description = "Start date (inclusive), format yyyy-MM-dd", example = "2026-03-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "End date (inclusive), format yyyy-MM-dd", example = "2026-03-23") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @Parameter(description = "Maximum number of POIs returned", example = "10") @RequestParam(defaultValue = "10") @Min(1) int limit) {

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
        @Operation(summary = "Hourly visit heatmap (0-23h)", description = "Phân bổ lượt vào vùng (`ENTER_REGION`) theo từng giờ trong ngày (24 bucket từ 0-23). Có thể lọc theo `stallId` để xem heatmap của một quán cụ thể.")
        public ResponseEntity<?> getHourlyHeatmap(
                        @Parameter(description = "Optional stall id to filter a single POI", example = "1") @RequestParam(required = false) Long stallId) {

                List<Map<String, Object>> heatmap = analyticsService.getHourlyHeatmap(stallId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("stallId", stallId);
                response.put("count", heatmap.size());
                response.put("data", heatmap);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/audio-engagement")
        @Operation(summary = "Audio plays by POI", description = "Thống kê tổng số lượt phát audio (`PLAY_AUDIO`, `PLAY_AUDIO_MANUAL`, `PLAY_AUDIO_AUTO`) theo từng quán. Lọc theo `stallId` nếu chỉ muốn xem 1 quán.")
        public ResponseEntity<?> getAudioEngagement(
                        @Parameter(description = "Optional stall id to filter a single POI", example = "1") @RequestParam(required = false) Long stallId) {

                List<Map<String, Object>> engagement = analyticsService.getAudioEngagement(stallId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("stallId", stallId);
                response.put("count", engagement.size());
                response.put("data", engagement);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/session-stats")
        @Operation(summary = "Daily session quality stats", description = "Thống kê chất lượng phiên theo ngày: số quán trung bình mỗi phiên và thời lượng phiên trung bình (phút). Dùng để đánh giá mức độ gắn kết người dùng với ứng dụng.")
        public ResponseEntity<?> getSessionStats() {
                List<Map<String, Object>> stats = analyticsService.getSessionStats();
                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "count", stats.size(),
                                "data", stats));
        }

        @GetMapping("/daily-summary")
        @Operation(summary = "Daily summary: users, visits, plays", description = "Tổng hợp theo ngày trong khoảng `[from, to]`: số người dùng, lượt vào vùng (`ENTER_REGION`) và lượt nghe audio (`PLAY_AUDIO`). Dùng để vẽ biểu đồ xu hướng sử dụng theo ngày.")
        public ResponseEntity<?> getDailySummary(
                        @Parameter(description = "Start date (inclusive), format yyyy-MM-dd", example = "2026-03-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "End date (inclusive), format yyyy-MM-dd", example = "2026-03-23") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

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
        @Operation(summary = "Track a single user event", description = "Ghi nhận một sự kiện người dùng từ Mobile App. Các loại sự kiện hỗ trợ: `ENTER_REGION` (vào vùng quán), `PLAY_AUDIO` (bắt đầu nghe), `FINISH_AUDIO` (nghe xong), `SKIP_AUDIO` (bỏ qua). Body cần có `stallId`, `action` (loại sự kiện), `deviceId`.")
        public ResponseEntity<?> trackEvent(@Valid @RequestBody TrackEventRequest request) {
                analyticsService.trackEvent(request);

                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message", "Event received"));
        }

        @PostMapping("/track/batch")
        @Operation(summary = "Batch sync events (aggregated payload)", description = "Đồng bộ hàng loạt sự kiện gộp từ Frontend khi đóng app. "
                        +
                        "Payload gồm danh sách `DeviceActivityBatchRequest`, mỗi request chứa `deviceId` và số lượng `play/skip/finish` theo từng `stallId`.")
        public ResponseEntity<?> trackEventsBatch(
                        @Valid @RequestBody java.util.List<DeviceActivityBatchRequest> requests) {
                analyticsService.trackEventsBatch(requests);

                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message", "Batch aggregated events received",
                                "devicesCount", requests.size()));
        }
}
