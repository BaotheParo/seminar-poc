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

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizationService {

    private final FoodStallRepository foodStallRepository;
    private final FoodStallLocalizationRepository localizationRepository;
    private final TranslationService translationService;
    private final AudioService audioService;

    /**
     * Tu dong tao localization cho tat ca cac ngon ngu ho tro (en, ja, ko, zh).
     * Chay bat dong bo de khong lam cham luong tao quan an.
     */
    @Async
    public void generateAllLanguagesForStall(Long stallId) {
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        log.info("[Localization] Bat dau tu dong tao audio cho {} ngon ngu, stallId={}", languages.length, stallId);
        
        for (String lang : languages) {
            try {
                this.generateLocalization(stallId, lang);
            } catch (Exception e) {
                log.error("[Localization] Loi khi tu dong tao lang={} cho stallId={}: {}", lang, stallId, e.getMessage());
            }
        }
        log.info("[Localization] Hoan thanh tu dong tao audio cho stallId={}", stallId);
    }

    /**
     * Tao hoac cap nhat localization cho mot quan an.
     * Buoc:
     * 1. Lay thong tin tieng Viet (goc)
     * 2. Dich name + description sang targetLang
     * 3. Tao audio MP3 cho ban dich
     * 4. Luu vao bang food_stall_localizations
     *
     * @param stallId    ID cua quan an
     * @param targetLang Ma ngon ngu dich (en, ja, ko, zh)
     * @return AudioUrl cua file MP3 tieng nuoc ngoai
     */
    @Transactional
    public String generateLocalization(Long stallId, String targetLang) {
        log.info("[Localization] Bat dau tao localization stallId={}, lang={}", stallId, targetLang);

        // 1. Lay thong tin goc tieng Viet
        if (stallId == null) throw new IllegalArgumentException("stallId must not be null");
        FoodStall stall = foodStallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan an khong ton tai: " + stallId));

        FoodStallLocalization viLoc = localizationRepository
                .findByFoodStallIdAndLanguageCode(stallId, "vi")
                .orElse(null);

        String sourceName = (viLoc != null && viLoc.getName() != null) ? viLoc.getName() : stall.getName();
        String sourceDesc = (viLoc != null && viLoc.getDescription() != null) ? viLoc.getDescription() : stall.getDescription();

        // 2. Dich sang ngon ngu dich
        String translatedName;
        String translatedDesc;

        if ("vi".equalsIgnoreCase(targetLang)) {
            translatedName = sourceName;
            translatedDesc = sourceDesc;
        } else {
            log.info("[Localization] Dich name + description sang {}", targetLang);
            translatedName = translationService.translate(sourceName, targetLang);
            translatedDesc = translationService.translate(sourceDesc, targetLang);
        }

        // 3. Tao audio MP3
        String audioText = translatedName + ". " + translatedDesc;
        log.info("[Localization] Tao audio lang={}, do dai text={} chars", targetLang, audioText.length());
        @SuppressWarnings("null")
        String audioUrl = audioService.getOrCreateAudioForStall(stallId, audioText, targetLang);

        // 4. Luu hoac cap nhat localization
        FoodStallLocalization localization = localizationRepository
                .findByFoodStallIdAndLanguageCode(stallId, targetLang)
                .orElse(FoodStallLocalization.builder()
                        .foodStall(stall)
                        .languageCode(targetLang)
                        .build());

        localization.setName(translatedName);
        localization.setDescription(translatedDesc);
        localization.setAudioUrl(audioUrl);

        localizationRepository.save(localization);

        log.info("[Localization] Hoan thanh stallId={}, lang={}, audioUrl={}", stallId, targetLang, audioUrl);
        return audioUrl;
    }
}
