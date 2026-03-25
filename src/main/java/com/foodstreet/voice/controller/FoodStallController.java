package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.CreateFoodStallRequest;
import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.GeofenceStallResponse;
import com.foodstreet.voice.dto.NearbyRequest;
import com.foodstreet.voice.dto.UpdateFoodStallRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.service.AudioService;
import com.foodstreet.voice.service.FoodStallService;
import com.foodstreet.voice.service.LocalizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stalls")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Public - Food Stalls", description = "APIs for Mobile App to fetch locations and audio data")
public class FoodStallController {

    private final FoodStallService foodStallService;
    private final LocalizationService localizationService;

    @GetMapping("/search")
    @Operation(summary = "Search and filter food stalls with pagination")
    public ResponseEntity<Page<FoodStallResponse>> searchStalls(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "vi") String lang,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Received request to search stalls: keyword={}, minPrice={}, maxPrice={}, minRating={}, lang={}",
                keyword, minPrice, maxPrice, minRating, lang);
        Page<FoodStallResponse> results = foodStallService.searchStalls(keyword, minPrice, maxPrice, minRating,
                lang, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/geofence")
    @Operation(summary = "Get food stalls within geofence radius, ordered by priority and distance",
               description = "API dành cho Mobile App (Flutter) để quét danh sách các quán ăn xung quanh vị trí hiện tại của người dùng. Trả về tối đa 5 quán, ưu tiên quán có priority cao trước, sau đó mới xét đến khoảng cách.")
    public ResponseEntity<List<GeofenceStallResponse>> getGeofenceMatches(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vĩ độ (Latitude) hiện tại của người dùng", example = "10.762622") @RequestParam double lat,
            @io.swagger.v3.oas.annotations.Parameter(description = "Kinh độ (Longitude) hiện tại của người dùng", example = "106.700174") @RequestParam double lng,
            @io.swagger.v3.oas.annotations.Parameter(description = "Bán kính quét để lọc quán (mặc định 50 mét)", example = "50.0") @RequestParam(defaultValue = "50.0") double radius) {
        log.info("Received request for geofence matches: lat={}, lng={}, radius={}", lat, lng, radius);
        List<GeofenceStallResponse> results = foodStallService.getGeofenceMatches(lat, lng, radius);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "Get all food stalls")
    public ResponseEntity<List<FoodStallResponse>> getAllStalls(@RequestParam(defaultValue = "vi") String lang) {
        log.info("Da nhan request de lay tat ca cac quan an, lang={}", lang);
        List<FoodStallResponse> stalls = foodStallService.getAllStalls(lang);
        log.info("Returning {} food stalls", stalls.size());
        return ResponseEntity.ok(stalls);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a food stall by ID with optional language (fallback to vi)")
    public ResponseEntity<FoodStallResponse> getStallById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("Da nhan request de lay quan an co id: {}, lang={}", id, lang);
        FoodStallResponse stall = foodStallService.getStallByIdWithLang(id, lang);
        return ResponseEntity.ok(stall);
    }

    @GetMapping("/{id}/audio")
    @Operation(summary = "Get audio URL for a specific food stall")
    public ResponseEntity<?> getAudioByStallId(@PathVariable Long id) {
        log.info("Nhan request lay audio cho quan an id: {}", id);
        FoodStallResponse stall = foodStallService.getStallById(id);
        String audioUrl = stall.getAudioUrl();
        if (audioUrl == null || audioUrl.isBlank()) {
            return ResponseEntity.ok(java.util.Map.of(
                "id", id,
                "name", stall.getName(),
                "audioUrl", "",
                "message", "Quan nay chua co audio. Vui long goi API /sync truoc."
            ));
        }
        return ResponseEntity.ok(java.util.Map.of(
            "id", id,
            "name", stall.getName(),
            "audioUrl", audioUrl,
            "audioDuration", stall.getAudioDuration() != null ? stall.getAudioDuration() : 0
        ));
    }


    @GetMapping("/nearby")
    @Operation(summary = "Find the nearest food stall")
    public ResponseEntity<FoodStallResponse> findNearestStall(
            @Valid @ModelAttribute NearbyRequest request,
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("Da nhan request de tim quan an gan nhat: lat={}, lon={}, lang={}", request.getLat(), request.getLon(), lang);

        FoodStallResponse response = foodStallService.findNearestStall(
                request.getLat(),
                request.getLon(),
                lang);

        log.info("Da tra ve quan an gan nhat: {}", response.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new food stall")
    public ResponseEntity<FoodStallResponse> createStall(@Valid @RequestBody CreateFoodStallRequest request) {
        log.info("Da nhan request de tao quan an moi: {}", request.getName());
        FoodStallResponse stall = foodStallService.createStall(request);
        log.info("Da tao quan an moi: {}", stall.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(stall);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing food stall")
    public ResponseEntity<FoodStallResponse> updateStall(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFoodStallRequest request) {
        log.info("Da nhan request de cap nhat quan an co id: {}", id);
        FoodStallResponse stall = foodStallService.updateStall(id, request);
        log.info("Cap nhat quan an: {}", stall.getName());
        return ResponseEntity.ok(stall);
    }

    @PostMapping("/{id}/audio/generate")
    @Operation(summary = "On-demand: generate/regenerate audio for a food stall in a specific language")
    public ResponseEntity<?> generateAudioOnDemand(
            @PathVariable Long id,
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("On-demand audio generate: stallId={}, lang={}", id, lang);
        try {
            String audioUrl = localizationService.generateLocalization(id, lang);
            return ResponseEntity.ok(Map.of(
                    "audioUrl", audioUrl,
                    "language", lang,
                    "cached", false
            ));
        } catch (Exception e) {
            log.error("Failed to generate audio on-demand for stallId={}, lang={}: {}", id, lang, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/audio/generate-all")
    @Operation(summary = "On-demand: generate/regenerate audio for ALL supported languages for a specific food stall")
    public ResponseEntity<?> generateAllAudioForStallOnDemand(@PathVariable Long id) {
        log.info("On-demand ALL languages audio generate: stallId={}", id);
        try {
            localizationService.generateAllLanguagesForStall(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Phát dẫn đa ngôn ngữ đang được tạo ngầm cho stallId=" + id,
                    "languages", List.of("en", "ja", "ko", "zh")
            ));
        } catch (Exception e) {
            log.error("Failed to generate ALL audio on-demand for stallId={}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a food stall")
    public ResponseEntity<Void> deleteStall(@PathVariable Long id) {
        log.info("Da nhan request de xoa quan an co id: {}", id);
        foodStallService.deleteStall(id);
        log.info("Xoa quan an co id: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Autowired
    private AudioService audioService;

    @Autowired
    private FoodStallRepository foodStallRepository;

    // API Sync cho Mobile (Offline)
    @GetMapping("/sync")
    @Operation(summary = "Sync food stall data for mobile app (offline mode)")
    public ResponseEntity<List<FoodStallResponse>> syncDataForMobile(
            @RequestParam(defaultValue = "10.762622") double lat, // Toa do Q4
            @RequestParam(defaultValue = "106.700174") double lng,
            @RequestParam(defaultValue = "2000") double radius) { // R=2km

        // Lay tat ca cac quan Q4 voi R=2km
        List<FoodStall> stalls = foodStallRepository.findStallsWithinRadius(lat, lng, radius);

        // Mapping
        List<FoodStallResponse> response = stalls.stream().map(stall -> {
            FoodStallResponse res = convertToResponse(stall);

            // Lazy generation check
            if (res.getAudioUrl() == null || res.getAudioUrl().isEmpty() || res.getAudioUrl().equals("null")) {
                String audioText = "Xin chào, đây là " + stall.getName() + ". " + stall.getDescription();
                String audioUrl = audioService.getOrCreateAudioForStall(stall.getId(), audioText, "vi");
                res.setAudioUrl(audioUrl);
            }
            return res;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pack-info")
    @Operation(summary = "Get audio pack info and latest data version for offline downloading")
    public ResponseEntity<com.foodstreet.voice.dto.PackInfoResponse> getPackInfo(
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("Received request for pack info with lang={}", lang);
        return ResponseEntity.ok(foodStallService.getPackInfo(lang));
    }

    @GetMapping("/audio/download-pack")
    @Operation(summary = "Download a ZIP package containing all audio files for a specific language")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAudioPack(
            @RequestParam(defaultValue = "vi") String lang) {
        log.info("Received request to download audio pack for lang={}", lang);
        try {
            org.springframework.core.io.Resource zipResource = foodStallService.exportAudioPack(lang);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audio_pack_" + lang + ".zip\"")
                    .contentType(org.springframework.http.MediaType.valueOf("application/zip"))
                    .body(zipResource);
        } catch (com.foodstreet.voice.exception.ResourceNotFoundException e) {
            log.warn("Audio pack not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (java.io.IOException e) {
            log.error("Error creating audio pack ZIP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/audio/download-all")
    @Operation(summary = "Download a ZIP package containing all audio files across all languages for a specific stall")
    public ResponseEntity<org.springframework.core.io.Resource> downloadStallAudioPack(@PathVariable Long id) {
        log.info("Received request to download all audio files for stallId={}", id);
        try {
            org.springframework.core.io.Resource zipResource = foodStallService.exportStallAudio(id);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stall_" + id + "_all_audio.zip\"")
                    .contentType(org.springframework.http.MediaType.valueOf("application/zip"))
                    .body(zipResource);
        } catch (com.foodstreet.voice.exception.ResourceNotFoundException e) {
            log.warn("No audio files found for stallId={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (java.io.IOException e) {
            log.error("Error creating stall audio pack ZIP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/audio/download-all")
    @Operation(summary = "Download a ZIP package containing all audio files across all languages")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAllAudio() {
        log.info("Received request to download all audio files");
        try {
            org.springframework.core.io.Resource zipResource = foodStallService.exportAllAudio();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"street_voice_full_audio.zip\"")
                    .contentType(org.springframework.http.MediaType.valueOf("application/zip"))
                    .body(zipResource);
        } catch (com.foodstreet.voice.exception.ResourceNotFoundException e) {
            log.warn("Audio directory is empty: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (java.io.IOException e) {
            log.error("Error creating total audio pack ZIP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private FoodStallResponse convertToResponse(FoodStall stall) {
        FoodStallResponse response = new FoodStallResponse();

        response.setId(stall.getId());
        response.setName(stall.getName());
        response.setAddress(stall.getAddress());
        response.setDescription(stall.getDescription());

        // Map path
        response.setAudioUrl(stall.getAudioUrl());
        response.setImageUrl(stall.getImageUrl());
        response.setTriggerRadius(stall.getTriggerRadius());

        // Map new fields
        response.setMinPrice(stall.getMinPrice());
        response.setMaxPrice(stall.getMaxPrice());
        response.setAudioDuration(stall.getAudioDuration());
        response.setFeaturedReviews(stall.getFeaturedReviews());

        // QUAN TRỌNG: Chuyển đổi tọa độ từ PostGIS (Point) sang Lat/Lng
        // Vì Mobile App (Flutter/React Native) chỉ hiểu Lat/Lng, không hiểu Geometry
        // Object
        if (stall.getLocation() != null) {
            response.setLatitude(stall.getLocation().getY()); // Y là Vĩ độ (Lat)
            response.setLongitude(stall.getLocation().getX()); // X là Kinh độ (Lng)
        }
        return response;
    }

}