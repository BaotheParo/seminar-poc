package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.FoodStallUpdate;
import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodStallUpdateRepository extends JpaRepository<FoodStallUpdate, Long> {
    List<FoodStallUpdate> findByStatusOrderByCreatedAtDesc(FoodStallUpdateStatus status);

    boolean existsByFoodStall_IdAndStatus(Long foodStallId, FoodStallUpdateStatus status);
}
