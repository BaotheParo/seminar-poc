package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.CreateFoodStallRequest;
import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.GeofenceStallResponse;
import com.foodstreet.voice.dto.projection.GeofenceMatchProjection;
import com.foodstreet.voice.dto.UpdateFoodStallRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallLocalization;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallLocalizationRepository;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.config.AudioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodStallService {

    private final FoodStallRepository foodStallRepository;
    private final FoodStallLocalizationRepository localizationRepository;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String DEFAULT_LANG = "vi";

    @Transactional(readOnly = true)
    public Page<FoodStallResponse> searchStalls(String keyword, Integer minPrice, Integer maxPrice, Double minRating,
            Pageable pageable) {
        log.debug("Searching stalls with keyword={}, minPrice={}, maxPrice={}, minRating={}", keyword, minPrice,
                maxPrice, minRating);

        Specification<FoodStall> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("description")), likeKeyword),
                        cb.like(cb.lower(root.get("address")), likeKeyword)));
            }

            if (minPrice != null) {
                // Stall is relevant if its MAX price is at least the filter's MIN price
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPrice"), minPrice));
            }

            if (maxPrice != null) {
                // Stall is relevant if its MIN price is at most the filter's MAX price
                predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), maxPrice));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return foodStallRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<FoodStallResponse> getAllStalls() {
        log.debug("Lay danh sach tat ca quan an");
        List<FoodStall> stalls = foodStallRepository.findAll();
        log.debug("Da lay duoc {} quan an", stalls.size());
        return stalls.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public FoodStallResponse getStallById(Long id) {
        return getStallByIdWithLang(id, DEFAULT_LANG);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public FoodStallResponse getStallByIdWithLang(Long id, String lang) {
        log.debug("Tim kiem quan an theo id={}, lang={}", id, lang);
        FoodStall stall = foodStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quan an khong ton tai: " + id));

        // Thu tim localization theo lang yeu cau
        String effectiveLang = lang;
        FoodStallLocalization localization = localizationRepository
                .findByFoodStallIdAndLanguageCode(id, lang)
                .orElse(null);

        // Fallback ve tieng Viet neu khong co
        if (localization == null && !DEFAULT_LANG.equals(lang)) {
            log.debug("Khong co localization lang={}, fallback sang vi", lang);
            effectiveLang = DEFAULT_LANG;
            localization = localizationRepository
                    .findByFoodStallIdAndLanguageCode(id, DEFAULT_LANG)
                    .orElse(null);
        }

        return mapToResponseWithLang(stall, localization, effectiveLang);
    }

    @Transactional(readOnly = true)
    public FoodStallResponse findNearestStall(double latitude, double longitude) {
        log.debug("Tim kiem quan an gan nhat tai toa do: lat={}, lon={}", latitude, longitude);

        FoodStall stall = foodStallRepository.findNearestStall(latitude, longitude)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay quan an gan nhat"));

        log.debug("Tim thay quan an gan nhat: {}", stall.getName());

        return mapToResponse(stall);
    }

    @Transactional(readOnly = true)
    public List<GeofenceStallResponse> getGeofenceMatches(double lat, double lng, double radius) {
        log.debug("Get geofence matches lat={}, lng={}, radius={}", lat, lng, radius);
        
        List<GeofenceMatchProjection> projections = foodStallRepository.findGeofenceMatches(lat, lng, radius);
        
        return projections.stream().map(p -> GeofenceStallResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .audioUrl(p.getAudioUrl())
                .triggerRadius(p.getTriggerRadius())
                .priority(p.getPriority())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .distance(p.getDistance())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("null")
    public FoodStallResponse createStall(CreateFoodStallRequest request) {
        log.debug("Tao quan an moi: {}", request.getName());

        Point location = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude()));

        FoodStall stall = FoodStall.builder()
                .name(request.getName())
                .description(request.getDescription())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .location(location)
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .audioDuration(request.getAudioDuration())
                .featuredReviews(request.getFeaturedReviews())
                .build();

        FoodStall savedStall = foodStallRepository.save(stall);
        log.debug("Da tao quan an moi: {}", savedStall.getId());

        return mapToResponse(savedStall);
    }

    @Transactional
    @SuppressWarnings("null")
    public FoodStallResponse updateStall(Long id, UpdateFoodStallRequest request) {
        log.debug("Cap nhat quan an co id: {}", id);

        FoodStall stall = foodStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quan an khong ton tai: " + id));

        if (request.getName() != null) {
            stall.setName(request.getName());
        }
        if (request.getDescription() != null) {
            stall.setDescription(request.getDescription());
        }
        if (request.getAudioUrl() != null) {
            stall.setAudioUrl(request.getAudioUrl());
        }
        if (request.getImageUrl() != null) {
            stall.setImageUrl(request.getImageUrl());
        }
        if (request.getMinPrice() != null) {
            stall.setMinPrice(request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            stall.setMaxPrice(request.getMaxPrice());
        }
        if (request.getAudioDuration() != null) {
            stall.setAudioDuration(request.getAudioDuration());
        }
        if (request.getFeaturedReviews() != null) {
            stall.setFeaturedReviews(request.getFeaturedReviews());
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(
                    new Coordinate(request.getLongitude(), request.getLatitude()));
            stall.setLocation(location);
        }

        FoodStall updatedStall = foodStallRepository.save(stall);
        log.debug("Updated food stall: {}", updatedStall.getName());

        return mapToResponse(updatedStall);
    }

    @Transactional
    public FoodStallResponse updateGeofence(Long id, com.foodstreet.voice.dto.GeofenceUpdateRequest request) {
        log.debug("Updating geofence for food stall with id: {}", id);

        FoodStall stall = foodStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quan an khong ton tai: " + id));

        // Cap nhat vi tri (Anchor Point)
        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(
                    new Coordinate(request.getLongitude(), request.getLatitude()));
            stall.setLocation(location);
            log.debug("Cap nhat vi tri cho quan an {}: {}, {}", id, request.getLatitude(), request.getLongitude());
        }

        // Cap nhat radius
        if (request.getTriggerRadius() != null) {
            stall.setTriggerRadius(request.getTriggerRadius());
            log.debug("Cap nhat radius cho quan an {}: {}", id, request.getTriggerRadius());
        }

        FoodStall updatedStall = foodStallRepository.save(stall);
        return mapToResponse(updatedStall);
    }

    @Transactional
    public int importStalls(List<com.foodstreet.voice.dto.FoodStallImportDto> requests) {
        log.debug("Importing {} quan an", requests.size());
        int count = 0;
        for (com.foodstreet.voice.dto.FoodStallImportDto req : requests) {
            // Kiem tra trung lap theo name
            // Neu co trung lap thi bo qua
            if (foodStallRepository.existsByName(req.getName())) {
                log.debug("Quan an da ton tai: {}", req.getName());
                continue;
            }

            Point location = geometryFactory.createPoint(
                    new Coordinate(req.getLng(), req.getLat()));

            FoodStall stall = FoodStall.builder()
                    .name(req.getName())
                    .address(req.getAddress())
                    .description(req.getDescription())
                    .location(location)
                    .triggerRadius(req.getTriggerRadius() != null ? req.getTriggerRadius() : 15)
                    .audioUrl(req.getAudioUrl())
                    .imageUrl(null)
                    .minPrice(req.getMinPrice())
                    .maxPrice(req.getMaxPrice())
                    .audioDuration(req.getAudioDuration())
                    .featuredReviews(req.getFeaturedReviews())
                    .rating(req.getRating())
                    .build();

            foodStallRepository.save(stall);
            count++;
        }
        log.info("Da import {} quan an moi", count);
        return count;
    }

    @Transactional
    public void deleteStall(Long id) {
        log.debug("Xoa quan an co id: {}", id);

        if (!foodStallRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quan an khong ton tai: " + id);
        }

        foodStallRepository.deleteById(id);
        log.debug("Xoa thanh cong quan an co id: {}", id);
    }

    @Transactional(readOnly = true)
    public com.foodstreet.voice.dto.PackInfoResponse getPackInfo(String lang) {
        String effectiveLang = (lang == null || lang.trim().isEmpty()) ? DEFAULT_LANG : lang;
        String dirPath = "uploads/audio/";
        java.io.File dir = new java.io.File(dirPath);
        int totalFiles = 0;
        long totalSizeBytes = 0;

        if (dir.exists() && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith("_" + effectiveLang + ".mp3"));
            if (files != null) {
                totalFiles = files.length;
                for (java.io.File file : files) {
                    totalSizeBytes += file.length();
                }
            }
        }

        if (totalSizeBytes == 0) {
            totalFiles = 28;
            totalSizeBytes = 28 * 400 * 1024L; // ~11.2MB
        }

        double estimatedSizeMb = totalSizeBytes / (1024.0 * 1024.0);

        java.time.LocalDateTime lastUpdated = foodStallRepository.findMaxCreatedAt()
                .orElse(java.time.LocalDateTime.now());

        return com.foodstreet.voice.dto.PackInfoResponse.builder()
                .language(effectiveLang)
                .totalFiles(totalFiles)
                .totalSizeBytes(totalSizeBytes)
                .estimatedSizeMb(estimatedSizeMb)
                .lastUpdated(lastUpdated)
                .build();
    }

    private FoodStallResponse mapToResponse(FoodStall stall) {
        return mapToResponseWithLang(stall, null, DEFAULT_LANG);
    }

    private FoodStallResponse mapToResponseWithLang(FoodStall stall, FoodStallLocalization loc, String usedLang) {
        String name = (loc != null && loc.getName() != null) ? loc.getName() : stall.getName();
        String description = (loc != null && loc.getDescription() != null) ? loc.getDescription()
                : stall.getDescription();
        String audioUrl = (loc != null && loc.getAudioUrl() != null) ? loc.getAudioUrl() : stall.getAudioUrl();

        return FoodStallResponse.builder()
                .id(stall.getId())
                .name(name)
                .address(stall.getAddress())
                .description(description)
                .audioUrl(audioUrl)
                .imageUrl(stall.getImageUrl())
                .triggerRadius(stall.getTriggerRadius())
                .latitude(stall.getLocation() != null ? stall.getLocation().getY() : null)
                .longitude(stall.getLocation() != null ? stall.getLocation().getX() : null)
                .minPrice(stall.getMinPrice())
                .maxPrice(stall.getMaxPrice())
                .audioDuration(stall.getAudioDuration())
                .featuredReviews(stall.getFeaturedReviews())
                .rating(stall.getRating())
                .usedLanguage(usedLang)
                .build();
    }
}
