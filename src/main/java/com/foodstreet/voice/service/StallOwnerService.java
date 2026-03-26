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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StallOwnerService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final UserRepository userRepository;
    private final FoodStallRepository foodStallRepository;
    private final FoodStallUpdateRepository foodStallUpdateRepository;

    @Transactional(readOnly = true)
    public FoodStall getMyStall(String username) {
        User owner = findOwner(username);
        if (owner.getRestaurantId() == null) {
            return null;
        }

        return foodStallRepository.findByIdAndOwnerId(owner.getRestaurantId(), owner.getId())
                .orElse(null);
    }

    @Transactional
    public FoodStall submitStallUpdate(String username, StallOwnerUpsertRequest request) {
        User owner = findOwner(username);
        FoodStall stall;

        if (owner.getRestaurantId() == null) {
            stall = createPendingStall(owner, request);
            owner.setRestaurantId(stall.getId());
            userRepository.save(owner);
        } else {
            stall = foodStallRepository.findByIdAndOwnerId(owner.getRestaurantId(), owner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stall does not exist for this owner"));
            stall.setStatus(StallStatus.PENDING);
            foodStallRepository.save(stall);
        }

        Map<String, Object> changes = toChanges(request);
        FoodStallUpdate update = FoodStallUpdate.builder()
                .foodStall(stall)
                .owner(owner)
                .status(FoodStallUpdateStatus.PENDING)
                .changes(changes)
                .build();
        foodStallUpdateRepository.save(update);

        return stall;
    }

    private FoodStall createPendingStall(User owner, StallOwnerUpsertRequest request) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));

        FoodStall stall = FoodStall.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .location(location)
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .triggerRadius(request.getTriggerRadius() == null ? 15 : request.getTriggerRadius())
                .ownerId(owner.getId())
                .status(StallStatus.PENDING)
                .build();

        return foodStallRepository.save(stall);
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
