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
import java.util.Set;
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
     * Nhan thang entity da duoc luu, tranh phat sinh them 1 query DB.
     * Chay bat dong bo (@Async) de API tra ve 201 ngay lap tuc.
     *
     * @param savedStall Entity FoodStall vua duoc persist thanh cong
     */
    @Async
    public void processLocalizationAndAudioInBackground(FoodStall savedStall) {
        Long stallId = savedStall.getId();
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        log.info("[Localization] [Async] Bat dau xu ly localization cho stallId={}, {} ngon ngu", stallId, languages.length);

        String sourceName = savedStall.getName();
        String sourceDesc = savedStall.getDescription();
        String sourceAddress = savedStall.getAddress();

        for (String lang : languages) {
            try {
                String translatedName;
                String translatedDesc;
                String translatedAddress;

                // Translation Phase
                if (lang.equals("vi")) {
                    translatedName = sourceName;
                    translatedDesc = sourceDesc;
                    translatedAddress = sourceAddress;
                    log.debug("[Localization] [vi] Su dung text goc, stallId={}", stallId);
                } else {
                    log.info("[Localization] [{}] Dich name + description + address, stallId={}", lang, stallId);
                    translatedName = translationService.translate(sourceName, lang);
                    translatedDesc = translationService.translate(sourceDesc, lang);
                    translatedAddress = translationService.translate(sourceAddress, lang);
                }

                // Effectively-final aliases required for use inside lambda
                final String finalName = translatedName;
                final String finalDesc = translatedDesc;
                final String finalAddress = translatedAddress;

                // Audio Generation Phase
                String audioText = finalName + ". " + finalDesc;
                String audioUrl = audioService.getOrCreateAudioForStall(stallId, audioText, lang);
                if (audioUrl == null) {
                    audioUrl = stallId + "_" + lang + ".mp3";
                    log.warn("[Localization] [{}] getOrCreateAudio tra ve null, dung fallback audioUrl={}", lang, audioUrl);
                }
                final String finalAudioUrl = audioUrl;

                if ("vi".equalsIgnoreCase(lang) && finalAudioUrl != null && !finalAudioUrl.isBlank()) {
                    savedStall.setAudioUrl(finalAudioUrl);
                    foodStallRepository.save(savedStall);
                }

                // Database Save Phase (upsert)
                FoodStallLocalization stallRef = localizationRepository
                        .findByFoodStall_IdAndLanguageCode(stallId, lang)
                        .map(existing -> {
                            existing.setName(finalName);
                            existing.setDescription(finalDesc);
                            existing.setAddress(finalAddress);
                            existing.setAudioUrl(finalAudioUrl);
                            return existing;
                        })
                        .orElseGet(() -> FoodStallLocalization.builder()
                                .foodStall(savedStall)
                                .languageCode(lang)
                                .name(finalName)
                                .description(finalDesc)
                                .address(finalAddress)
                                .audioUrl(finalAudioUrl)
                                .build());
                localizationRepository.save(stallRef);

                log.info("[Localization] [{}] Hoan thanh stallId={}, audioUrl={}", lang, stallId, finalAudioUrl);

            } catch (Exception e) {
                // Fault-tolerant: loi o 1 ngon ngu khong dung qua trinh cac ngon ngu con lai
                log.error("[Localization] [{}] Loi khi xu ly stallId={}: {}", lang, stallId, e.getMessage(), e);
            }
        }

        log.info("[Localization] [Async] Hoan thanh tat ca ngon ngu cho stallId={}", stallId);
    }

    /**
     * Sync All Localizations: Quet toan bo FoodStall trong DB, tim cac quan chua co
     * du 5 ban dich (vi/en/ja/ko/zh) va kich hoat processLocalizationAndAudioInBackground
     * cho tung quan do. API tra ve bao cao ngay lap tuc, cong viec dich chay ngam.
     *
     * @return Map chua tong so quan, so quan can xu ly va so quan da day du ban dich
     */
    public Map<String, Object> syncAllMissingLocalizations() {
        String[] allLangs = {"vi", "en", "ja", "ko", "zh"};
        int totalLangCount = allLangs.length;

        List<FoodStall> allStalls = foodStallRepository.findAll();
        log.info("[SyncAll] Bat dau quet {} quan an de tim ban dich con thieu", allStalls.size());

        // Lay tat ca id cua cac localization hien co trong DB
        List<FoodStallLocalization> allLocs = localizationRepository.findAll();
        // Group: stallId -> so ngon ngu da co
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
                log.info("[SyncAll] stallId={} (ten='{}') chi co {}/{} ngon ngu -> kich hoat dong bo",
                        stall.getId(), stall.getName(), existingLangCount, totalLangCount);
                processLocalizationAndAudioInBackground(stall);
                needsSync++;
            } else {
                alreadyComplete++;
            }
        }

        log.info("[SyncAll] Hoan thanh phat lenh: {} quan can dong bo, {} quan da day du", needsSync, alreadyComplete);
        return Map.of(
                "totalStalls", allStalls.size(),
                "queuedForSync", needsSync,
                "alreadyComplete", alreadyComplete,
                "message", needsSync > 0
                        ? needsSync + " quan dang duoc dong bo da ngon ngu trong nen. Vui long doi 15-30 giay roi kiem tra lai."
                        : "Tat ca quan da co du ban dich. Khong can dong bo them."
        );
    }

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
     * Force-regenerate audio files for all languages (overwrite mp3) and upsert localizations.
     * Used after admin approves an update to ensure audio matches the latest content.
     */
    @Async
    public void regenerateAllLanguagesForStall(Long stallId) {
        String[] languages = {"vi", "en", "ja", "ko", "zh"};
        log.info("[Localization] Force regenerate audio cho {} ngon ngu, stallId={}", languages.length, stallId);

        for (String lang : languages) {
            try {
                this.generateLocalizationForceAudio(stallId, lang);
            } catch (Exception e) {
                log.error("[Localization] Force regenerate loi lang={} stallId={}: {}", lang, stallId, e.getMessage());
            }
        }
        log.info("[Localization] Hoan thanh force regenerate audio cho stallId={}", stallId);
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
        return generateLocalizationInternal(stallId, targetLang, false);
    }

    @Transactional
    public String generateLocalizationForceAudio(Long stallId, String targetLang) {
        return generateLocalizationInternal(stallId, targetLang, true);
    }

    private String generateLocalizationInternal(Long stallId, String targetLang, boolean forceAudio) {
        log.info("[Localization] Bat dau tao localization stallId={}, lang={}", stallId, targetLang);

        // 1. Lay thong tin goc tieng Viet
        if (stallId == null) throw new IllegalArgumentException("stallId must not be null");
        FoodStall stall = foodStallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan an khong ton tai: " + stallId));

        FoodStallLocalization viLoc = localizationRepository
                .findByFoodStall_IdAndLanguageCode(stallId, "vi")
                .orElse(null);

        // Prefer the latest approved FoodStall fields as the Vietnamese source-of-truth.
        // Only fallback to stored "vi" localization if stall fields are missing.
        String sourceName = stall.getName();
        if (sourceName == null || sourceName.isBlank()) {
            sourceName = (viLoc != null) ? viLoc.getName() : null;
        }

        String sourceDesc = stall.getDescription();
        if (sourceDesc == null || sourceDesc.isBlank()) {
            sourceDesc = (viLoc != null) ? viLoc.getDescription() : null;
        }

        // 2. Dich sang ngon ngu dich
        String translatedName;
        String translatedDesc;
        String translatedAddress;

        if ("vi".equalsIgnoreCase(targetLang)) {
            translatedName = sourceName;
            translatedDesc = sourceDesc;
            translatedAddress = stall.getAddress();
        } else {
            log.info("[Localization] Dich name + description + address sang {}", targetLang);
            translatedName = translationService.translate(sourceName, targetLang);
            translatedDesc = translationService.translate(sourceDesc, targetLang);
            translatedAddress = translationService.translate(stall.getAddress(), targetLang);
        }

        // 3. Tao audio MP3
        String audioText = translatedName + ". " + translatedDesc;
        log.info("[Localization] Tao audio lang={}, do dai text={} chars", targetLang, audioText.length());
        @SuppressWarnings("null")
        String audioUrl = forceAudio
            ? audioService.generateAndOverwriteAudioForStall(stallId, audioText, targetLang)
                : audioService.getOrCreateAudioForStall(stallId, audioText, targetLang);

        // 4. Luu hoac cap nhat localization
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

        // Canonical audio URL for client retrieval is stored on food_stalls (VI/default audio).
        if ("vi".equalsIgnoreCase(targetLang) && audioUrl != null && !audioUrl.isBlank()) {
            stall.setAudioUrl(audioUrl);
            foodStallRepository.save(stall);
        }

        log.info("[Localization] Hoan thanh stallId={}, lang={}, audioUrl={}", stallId, targetLang, audioUrl);
        return audioUrl;
    }
}
