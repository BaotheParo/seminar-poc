package com.foodstreet.voice.service.audio;

import org.springframework.lang.NonNull;
import java.nio.charset.StandardCharsets;

/**
 * Legacy mock provider — NOT registered as a Spring bean.
 * EdgeTTSProvider is the active implementation.
 */
public class GoogleTTSProvider implements AudioProviderStrategy {

    @Override
    @NonNull
    @SuppressWarnings("null")
    public byte[] generateAudio(@NonNull String text, @NonNull String languageCode) {
        System.out.println("[GoogleTTSProvider MOCK] lang=" + languageCode + ": " + text);
        return ("Dummy content: " + text).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getProviderName() {
        return "google_tts";
    }
}
