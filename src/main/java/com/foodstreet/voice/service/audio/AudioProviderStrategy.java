package com.foodstreet.voice.service.audio;

import org.springframework.lang.NonNull;

public interface AudioProviderStrategy {
    @NonNull
    byte[] generateAudio(@NonNull String text, @NonNull String languageCode);

    String getProviderName();
}
