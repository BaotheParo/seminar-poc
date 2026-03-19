package com.foodstreet.voice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AudioProperties {

    @Value("${app.audio.base-url}")
    private String baseUrl;

    @Value("${app.audio.local-path:/app/audio}")
    private String localPath;

    public String getBaseUrl() { return baseUrl; }
    public String getLocalPath() { return localPath; }

    // Helper: build URL từ tên file
    // buildAudioUrl("oc_oanh.mp3") → "http://192.168.1.5:8080/audio/oc_oanh.mp3"
    public String buildAudioUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        return baseUrl + "/audio/" + filename;
    }
}
