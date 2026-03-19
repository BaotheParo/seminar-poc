package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.CreateFoodStallRequest;
import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.NearbyRequest;
import com.foodstreet.voice.dto.UpdateFoodStallRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.service.AudioService;
import com.foodstreet.voice.service.FoodStallService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stalls")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Public - Food Stalls", description = "APIs for Mobile App to fetch locations and audio data")
public class FoodStallController {

    private final FoodStallService foodStallService;

    @GetMapping("/search")
    @Operation(summary = "Search and filter food stalls with pagination")
    public ResponseEntity<Page<FoodStallResponse>> searchStalls(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Double minRating,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Received request to search stalls: keyword={}, minPrice={}, maxPrice={}, minRating={}",
                keyword, minPrice, maxPrice, minRating);
        Page<FoodStallResponse> results = foodStallService.searchStalls(keyword, minPrice, maxPrice, minRating,
                pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "Get all food stalls")
    public ResponseEntity<List<FoodStallResponse>> getAllStalls() {
        log.info("Da nhan request de lay tat ca cac quan an");
        List<FoodStallResponse> stalls = foodStallService.getAllStalls();
        log.info("Returning {} food stalls", stalls.size());
        return ResponseEntity.ok(stalls);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a food stall by ID")
    public ResponseEntity<FoodStallResponse> getStallById(@PathVariable Long id) {
        log.info("Da nhan request de lay quan an co id: {}", id);
        FoodStallResponse stall = foodStallService.getStallById(id);
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
    public ResponseEntity<FoodStallResponse> findNearestStall(@Valid @ModelAttribute NearbyRequest request) {
        log.info("Da nhan request de tim quan an gan nhat: lat={}, lon={}", request.getLat(), request.getLon());

        FoodStallResponse response = foodStallService.findNearestStall(
                request.getLat(),
                request.getLon());

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

            // Lazy gen
            if (res.getAudioUrl() == null || res.getAudioUrl().isEmpty()) {
                String audioUrl = audioService.getOrCreateAudio(
                        "Xin chao day la " + stall.getName() + ". " + stall.getDescription(),
                        "vi");
                res.setAudioUrl(audioUrl);
            }
            return res;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
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