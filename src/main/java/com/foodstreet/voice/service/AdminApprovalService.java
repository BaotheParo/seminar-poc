package com.foodstreet.voice.service;

import com.foodstreet.voice.auth.entity.User;
import com.foodstreet.voice.auth.repository.UserRepository;
import com.foodstreet.voice.dto.stall.FoodStallUpdateResponse;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallUpdate;
import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import com.foodstreet.voice.entity.StallStatus;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.repository.FoodStallUpdateRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminApprovalService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final FoodStallUpdateRepository foodStallUpdateRepository;
    private final FoodStallRepository foodStallRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FoodStallUpdateResponse> getPendingApprovals() {
        return foodStallUpdateRepository.findByStatusOrderByCreatedAtDesc(FoodStallUpdateStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FoodStallUpdateResponse> getHistory(FoodStallUpdateStatus status) {
        return foodStallUpdateRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FoodStallUpdateResponse approve(Long updateId, String reviewerUsername) {
        FoodStallUpdate update = foodStallUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Update request not found"));

        if (update.getStatus() != FoodStallUpdateStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending update can be approved");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer user not found"));

        FoodStall stall = update.getFoodStall();
        applyChanges(stall, update.getChanges());
        stall.setStatus(StallStatus.ACTIVE);
        foodStallRepository.save(stall);

        update.setStatus(FoodStallUpdateStatus.APPROVED);
        update.setReviewedAt(LocalDateTime.now());
        update.setReviewedBy(reviewer);
        update.setReason(null);

        return toResponse(foodStallUpdateRepository.save(update));
    }

    @Transactional
    public FoodStallUpdateResponse reject(Long updateId, String reviewerUsername, String reason) {
        FoodStallUpdate update = foodStallUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Update request not found"));

        if (update.getStatus() != FoodStallUpdateStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending update can be rejected");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer user not found"));

        update.setStatus(FoodStallUpdateStatus.REJECTED);
        update.setReason(reason == null ? "" : reason.trim());
        update.setReviewedAt(LocalDateTime.now());
        update.setReviewedBy(reviewer);

        FoodStall stall = update.getFoodStall();
        boolean hasApproved = foodStallUpdateRepository.existsByFoodStall_IdAndStatus(
                stall.getId(),
                FoodStallUpdateStatus.APPROVED
        );
        if (!hasApproved) {
            stall.setStatus(StallStatus.INACTIVE);
            foodStallRepository.save(stall);
        }

        return toResponse(foodStallUpdateRepository.save(update));
    }

    private void applyChanges(FoodStall stall, Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        if (changes.containsKey("name")) {
            stall.setName(toStringValue(changes.get("name")));
        }
        if (changes.containsKey("description")) {
            stall.setDescription(toStringValue(changes.get("description")));
        }
        if (changes.containsKey("address")) {
            stall.setAddress(toStringValue(changes.get("address")));
        }
        if (changes.containsKey("minPrice")) {
            stall.setMinPrice(toIntegerValue(changes.get("minPrice")));
        }
        if (changes.containsKey("maxPrice")) {
            stall.setMaxPrice(toIntegerValue(changes.get("maxPrice")));
        }
        if (changes.containsKey("triggerRadius")) {
            Integer radius = toIntegerValue(changes.get("triggerRadius"));
            if (radius != null) {
                stall.setTriggerRadius(radius);
            }
        }

        Double latitude = toDoubleValue(changes.get("latitude"));
        Double longitude = toDoubleValue(changes.get("longitude"));
        if (latitude != null && longitude != null) {
            stall.setLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude)));
        }
    }

    private FoodStallUpdateResponse toResponse(FoodStallUpdate update) {
        return FoodStallUpdateResponse.builder()
                .id(update.getId())
                .status(update.getStatus())
                .createdAt(update.getCreatedAt())
                .reviewedAt(update.getReviewedAt())
                .reason(update.getReason())
                .stallName(update.getFoodStall() == null ? null : update.getFoodStall().getName())
                .ownerUsername(update.getOwner() == null ? null : update.getOwner().getUsername())
                .changes(update.getChanges())
                .build();
    }

    private Integer toIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Double toDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Double.parseDouble(text);
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
