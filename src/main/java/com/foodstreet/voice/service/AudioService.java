package com.foodstreet.voice.service;

import com.foodstreet.voice.service.audio.AudioProviderStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AudioService {
    private final AudioProviderStrategy audioProvider; //Ap dung Strategy Pattern
    private final String UPLOAD_DIR = "./uploads/audio/";

    public String getOrCreateAudio(String text, String languageCode){
        try{
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String fileName = text.hashCode() + "_" + languageCode + ".mp3";
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            if (Files.exists(filePath)){
                System.out.println("Cache: " + fileName);
                return "/audio/" + fileName;
            }

            byte[] audioData = audioProvider.generateAudio(text, languageCode);

            FileCopyUtils.copy(audioData, filePath.toFile());
            System.out.println("new audio: " + fileName);

            return "/audio/" + fileName;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

}
