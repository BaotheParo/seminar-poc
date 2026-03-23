package com.foodstreet.voice.service;

import com.foodstreet.voice.repository.FoodStallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioTaskManager {

    private final FoodStallRepository foodStallRepository;
    private final LocalizationService localizationService;

    // Toi da 3 task chay song song
    private final Semaphore semaphore = new Semaphore(3);

    // Luu SseEmitter theo taskId
    private final ConcurrentHashMap<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    // Tao taskId moi va bat dau task bat dong bo
    public String createGenerateAllTask(String lang) {
        String taskId = UUID.randomUUID().toString();
        List<Long> stallIds = foodStallRepository.findAll()
                .stream()
                .map(s -> s.getId())
                .toList();

        log.info("[AudioTask] Created taskId={}, {} stalls, lang={}", taskId, stallIds.size(), lang);
        generateAllAsync(taskId, stallIds, lang);
        return taskId;
    }

    public SseEmitter getOrCreateEmitter(String taskId) {
        // Timeout 10 phut cho SSE
        return emitterMap.computeIfAbsent(taskId, id -> new SseEmitter(10 * 60 * 1000L));
    }

    @Async
    public void generateAllAsync(String taskId, List<Long> stallIds, String lang) {
        SseEmitter emitter = getOrCreateEmitter(taskId);
        int total = stallIds.size();
        int done = 0;
        int failed = 0;

        try {
            sendEvent(emitter, taskId, "start", "Bat dau tao audio cho " + total + " quan an, lang=" + lang);

            for (Long stallId : stallIds) {
                semaphore.acquire();
                try {
                    String audioUrl = localizationService.generateLocalization(stallId, lang);
                    done++;
                    int progress = (int) ((done + failed) * 100.0 / total);
                    sendEvent(emitter, taskId, "progress",
                            String.format("{\"stallId\":%d,\"audioUrl\":\"%s\",\"done\":%d,\"total\":%d,\"progress\":%d}",
                                    stallId, audioUrl, done, total, progress));
                } catch (Exception e) {
                    failed++;
                    log.warn("[AudioTask] stallId={} failed: {}", stallId, e.getMessage());
                    sendEvent(emitter, taskId, "error",
                            String.format("{\"stallId\":%d,\"error\":\"%s\"}", stallId, e.getMessage()));
                } finally {
                    semaphore.release();
                }
            }

            sendEvent(emitter, taskId, "complete",
                    String.format("{\"done\":%d,\"failed\":%d,\"total\":%d}", done, failed, total));
            emitter.complete();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[AudioTask] taskId={} interrupted", taskId);
            emitter.completeWithError(e);
        } finally {
            emitterMap.remove(taskId);
        }
    }

    @SuppressWarnings("null")
    private void sendEvent(SseEmitter emitter, String taskId, String event, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(taskId)
                    .name(event)
                    .data(data));
        } catch (IOException e) {
            log.warn("[AudioTask] SSE send failed for taskId={}: {}", taskId, e.getMessage());
        }
    }
}
