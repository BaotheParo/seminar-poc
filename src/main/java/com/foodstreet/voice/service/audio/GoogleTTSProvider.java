package com.foodstreet.voice.service.audio;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class GoogleTTSProvider implements AudioProviderStrategy {
    //TODO: Khi nào có API Key thật thì bỏ code gọi Google Cloud vào đây
    // Hien tai mock de test flow
    @Override
    public byte[] generateAudio(String text, String languageCode){
        System.out.println("google tts api [" + languageCode + "]: " + text);

        try {
            Thread.sleep(500);
        }catch (InterruptedException e){}

        return ("Dummy content: "+ text).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getProviderName() {
        return "google_tts";
    }
}
