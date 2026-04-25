package com.foodstreet.voice.service;

import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallLocalization;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallLocalizationRepository;
import com.foodstreet.voice.repository.FoodStallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizationService {

    private final FoodStallRepository foodStallRepository;
    private final FoodStallLocalizationRepository localizationRepository;
    private final TranslationService translationService;
    private final AudioService audioService;

    /**
     * Upsert nhanh ban tieng Viet (vi) tu food_stalls vao food_stall_localizations.
     * Dung de UI nhan du lieu moi ngay sau khi admin approve, truoc khi background translate/audio hoan tat.
     */
    @Transactional
    public void upsertVietnameseFromStall(Long stallId) {
        if (stallId == null) return;

        FoodStall stall = foodStallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan an khong ton tai: " + stallId));

        FoodStallLocalization loc = localizationRepository
                .findByFoodStall_IdAndLanguageCode(stallId, "vi")
                .orElse(FoodStallLocalization.builder()
                        .foodStall(stall)
                        .languageCode("vi")
                        .build());

        loc.setName(stall.getName());
        loc.setDescription(stall.getDescription());
        loc.setAddress(stall.getAddress());

        // Keep VI localization audio in sync with the canonical audio_url stored on food_stalls.
        String stallAudioUrl = (stall.getAudioUrl() != null && !stall.getAudioUrl().isBlank())
            ? stall.getAudioUrl()
            : "/audio/" + stallId + "_vi.mp3";
        loc.setAudioUrl(stallAudioUrl);

        localizationRepository.save(loc);
    }

    /**
     * Translate-on-Create: Tao localization + audio cho tat ca ngon ngu ngay khi FoodStall duoc tao.
     * Chay bat dong bo (@Async) de API tra ve 201 ngay lap tuc.
     *
     * @param savedStall Entity FoodStall vua duoc persist thanh cong
     */
    /**
     * Translate-on-Create: Tao localization + audio cho tat ca ngon ngu ngay khi FoodStall duoc tao.
     * Chay bat dong bo (@Async) de API tra ve 201 ngay lap tuc.
     *
     * @param savedStall Entity FoodStall vua duoc persist thanh cong
     */
    @Async
    public void processLocalizationAndAudioInBackground(FoodStall savedStall) {
        Long stallId = savedStall.getId();
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        log.info("[Localization] [Async] Bat dau xu ly localization cho stallId={}, {} ngon ngu", stallId, languages.length);

        for (String lang : languages) {
            try {
                // Use the centralized internal method to avoid duplicating translation/audio/save logic.
                // We use forceAudio=false here to allow caching if a file already exists (e.g. hash-based).
                this.generateLocalizationInternal(stallId, lang, false);
            } catch (Exception e) {
                log.error("[Localization] [{}] Loi khi xu ly stallId={}: {}", lang, stallId, e.getMessage());
            }
        }
        log.info("[Localization] [Async] Hoan thanh tat ca ngon ngu cho stallId={}", stallId);
    }

    /**
     * Sync All Localizations: Quet toan bo FoodStall trong DB de tim ban dich con thieu.
     */
    public Map<String, Object> syncAllMissingLocalizations() {
        String[] allLangs = {"vi", "en", "ja", "ko", "zh"};
        int totalLangCount = allLangs.length;

        List<FoodStall> allStalls = foodStallRepository.findAll();
        List<FoodStallLocalization> allLocs = localizationRepository.findAll();
        Map<Long, Long> locCountByStall = allLocs.stream()
                .collect(Collectors.groupingBy(
                        loc -> loc.getFoodStall().getId(),
                        Collectors.counting()
                ));

        int needsSync = 0;
        int alreadyComplete = 0;

        for (FoodStall stall : allStalls) {
            long existingLangCount = locCountByStall.getOrDefault(stall.getId(), 0L);
            if (existingLangCount < totalLangCount) {
                processLocalizationAndAudioInBackground(stall);
                needsSync++;
            } else {
                alreadyComplete++;
            }
        }

        return Map.of(
                "totalStalls", allStalls.size(),
                "queuedForSync", needsSync,
                "alreadyComplete", alreadyComplete
        );
    }

    /**
     * Tu dong tao localization cho tat ca cac ngon ngu ho tro.
     */
    @Async
    public void generateAllLanguagesForStall(Long stallId) {
        generateAllLanguagesForStall(stallId, false);
    }

    @Async
    public void generateAllLanguagesForStall(Long stallId, boolean forceRegenerate) {
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        for (String lang : languages) {
            try {
                this.generateLocalizationInternal(stallId, lang, forceRegenerate);
            } catch (Exception e) {
                log.error("[Localization] Loi khi tu dong tao lang={} cho stallId={}: {}", lang, stallId, e.getMessage());
            }
        }
    }

    @Async
    public void regenerateAllLanguagesForStall(Long stallId) {
        this.regenerateLanguagesForStall(stallId, List.of());
    }

    /**
     * Force-regenerate audio files for specific languages (with timestamp).
     * @param stallId ID of the stall
     * @param excludeLanguages List of language codes to skip
     */
    @Async
    public void regenerateLanguagesForStall(Long stallId, List<String> excludeLanguages) {
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        log.info("[Localization] Force regenerate audio (with timestamp) cho stallId={}, excluding={}", stallId, excludeLanguages);

        for (String lang : languages) {
            if (excludeLanguages.contains(lang)) continue;
            try {
                this.generateLocalizationForceAudio(stallId, lang);
            } catch (Exception e) {
                log.error("[Localization] Force regenerate loi lang={} stallId={}: {}", lang, stallId, e.getMessage());
            }
        }
    }

    @Transactional
    public String generateLocalization(Long stallId, String targetLang) {
        return generateLocalizationInternal(stallId, targetLang, false);
    }

    @Transactional
    public String generateLocalizationForceAudio(Long stallId, String targetLang) {
        return generateLocalizationInternal(stallId, targetLang, true);
    }

    private String generateLocalizationInternal(Long stallId, String targetLang, boolean forceAudio) {
        log.info("[Localization] Process stallId={}, lang={}, forceAudio={}", stallId, targetLang, forceAudio);

        if (stallId == null) throw new IllegalArgumentException("stallId must not be null");
        FoodStall stall = foodStallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan an khong ton tai: " + stallId));

        FoodStallLocalization viLoc = localizationRepository
                .findByFoodStall_IdAndLanguageCode(stallId, "vi")
                .orElse(null);

        // source-of-truth from latest FoodStall fields
        String sourceName = stall.getName();
        if (sourceName == null || sourceName.isBlank()) {
            sourceName = (viLoc != null) ? viLoc.getName() : "";
        }

        String sourceDesc = stall.getDescription();
        if (sourceDesc == null || sourceDesc.isBlank()) {
            sourceDesc = (viLoc != null) ? viLoc.getDescription() : "";
        }

        // 2. Translation
        String translatedName;
        String translatedDesc;
        String translatedAddress;

        if ("vi".equalsIgnoreCase(targetLang)) {
            translatedName = sourceName;
            translatedDesc = sourceDesc;
            translatedAddress = stall.getAddress();
        } else {
            translatedName = translationService.translate(sourceName, targetLang);
            translatedDesc = translationService.translate(sourceDesc, targetLang);
            translatedAddress = translationService.translate(stall.getAddress(), targetLang);
        }

        // 3. Audio Generation
        String audioText = translatedName + ". " + translatedDesc;
        String audioUrl = forceAudio
            ? audioService.generateVersionedAudioForStall(stallId, audioText, targetLang)
            : audioService.getOrCreateAudioForStall(stallId, audioText, targetLang);

        // 4. Persistence
        FoodStallLocalization localization = localizationRepository
                .findByFoodStall_IdAndLanguageCode(stallId, targetLang)
                .orElse(FoodStallLocalization.builder()
                        .foodStall(stall)
                        .languageCode(targetLang)
                        .build());

        localization.setName(translatedName);
        localization.setDescription(translatedDesc);
        localization.setAddress(translatedAddress);
        localization.setAudioUrl(audioUrl);

        localizationRepository.save(localization);

        if ("vi".equalsIgnoreCase(targetLang) && audioUrl != null && !audioUrl.isBlank()) {
            stall.setAudioUrl(audioUrl);
            foodStallRepository.save(stall);
        }

        return audioUrl;
    }
}
