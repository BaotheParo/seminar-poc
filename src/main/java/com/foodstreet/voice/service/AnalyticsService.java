package com.foodstreet.voice.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserActivityRepository userActivityRepository;
    private final FoodStallRepository foodStallRepository;

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
