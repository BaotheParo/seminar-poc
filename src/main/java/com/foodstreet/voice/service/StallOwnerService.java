package com.foodstreet.voice.service;

import com.foodstreet.voice.auth.entity.User;
import com.foodstreet.voice.auth.entity.UserRole;
import com.foodstreet.voice.auth.repository.UserRepository;
import com.foodstreet.voice.dto.stall.StallOwnerUpsertRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallUpdate;
import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import com.foodstreet.voice.entity.StallStatus;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.repository.FoodStallUpdateRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StallOwnerService {

    private static final Collection<FoodStallUpdateStatus> PENDING_UPDATE_STATUSES = List.of(
            FoodStallUpdateStatus.PENDING,
            FoodStallUpdateStatus.CREATE_PENDING,
            FoodStallUpdateStatus.UPDATE_PENDING
    );

    private final UserRepository userRepository;
    private final FoodStallRepository foodStallRepository;
    private final FoodStallUpdateRepository foodStallUpdateRepository;

    @Transactional(readOnly = true)
    public FoodStall getMyStall(String username) {
        List<FoodStall> stalls = getMyStalls(username);
        return stalls.isEmpty() ? null : stalls.get(0);
    }

    @Transactional(readOnly = true)
    public List<FoodStall> getMyStalls(String username) {
        User owner = findOwner(username);
        return foodStallRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public Optional<FoodStallUpdate> getLatestPendingUpdateForStall(String username, Long stallId) {
        User owner = findOwner(username);
        return foodStallUpdateRepository.findTopByOwner_IdAndFoodStall_IdAndStatusInOrderByCreatedAtDesc(
                owner.getId(),
                stallId,
                PENDING_UPDATE_STATUSES
        );
    }

    @Transactional(readOnly = true)
    public List<FoodStallUpdate> getPendingCreateRequests(String username) {
        User owner = findOwner(username);
        return foodStallUpdateRepository.findByOwner_IdAndFoodStallIsNullAndStatusInOrderByCreatedAtDesc(
                owner.getId(),
                PENDING_UPDATE_STATUSES
        );
    }

    @Transactional
    public FoodStall submitStallUpdate(String username, StallOwnerUpsertRequest request) {
        User owner = findOwner(username);
        FoodStall stall;
        FoodStallUpdateStatus pendingStatus;

        if (request.getStallId() == null) {
            // New stall registration request:
            // Do NOT create a FoodStall record yet. Only store the request in food_stall_updates.
            stall = null;
            pendingStatus = FoodStallUpdateStatus.CREATE_PENDING;
        } else {
            stall = foodStallRepository.findByIdAndOwnerId(request.getStallId(), owner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stall does not exist for this owner"));

            if (stall.getStatus() == StallStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Your stall update is pending approval. You cannot edit until admin reviews it.");
            }

            stall.setStatus(StallStatus.PENDING);
            foodStallRepository.save(stall);
            pendingStatus = hasApprovedUpdate(stall.getId())
                    ? FoodStallUpdateStatus.UPDATE_PENDING
                    : FoodStallUpdateStatus.CREATE_PENDING;
        }

        Map<String, Object> changes = toChanges(request);
        FoodStallUpdate update = FoodStallUpdate.builder()
                .foodStall(stall)
                .owner(owner)
                .status(pendingStatus)
                .changes(changes)
                .build();
        foodStallUpdateRepository.save(update);

        return stall;
    }

    @Transactional
    public void cancelRequest(String username, Long updateId) {
        User owner = findOwner(username);
        FoodStallUpdate update = foodStallUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!update.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to cancel this request");
        }

        if (!PENDING_UPDATE_STATUSES.contains(update.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can only cancel pending requests");
        }

        // If this is an update request for existing stall, revert stall status back to ACTIVE
        if (update.getFoodStall() != null) {
            FoodStall stall = update.getFoodStall();
            stall.setStatus(StallStatus.ACTIVE);
            foodStallRepository.save(stall);
        }

        update.setStatus(FoodStallUpdateStatus.CANCELLED);
        foodStallUpdateRepository.save(update);
    }

    private boolean hasApprovedUpdate(Long stallId) {
        return foodStallUpdateRepository.existsByFoodStall_IdAndStatus(
                stallId,
                FoodStallUpdateStatus.APPROVED
        );
    }

    private Map<String, Object> toChanges(StallOwnerUpsertRequest request) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("name", request.getName());
        changes.put("description", request.getDescription());
        changes.put("address", request.getAddress());
        changes.put("latitude", request.getLatitude());
        changes.put("longitude", request.getLongitude());
        changes.put("minPrice", request.getMinPrice());
        changes.put("maxPrice", request.getMaxPrice());
        changes.put("triggerRadius", request.getTriggerRadius());
        return changes;
    }

    private User findOwner(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (owner.getRole() != UserRole.RESTAURANT_OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only restaurant owner can access this endpoint");
        }

        return owner;
    }
}
