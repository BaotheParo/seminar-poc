package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByDeviceId(String deviceId);

    List<UserActivity> findByFoodStallId(Long foodStallId);
}
