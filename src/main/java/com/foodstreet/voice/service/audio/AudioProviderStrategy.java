package com.foodstreet.voice.service.audio;

public interface AudioProviderStrategy {
    byte[] generateAudio (String text, String languageCode);
    String getProviderName();
}
