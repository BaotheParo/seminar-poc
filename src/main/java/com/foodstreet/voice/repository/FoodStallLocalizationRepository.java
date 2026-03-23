package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.FoodStallLocalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodStallLocalizationRepository extends JpaRepository<FoodStallLocalization, Long> {

    Optional<FoodStallLocalization> findByFoodStallIdAndLanguageCode(Long foodStallId, String languageCode);

    boolean existsByFoodStallIdAndLanguageCode(Long foodStallId, String languageCode);
}
