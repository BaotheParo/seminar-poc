package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.CreateFoodStallRequest;
import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.UpdateFoodStallRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodStallService {

    private final FoodStallRepository foodStallRepository;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

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
    public FoodStallResponse getStallById(Long id) {
        log.debug("Tim kiem quan an theo id: {}", id);
        FoodStall stall = foodStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quan an khong ton tai: " + id));
        return mapToResponse(stall);
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

    @Transactional
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

    private FoodStallResponse mapToResponse(FoodStall stall) {
        return FoodStallResponse.builder()
                .id(stall.getId())
                .name(stall.getName())
                .address(stall.getAddress())
                .description(stall.getDescription())
                .audioUrl(stall.getAudioUrl())
                .imageUrl(stall.getImageUrl())
                .triggerRadius(stall.getTriggerRadius())
                .latitude(stall.getLocation() != null ? stall.getLocation().getY() : null)
                .longitude(stall.getLocation() != null ? stall.getLocation().getX() : null)
                .minPrice(stall.getMinPrice())
                .maxPrice(stall.getMaxPrice())
                .audioDuration(stall.getAudioDuration())
                .featuredReviews(stall.getFeaturedReviews())
                .rating(stall.getRating())
                .build();
    }
}