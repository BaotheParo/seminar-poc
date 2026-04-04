package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.FoodStallUpdate;
import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodStallUpdateRepository extends JpaRepository<FoodStallUpdate, Long> {
    List<FoodStallUpdate> findByStatusOrderByCreatedAtDesc(FoodStallUpdateStatus status);

    List<FoodStallUpdate> findByStatusInOrderByCreatedAtDesc(Collection<FoodStallUpdateStatus> statuses);

    boolean existsByFoodStall_IdAndStatus(Long foodStallId, FoodStallUpdateStatus status);

    boolean existsByFoodStall_IdAndStatusAndCreatedAtBefore(
            Long foodStallId,
            FoodStallUpdateStatus status,
            LocalDateTime createdAt
    );

        Optional<FoodStallUpdate> findTopByOwner_IdAndFoodStall_IdAndStatusInOrderByCreatedAtDesc(
            Long ownerId,
            Long foodStallId,
            Collection<FoodStallUpdateStatus> statuses
        );

        List<FoodStallUpdate> findByOwner_IdAndFoodStallIsNullAndStatusInOrderByCreatedAtDesc(
            Long ownerId,
            Collection<FoodStallUpdateStatus> statuses
        );
}
