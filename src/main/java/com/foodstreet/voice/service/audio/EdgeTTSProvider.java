package com.foodstreet.voice.service.audio;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class EdgeTTSProvider implements AudioProviderStrategy {

    // Map language code → Edge TTS voice name
    private static final Map<String, String> VOICE_MAP = Map.of(
            "vi", "vi-VN-HoaiMyNeural",
            "en", "en-US-JennyNeural",
            "ja", "ja-JP-NanamiNeural",
            "ko", "ko-KR-SunHiNeural",
            "zh", "zh-CN-XiaoxiaoNeural"
    );

    @Override
    @NonNull
    @SuppressWarnings("null")
    public byte[] generateAudio(@NonNull String text, @NonNull String languageCode) {
        String voice = VOICE_MAP.getOrDefault(languageCode, VOICE_MAP.get("vi"));
        Path tempFile = null;

        try {
            // Tạo file tạm để edge-tts ghi output
            tempFile = Files.createTempFile("edge-tts-" + UUID.randomUUID(), ".mp3");

            ProcessBuilder pb = new ProcessBuilder(
                    "edge-tts",
                    "--voice", voice,
                    "--text", text,
                    "--write-media", tempFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc output để tránh blocking
            String output = new String(process.getInputStream().readAllBytes());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("edge-tts failed (exit=" + exitCode + "): " + output);
            }

            byte[] audioBytes = Files.readAllBytes(tempFile);

            if (audioBytes.length == 0) {
                throw new RuntimeException("edge-tts produced empty file for lang=" + languageCode);
            }

            System.out.println("[EdgeTTS] Generated " + audioBytes.length + " bytes for lang=" + languageCode + " voice=" + voice);
            return audioBytes;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("EdgeTTS generation failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public String getProviderName() {
        return "edge_tts";
    }
}
