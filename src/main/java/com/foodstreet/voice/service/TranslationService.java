package com.foodstreet.voice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private static final String MYMEMORY_URL =
            "https://api.mymemory.translated.net/get?q={text}&langpair={langpair}";

    private final RestTemplate restTemplate;

    /**
     * Dich van ban tu tieng Viet sang ngon ngu dich.
     *
     * @param text       Van ban tieng Viet can dich
     * @param targetLang Ma ngon ngu dich (en, ja, ko, zh)
     * @return Van ban da dich, hoac text goc neu loi
     */
    public String translate(String text, String targetLang) {
        if ("vi".equalsIgnoreCase(targetLang)) {
            return text; // Khong can dich
        }

        try {
            String langpair = "vi|" + targetLang;

            @SuppressWarnings("rawtypes")
            Map response = restTemplate.getForObject(
                    MYMEMORY_URL,
                    Map.class,
                    text, langpair
            );

            if (response != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) response.get("responseData");
                if (responseData != null) {
                    String translated = (String) responseData.get("translatedText");
                    if (translated != null && !translated.isBlank()) {
                        log.info("[Translation] {} -> {}: {} chars", "vi", targetLang, translated.length());
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Translation] MyMemory API failed for lang={}: {}", targetLang, e.getMessage());
        }

        // Fallback: tra ve text goc
        log.warn("[Translation] Fallback to original text for lang={}", targetLang);
        return text;
    }
}
