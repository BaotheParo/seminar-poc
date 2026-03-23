package com.foodstreet.voice.controller;

import com.foodstreet.voice.service.AudioService;
import com.foodstreet.voice.service.AudioTaskManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Audio Management", description = "APIs for managing audio files, caching and background tasks")
public class AudioAdminController {

    private final AudioService audioService;
    private final AudioTaskManager audioTaskManager;

    // === Audio file management ===

    @GetMapping("/audio")
    @Operation(summary = "List all generated audio files")
    public ResponseEntity<List<String>> listAudioFiles() {
        log.info("Admin request to list all audio files");
        return ResponseEntity.ok(audioService.listAllAudioFiles());
    }

    @DeleteMapping("/audio/{fileName}")
    @Operation(summary = "Delete an audio file from server")
    public ResponseEntity<?> deleteAudioFile(@PathVariable String fileName) {
        log.info("Admin request to delete audio file: {}", fileName);
        boolean deleted = audioService.deleteAudioFile(fileName);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "File not found or could not be deleted"));
        }
    }

    @PostMapping("/audio/regenerate/{stallId}")
    @Operation(summary = "Force regenerate audio for a specific food stall (vi only)")
    public ResponseEntity<?> regenerateAudio(@PathVariable Long stallId) {
        log.info("Admin request to regenerate audio for stall: {}", stallId);
        try {
            String newUrl = audioService.regenerateAudio(stallId);
            return ResponseEntity.ok(Map.of(
                    "message", "Audio regenerated successfully",
                    "newUrl", newUrl));
        } catch (Exception e) {
            log.error("Failed to regenerate audio", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audio/orphaned")
    @Operation(summary = "List audio files not linked to any food stall")
    public ResponseEntity<List<String>> listOrphanedFiles() {
        log.info("Admin request to list orphaned audio files");
        return ResponseEntity.ok(audioService.getOrphanedAudioFiles());
    }

    // === Background Task + SSE ===

    @PostMapping("/audio-tasks/generate-all")
    @Operation(summary = "Start background task to generate audio for ALL stalls in a given language")
    public ResponseEntity<Map<String, String>> generateAll(
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("Admin request to generate all audio, lang={}", lang);
        String taskId = audioTaskManager.createGenerateAllTask(lang);
        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "message", "Task started. Stream progress at /api/v1/admin/audio-tasks/stream/" + taskId,
                "lang", lang
        ));
    }

    @GetMapping(value = "/audio-tasks/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream - realtime progress for a generate-all task")
    public SseEmitter streamTaskProgress(@PathVariable String taskId) {
        log.info("SSE stream requested for taskId={}", taskId);
        return audioTaskManager.getOrCreateEmitter(taskId);
    }
}
