package com.foodstreet.voice.service.audio;

import org.springframework.stereotype.Service;

import org.springframework.lang.NonNull;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleTTSProvider implements AudioProviderStrategy {
    // Hien tai mock de test flow
    @Override
    @NonNull
    @SuppressWarnings("null")
    public byte[] generateAudio(@NonNull String text, @NonNull String languageCode) {
        System.out.println("google tts api [" + languageCode + "]: " + text);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        return ("Dummy content: " + text).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getProviderName() {
        return "google_tts";
    }
}
