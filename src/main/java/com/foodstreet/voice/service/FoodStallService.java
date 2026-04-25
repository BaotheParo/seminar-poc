package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.CreateFoodStallRequest;
import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.LocalizationResponse;
import com.foodstreet.voice.dto.GeofenceStallResponse;
import com.foodstreet.voice.dto.projection.GeofenceMatchProjection;
import com.foodstreet.voice.dto.UpdateFoodStallRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallLocalization;
import com.foodstreet.voice.entity.StallStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodStallService {

    private final FoodStallRepository foodStallRepository;
    private final FoodStallLocalizationRepository localizationRepository;
    private final LocalizationService localizationService;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String DEFAULT_LANG = "vi";

    @Transactional(readOnly = true)
    public Page<FoodStallResponse> searchStalls(String keyword, Integer minPrice, Integer maxPrice, Double minRating,
            String lang, Pageable pageable) {
        log.debug("Searching stalls with keyword={}, minPrice={}, maxPrice={}, minRating={}, lang={}", keyword, minPrice,
                maxPrice, minRating, lang);

        Specification<FoodStall> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.equal(root.get("status"), StallStatus.ACTIVE),
                    cb.isNull(root.get("status"))
            ));

            if (keyword != null && !keyword.isEmpty()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("description")), likeKeyword),
                        cb.like(cb.lower(root.get("address")), likeKeyword)));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), maxPrice));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FoodStall> stallPage = foodStallRepository.findAll(spec, pageable);
        List<Long> stallIds = stallPage.getContent().stream().map(FoodStall::getId).toList();
        
        // Fetch localizations in bulk
        Map<Long, FoodStallLocalization> locMap = fetchLocalizationMap(stallIds, lang);

        return stallPage.map(stall -> mapToResponseWithLang(stall, locMap.get(stall.getId()), lang));
    }

    @Transactional(readOnly = true)
    public List<FoodStallResponse> getAllStalls(String lang) {
        log.debug("Lay danh sach tat ca quan an, lang={}", lang);
        List<FoodStall> stalls = foodStallRepository.findAll().stream()
            .filter(stall -> stall.getStatus() == null || stall.getStatus() == StallStatus.ACTIVE)
            .sorted(java.util.Comparator.comparing(FoodStall::getId))
            .toList();
        List<Long> stallIds = stalls.stream().map(FoodStall::getId).toList();

        // Fetch localizations in bulk
        Map<Long, FoodStallLocalization> locMap = fetchLocalizationMap(stallIds, lang);

        return stalls.stream()
                .map(stall -> mapToResponseWithLang(stall, locMap.get(stall.getId()), lang))
                .collect(Collectors.toList());
    }

    private Map<Long, FoodStallLocalization> fetchLocalizationMap(List<Long> stallIds, String lang) {
        if (stallIds.isEmpty()) return Collections.emptyMap();
        
        List<FoodStallLocalization> locs = localizationRepository.findAllByLanguageCodeAndFoodStall_IdIn(lang, stallIds);
        
        // Neu khong phai tieng Viet va bi thieu, fallback ve tieng Viet
        if (!DEFAULT_LANG.equals(lang) && locs.size() < stallIds.size()) {
            List<FoodStallLocalization> viLocs = localizationRepository.findAllByLanguageCodeAndFoodStall_IdIn(DEFAULT_LANG, stallIds);
            Map<Long, FoodStallLocalization> viMap = viLocs.stream()
                .collect(Collectors.toMap(l -> l.getFoodStall().getId(), l -> l, (a, b) -> a));
            
            Map<Long, FoodStallLocalization> targetMap = locs.stream()
                .collect(Collectors.toMap(l -> l.getFoodStall().getId(), l -> l, (a, b) -> a));
            
            // Merge: target ghi de vi
            viMap.putAll(targetMap);
            return viMap;
        }

        return locs.stream().collect(Collectors.toMap(l -> l.getFoodStall().getId(), l -> l, (a, b) -> a));
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
                .findByFoodStall_IdAndLanguageCode(id, lang)
                .orElse(null);

        // Fallback ve tieng Viet neu khong co
        if (localization == null && !DEFAULT_LANG.equals(lang)) {
            log.debug("Khong co localization lang={}, fallback sang vi", lang);
            effectiveLang = DEFAULT_LANG;
            localization = localizationRepository
                    .findByFoodStall_IdAndLanguageCode(id, DEFAULT_LANG)
                    .orElse(null);
        }

        FoodStallResponse response = mapToResponseWithLang(stall, localization, effectiveLang);


        return response;
    }

    @Transactional(readOnly = true)
    public FoodStallResponse findNearestStall(double latitude, double longitude, String lang) {
        log.debug("Tim kiem quan an gan nhat tai toa do: lat={}, lon={}, lang={}", latitude, longitude, lang);

        FoodStall stall = foodStallRepository.findNearestStall(latitude, longitude)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay quan an gan nhat"));

        log.debug("Tim thay quan an gan nhat: {}", stall.getName());

        return getStallByIdWithLang(stall.getId(), lang);
    }

    @Transactional(readOnly = true)
    public List<GeofenceStallResponse> getGeofenceMatches(double lat, double lng, double radius, String lang) {
        log.debug("Get geofence matches lat={}, lng={}, radius={}, lang={}", lat, lng, radius, lang);
        
        List<GeofenceMatchProjection> projections = foodStallRepository.findGeofenceMatches(lat, lng, radius);
        List<Long> stallIds = projections.stream().map(GeofenceMatchProjection::getId).collect(Collectors.toList());
        
        Map<Long, FoodStallLocalization> locMap = fetchLocalizationMap(stallIds, lang);
        
        return projections.stream().map(p -> {
            FoodStallLocalization loc = locMap.get(p.getId());
            
            String name = (loc != null && loc.getName() != null) ? loc.getName() : p.getName();
            String description = (loc != null && loc.getDescription() != null) ? loc.getDescription() : p.getDescription();
            String address = (loc != null && loc.getAddress() != null) ? loc.getAddress() : p.getAddress();
            
            String actualLang = (loc != null && loc.getLanguageCode() != null) ? loc.getLanguageCode() : DEFAULT_LANG;
            String audioUrl = (p.getAudioUrl() != null && !p.getAudioUrl().isBlank())
                    ? p.getAudioUrl()
                    : "/audio/" + p.getId() + "_" + DEFAULT_LANG + ".mp3";
            
            String localizationStatus = (p.getLocalizationStatus() != null) ? p.getLocalizationStatus() :
                                        ((lang != null && !DEFAULT_LANG.equals(lang) && !lang.equals(actualLang)) 
                                        ? "FALLBACK_TO_VI" : null);

            return GeofenceStallResponse.builder()
                .id(p.getId())
                .name(name)
                .description(description)
                .address(address)
                .audioUrl(audioUrl)
                .triggerRadius(p.getTriggerRadius())
                .priority(p.getPriority())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .distance(p.getDistance())
                .usedLanguage(actualLang)
                .localizationStatus(localizationStatus)
                .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("null")
    public FoodStallResponse createStall(CreateFoodStallRequest request) {
        log.debug("Tao quan an moi: {}", request.getName());

        Point location = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude()));

        FoodStall stall = FoodStall.builder()
                .name(request.getName())
                .address(request.getAddress())
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

        // Translate-on-Create: dich, tao audio, luu localization cho ca 5 ngon ngu (chay ngam)
        localizationService.processLocalizationAndAudioInBackground(savedStall);

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

        // Neu ten hoac mo ta thay doi (hoac gia su la vay), cap nhat lai audio da ngon ngu
        localizationService.generateAllLanguagesForStall(updatedStall.getId(), true);

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

            FoodStall savedStall = foodStallRepository.save(stall);
            // Tu dong sinh audio cho tung quan duoc import
            localizationService.generateAllLanguagesForStall(savedStall.getId());
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

    public org.springframework.core.io.Resource exportAudioPack(String lang) throws java.io.IOException {
        String effectiveLang = (lang == null || lang.trim().isEmpty()) ? DEFAULT_LANG : lang;
        return generateZipResource((d, name) -> name.endsWith("_" + effectiveLang + ".mp3"));
    }

    public org.springframework.core.io.Resource exportAllAudio() throws java.io.IOException {
        return generateZipResource((d, name) -> name.endsWith(".mp3"));
    }

    public org.springframework.core.io.Resource exportStallAudio(Long id) throws java.io.IOException {
        log.debug("Exporting all audio for stallId={}", id);
        String prefix = id + "_";
        return generateZipResource((d, name) -> name.startsWith(prefix) && name.endsWith(".mp3"));
    }

    private org.springframework.core.io.Resource generateZipResource(java.io.FilenameFilter filter) throws java.io.IOException {
        java.io.File dir = new java.io.File("uploads/audio/");
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ResourceNotFoundException("Audio directory not found.");
        }

        java.io.File[] files = dir.listFiles(filter);
        if (files == null || files.length == 0) {
            throw new ResourceNotFoundException("No audio files found for the requested pack.");
        }

        java.nio.file.Path tempZipFile = java.nio.file.Files.createTempFile("audio_pack_", ".zip");
        
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(tempZipFile.toFile()))) {
            for (java.io.File file : files) {
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    byte[] buffer = new byte[1024 * 4];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
        
        return new org.springframework.core.io.FileSystemResource(tempZipFile.toFile());
    }

    private FoodStallResponse mapToResponse(FoodStall stall) {
        return mapToResponseWithLang(stall, null, DEFAULT_LANG);
    }

    private FoodStallResponse mapToResponseWithLang(FoodStall stall, FoodStallLocalization loc, String requestedLang) {
        String name = (loc != null && loc.getName() != null) ? loc.getName() : stall.getName();
        String description = (loc != null && loc.getDescription() != null) ? loc.getDescription()
                : stall.getDescription();
        String address = (loc != null && loc.getAddress() != null) ? loc.getAddress() : stall.getAddress();

        // Tinh toan ngon ngu thuc te duoc su dung
        String actualLang = (loc != null && loc.getLanguageCode() != null) ? loc.getLanguageCode()
                : DEFAULT_LANG;
        String audioUrl = (stall.getAudioUrl() != null && !stall.getAudioUrl().isBlank())
            ? stall.getAudioUrl()
            : "/audio/" + stall.getId() + "_" + DEFAULT_LANG + ".mp3";

        // Neu ngon ngu thuc te khac ngon ngu yeu cau => da fallback ve tieng Viet
        String localizationStatus = null;
        if (requestedLang != null && !requestedLang.equals(DEFAULT_LANG) && !requestedLang.equals(actualLang)) {
            localizationStatus = "FALLBACK_TO_VI";
        }

        return FoodStallResponse.builder()
                .id(stall.getId())
                .name(name)
                .address(address)
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
                .priority(stall.getPriority())
                .usedLanguage(actualLang)
                .localizationStatus(stall.getLocalizationStatus() != null ? stall.getLocalizationStatus() : localizationStatus)
                .status(stall.getStatus() == null ? null : stall.getStatus().name())
                .build();
    }
}
