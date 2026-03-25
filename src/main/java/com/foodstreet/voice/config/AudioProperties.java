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

    public String getResolvedLocalPath() {
        String path = localPath;
        if (!path.startsWith("/") && !path.startsWith("C:") && !path.startsWith("D:")) {
            String userDir = System.getProperty("user.dir");
            if (userDir.endsWith("senimar") && !userDir.endsWith("street-voice-backend")) {
                path = "street-voice-backend/" + path;
            }
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path;
    }

    // Helper: build URL từ tên file
    // buildAudioUrl("8_vi.mp3") → "http://192.168.1.5:8080/audio/oc_oanh.mp3"
    public String buildAudioUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        return baseUrl + "/audio/" + filename;
    }
}
