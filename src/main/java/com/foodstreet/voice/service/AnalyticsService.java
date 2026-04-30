package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.projection.AudioEngagementProjection;
import com.foodstreet.voice.dto.projection.DailySummaryProjection;
import com.foodstreet.voice.dto.projection.HourlyHeatmapProjection;
import com.foodstreet.voice.dto.projection.PoiRankingProjection;
import com.foodstreet.voice.dto.projection.SessionStatsProjection;
import com.foodstreet.voice.dto.DeviceActivityBatchRequest;
import com.foodstreet.voice.dto.StallActivityCount;
import com.foodstreet.voice.dto.TrackEventRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.UserActivity;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserActivityRepository userActivityRepository;
    private final FoodStallRepository foodStallRepository;

    @Transactional(readOnly = true)
    public long getActiveUsers(int minutes) {
        Long count = userActivityRepository.countActiveUsersInLastMinutes(minutes);
        return count != null ? count : 0L;
    }

//poi-ranking
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPoiRanking(LocalDate fromDate, LocalDate toDate, int limit) {
        LocalDateTime fromTime = fromDate.atStartOfDay();
        LocalDateTime toTimeExclusive = toDate.plusDays(1).atStartOfDay();

        List<PoiRankingProjection> rows = userActivityRepository.getPoiRanking(fromTime, toTimeExclusive, limit);
        List<Map<String, Object>> response = new ArrayList<>();

        int rank = 1;
        for (PoiRankingProjection row : rows) {
            long plays = row.getPlays() != null ? row.getPlays() : 0L;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("stallId", row.getStallId());
            item.put("stallName", row.getStallName());
            item.put("plays", plays);
            response.add(item);
        }

        return response;
    }

//hourly-heatmap
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHourlyHeatmap(Long stallId) {
        List<HourlyHeatmapProjection> rows = userActivityRepository.getHourlyHeatmap(stallId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (HourlyHeatmapProjection row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hourOfDay", row.getHourOfDay());
            item.put("visits", row.getVisits() != null ? row.getVisits() : 0L);
            response.add(item);
        }

        return response;
    }

//audio-engagement
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAudioEngagement(Long stallId) {
        List<AudioEngagementProjection> rows = userActivityRepository.getAudioEngagement(stallId);
        List<Map<String, Object>> response = new ArrayList<>();

        int rank = 1;
        for (AudioEngagementProjection row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("stallId", row.getStallId());
            item.put("stallName", row.getStallName());
            item.put("plays", row.getPlays() != null ? row.getPlays() : 0L);
            response.add(item);
        }

        return response;
    }

//session-stats
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSessionStats() {
        List<SessionStatsProjection> rows = userActivityRepository.getSessionStatsByDay();
        List<Map<String, Object>> response = new ArrayList<>();

        for (SessionStatsProjection row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", row.getDay());
            item.put("sessions", row.getSessions() != null ? row.getSessions() : 0L);
            item.put("avgStallsPerSession", row.getAvgStallsPerSession() != null ? row.getAvgStallsPerSession() : BigDecimal.ZERO);
            item.put("avgSessionDurationMinutes", row.getAvgSessionDurationMinutes() != null ? row.getAvgSessionDurationMinutes() : BigDecimal.ZERO);
            response.add(item);
        }

        return response;
    }

//daily-summary
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailySummary(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime fromTime = fromDate.atStartOfDay();
        LocalDateTime toTimeExclusive = toDate.plusDays(1).atStartOfDay();

        List<DailySummaryProjection> rows = userActivityRepository.getDailySummary(fromTime, toTimeExclusive);
        List<Map<String, Object>> response = new ArrayList<>();

        for (DailySummaryProjection row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", row.getDay());
            item.put("users", row.getUsers() != null ? row.getUsers() : 0L);
            item.put("visits", row.getVisits() != null ? row.getVisits() : 0L);
            item.put("plays", row.getPlays() != null ? row.getPlays() : 0L);
            response.add(item);
        }

        return response;
    }

    @Async // Run in background to avoid blocking API response
    @Transactional
    @SuppressWarnings("null")
    public void trackEvent(TrackEventRequest request) {
        log.debug("Tracking event: device={}, action={}, stall={}", request.getDeviceId(), request.getAction(),
                request.getStallId());

        try {
            FoodStall stall = null;
            if (request.getAction() != UserActivity.ActionType.IDLE && request.getStallId() != null) {
                stall = foodStallRepository.findById(request.getStallId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stall not found: " + request.getStallId()));
            }

            UserActivity activity = UserActivity.builder()
                    .deviceId(request.getDeviceId())
                    .sessionId(request.getSessionId())
                    .platform(request.getPlatform())
                    .foodStall(stall)
                    .actionType(request.getAction())
                    .eventTime(LocalDateTime.now())
                    .build();

            userActivityRepository.save(activity);
            log.info("Saved analytics event: {} for stall {}", request.getAction(),
                    stall != null ? stall.getName() : "none");

        } catch (Exception e) {
            log.error("Failed to save analytics event", e);
            // Don't rethrow, just log, so we don't crash the client if analytics fails
        }
    }

    @Async // Run in background to avoid blocking API response
    @Transactional
    public void trackEventsBatch(java.util.List<DeviceActivityBatchRequest> requests) {
        log.info("Processing batch of {} device activity requests in background", requests.size());
        try {
            java.util.List<UserActivity> activities = new java.util.ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (DeviceActivityBatchRequest deviceReq : requests) {
                String deviceId = deviceReq.getDeviceId();
                String sessionId = deviceReq.getSessionId();
                String platform = deviceReq.getPlatform();
                if (deviceReq.getStalls() == null) continue;

                for (StallActivityCount stallReq : deviceReq.getStalls()) {
                    log.info("Processing stall {}: manual={}, auto={}, skip={}, finish={}", 
                            stallReq.getStallId(), stallReq.getManualPlay(), stallReq.getAutoPlay(), stallReq.getSkip(), stallReq.getFinish());
                    FoodStall stall = foodStallRepository.getReferenceById(stallReq.getStallId());

                    unrollAction(activities, deviceId, sessionId, platform, stall, UserActivity.ActionType.PLAY_AUDIO_MANUAL, stallReq.getManualPlay(), now);
                    unrollAction(activities, deviceId, sessionId, platform, stall, UserActivity.ActionType.PLAY_AUDIO_AUTO, stallReq.getAutoPlay(), now);
                    unrollAction(activities, deviceId, sessionId, platform, stall, UserActivity.ActionType.SKIP_AUDIO, stallReq.getSkip(), now);
                    unrollAction(activities, deviceId, sessionId, platform, stall, UserActivity.ActionType.FINISH_AUDIO, stallReq.getFinish(), now);
                }
            }

            userActivityRepository.saveAll(activities);
            log.info("Saved unrolled batch: Total {} individual UserActivity records from {} device requests",
                    activities.size(), requests.size());

        } catch (Exception e) {
            log.error("Failed to save batch analytics events", e);
        }
    }

    private void unrollAction(java.util.List<UserActivity> list, String deviceId, String sessionId, String platform, FoodStall stall,
                             UserActivity.ActionType type, int count, LocalDateTime time) {
        for (int i = 0; i < count; i++) {
            list.add(UserActivity.builder()
                    .deviceId(deviceId)
                    .sessionId(sessionId)
                    .platform(platform)
                    .foodStall(stall)
                    .actionType(type)
                    .eventTime(time)
                    .build());
        }
    }
}
