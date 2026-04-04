package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.FoodStallLocalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodStallLocalizationRepository extends JpaRepository<FoodStallLocalization, Long> {

    Optional<FoodStallLocalization> findByFoodStall_IdAndLanguageCode(Long foodStallId, String languageCode);

    List<FoodStallLocalization> findAllByLanguageCodeAndFoodStall_IdIn(String languageCode, Collection<Long> foodStallIds);
    List<FoodStallLocalization> findAllByFoodStall_Id(Long foodStallId);
    boolean existsByFoodStall_IdAndLanguageCode(Long foodStallId, String languageCode);
}
