package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.projection.AudioEngagementProjection;
import com.foodstreet.voice.dto.projection.DailySummaryProjection;
import com.foodstreet.voice.dto.projection.HourlyHeatmapProjection;
import com.foodstreet.voice.dto.projection.PoiRankingProjection;
import com.foodstreet.voice.dto.projection.SessionStatsProjection;
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
            long visits = row.getVisits() != null ? row.getVisits() : 0L;
            long plays = row.getPlays() != null ? row.getPlays() : 0L;
            double engagementRate = visits == 0 ? 0.0 : Math.round(((double) plays / visits) * 10000.0) / 10000.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("stallId", row.getStallId());
            item.put("stallName", row.getStallName());
            item.put("visits", visits);
            item.put("plays", plays);
            item.put("engagementRate", engagementRate);
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
            item.put("enters", row.getEnters() != null ? row.getEnters() : 0L);
            item.put("plays", row.getPlays() != null ? row.getPlays() : 0L);

            BigDecimal engagementRate = row.getEngagementRate() != null ? row.getEngagementRate() : BigDecimal.ZERO;
            item.put("engagementRate", engagementRate);
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
            FoodStall stall = foodStallRepository.findById(request.getStallId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stall not found: " + request.getStallId()));

            UserActivity activity = UserActivity.builder()
                    .deviceId(request.getDeviceId())
                    .foodStall(stall)
                    .actionType(request.getAction())
                    .durationSeconds(request.getDuration())
                    .build();

            userActivityRepository.save(activity);
            log.info("Saved analytics event: {} for stall {}", request.getAction(), stall.getName());

        } catch (Exception e) {
            log.error("Failed to save analytics event", e);
            // Don't rethrow, just log, so we don't crash the client if analytics fails
        }
    }

    @Async // Run in background to avoid blocking API response
    @Transactional
    public void trackEventsBatch(java.util.List<TrackEventRequest> requests) {
        log.debug("Tracking batch of {} events", requests.size());

        try {
            java.util.List<UserActivity> activities = new java.util.ArrayList<>();

            for (TrackEventRequest request : requests) {
                FoodStall stall = foodStallRepository.getReferenceById(request.getStallId());

                UserActivity activity = UserActivity.builder()
                        .deviceId(request.getDeviceId())
                        .foodStall(stall)
                        .actionType(request.getAction())
                        .durationSeconds(request.getDuration())
                        .build();

                activities.add(activity);
            }

            userActivityRepository.saveAll(activities);
            log.info("Saved batch of {} analytics events", activities.size());

        } catch (Exception e) {
            log.error("Failed to save batch analytics events", e);
        }
    }
}
