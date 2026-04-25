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
import com.foodstreet.voice.service.LocalizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApprovalService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final FoodStallUpdateRepository foodStallUpdateRepository;
    private final FoodStallRepository foodStallRepository;
    private final UserRepository userRepository;
    private final LocalizationService localizationService;

    @Transactional(readOnly = true)
    public List<FoodStallUpdateResponse> getPendingApprovals() {
        return foodStallUpdateRepository.findByStatusInOrderByCreatedAtDesc(List.of(
                FoodStallUpdateStatus.CREATE_PENDING,
                FoodStallUpdateStatus.UPDATE_PENDING,
                FoodStallUpdateStatus.PENDING
            ))
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

        if (!isPendingStatus(update.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending update can be approved");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer user not found"));

        FoodStall stall = update.getFoodStall();
        if (stall == null) {
            // Approving a CREATE_PENDING request: create the FoodStall only at approval time.
            stall = createStallFromChanges(update);
            foodStallRepository.save(stall);

            update.setFoodStall(stall);
        } else {
            applyChanges(stall, update.getChanges());
            stall.setStatus(StallStatus.ACTIVE);
            foodStallRepository.save(stall);
        }

        // Ensure Vietnamese localization is updated immediately so UI (default vi) shows latest data on reload.
        // Also regenerate VI audio synchronously so the owner-facing page can play the newest audio right away.
        localizationService.upsertVietnameseFromStall(stall.getId());
        localizationService.generateLocalizationForceAudio(stall.getId(), "vi");

        update.setStatus(FoodStallUpdateStatus.APPROVED);
        update.setReviewedAt(LocalDateTime.now());
        update.setReviewedBy(reviewer);
        update.setReason(null);

        FoodStallUpdate savedUpdate = foodStallUpdateRepository.save(update);

        // After approval, regenerate translations + audio for all supported languages.
        // Run after-commit to avoid the async job reading stale/uncommitted data.
        Long stallId = stall.getId();
        if (stallId != null && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Overwrite mp3 so audio matches the latest approved content.
                    // Skip "vi" as we already did it synchronously for immediate feedback.
                    localizationService.regenerateLanguagesForStall(stallId, List.of("vi"));
                }
            });
        } else if (stallId != null) {
            localizationService.regenerateLanguagesForStall(stallId, List.of("vi"));
        }

        return toResponse(savedUpdate);
    }

    @Transactional
    public FoodStallUpdateResponse reject(Long updateId, String reviewerUsername, String reason) {
        FoodStallUpdate update = foodStallUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Update request not found"));

        if (!isPendingStatus(update.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending update can be rejected");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer user not found"));

        update.setStatus(FoodStallUpdateStatus.REJECTED);
        update.setReason(reason == null ? "" : reason.trim());
        update.setReviewedAt(LocalDateTime.now());
        update.setReviewedBy(reviewer);

        FoodStall stall = update.getFoodStall();
        boolean hasApproved = false;
        if (stall != null) {
            hasApproved = foodStallUpdateRepository.existsByFoodStall_IdAndStatus(
                    stall.getId(),
                    FoodStallUpdateStatus.APPROVED
            );
        }

        FoodStallUpdate savedUpdate = foodStallUpdateRepository.save(update);
        FoodStallUpdateResponse response = toResponse(savedUpdate);

        if (!hasApproved) {
            // New stall registration request rejected:
            // There should be no FoodStall row (new flow). If an older pending row exists, delete it.
            if (stall != null) {
                foodStallRepository.delete(stall);
            }
            return response;
        }

        // Existing stall update rejected: keep stall live and revert status to ACTIVE.
        if (stall != null) {
            stall.setStatus(StallStatus.ACTIVE);
            foodStallRepository.save(stall);
        }
        return response;
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

    private FoodStall createStallFromChanges(FoodStallUpdate update) {
        Map<String, Object> changes = update.getChanges();
        String name = (changes != null && changes.containsKey("name")) ? toStringValue(changes.get("name")) : null;
        if (name == null || name.isBlank()) {
            name = "Pending stall";
        }

        FoodStall stall = FoodStall.builder()
                .name(name)
                .description(changes == null ? null : toStringValue(changes.get("description")))
                .address(changes == null ? null : toStringValue(changes.get("address")))
                .minPrice(changes == null ? null : toIntegerValue(changes.get("minPrice")))
                .maxPrice(changes == null ? null : toIntegerValue(changes.get("maxPrice")))
                .triggerRadius(changes == null ? 15 : (toIntegerValue(changes.get("triggerRadius")) == null ? 15 : toIntegerValue(changes.get("triggerRadius"))))
                .ownerId(update.getOwner() == null ? null : update.getOwner().getId())
                .status(StallStatus.ACTIVE)
                .build();

        Double latitude = changes == null ? null : toDoubleValue(changes.get("latitude"));
        Double longitude = changes == null ? null : toDoubleValue(changes.get("longitude"));
        if (latitude != null && longitude != null) {
            Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
            stall.setLocation(p);
        }

        // Ensure any additional fields are applied consistently.
        applyChanges(stall, changes);
        stall.setStatus(StallStatus.ACTIVE);
        return stall;
    }

    private FoodStallUpdateResponse toResponse(FoodStallUpdate update) {
        boolean isCreatePending = update.getStatus() == FoodStallUpdateStatus.CREATE_PENDING;
        boolean isUpdatePending = update.getStatus() == FoodStallUpdateStatus.UPDATE_PENDING;

        Long stallId = update.getFoodStall() == null ? null : update.getFoodStall().getId();
        boolean hadApprovedBefore = stallId != null && update.getCreatedAt() != null
                && foodStallUpdateRepository.existsByFoodStall_IdAndStatusAndCreatedAtBefore(
                        stallId,
                        FoodStallUpdateStatus.APPROVED,
                        update.getCreatedAt()
                );

        boolean isNewStallRequest = isCreatePending || (!isUpdatePending && !hadApprovedBefore);

        String stallName = null;
        if (update.getFoodStall() != null) {
            stallName = update.getFoodStall().getName();
        } else if (update.getChanges() != null && update.getChanges().containsKey("name")) {
            stallName = toStringValue(update.getChanges().get("name"));
        }

        return FoodStallUpdateResponse.builder()
                .id(update.getId())
                .status(update.getStatus())
                .newStallRequest(isNewStallRequest)
                .createdAt(update.getCreatedAt())
                .reviewedAt(update.getReviewedAt())
                .reason(update.getReason())
                .stallName(stallName)
                .ownerUsername(update.getOwner() == null ? null : update.getOwner().getUsername())
                .changes(update.getChanges())
                .build();
    }

    private boolean isPendingStatus(FoodStallUpdateStatus status) {
        return status == FoodStallUpdateStatus.PENDING
                || status == FoodStallUpdateStatus.CREATE_PENDING
                || status == FoodStallUpdateStatus.UPDATE_PENDING;
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
