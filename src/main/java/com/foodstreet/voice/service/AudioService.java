package com.foodstreet.voice.service;

import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.exception.ResourceNotFoundException;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.service.audio.AudioProviderStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AudioService {
    private final AudioProviderStrategy audioProvider;

    @Autowired
    @Lazy
    private FoodStallRepository foodStallRepository;

    private final String UPLOAD_DIR = "./uploads/audio/";

    @SuppressWarnings("null")
    public String getOrCreateAudio(@NonNull String text, @NonNull String languageCode) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            // Su dung text truncate hoac hash de tinh ten file
            String fileName = Math.abs(text.hashCode()) + "_" + languageCode + ".mp3";
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            if (Files.exists(filePath)) {
                return "/audio/" + fileName;
            }

            byte[] audioData = audioProvider.generateAudio(text, languageCode);
            FileCopyUtils.copy(audioData, filePath.toFile());

            return "/audio/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> listAllAudioFiles() {
        try (Stream<Path> stream = Files.list(Paths.get(UPLOAD_DIR))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    public boolean deleteAudioFile(String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    public String regenerateAudio(Long stallId) {
        FoodStall stall = foodStallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan an khong ton tai: " + stallId));

        // Xoa file cu neu co
        if (stall.getAudioUrl() != null && !stall.getAudioUrl().isEmpty()) {
            String oldFileName = stall.getAudioUrl().replace("/audio/", "");
            deleteAudioFile(oldFileName);
        }

        // Tao audio moi tu description
        String text = stall.getName() + ". " + stall.getDescription();
        String newAudioUrl = getOrCreateAudio(text, "vi");

        stall.setAudioUrl(newAudioUrl);
        foodStallRepository.save(stall);

        return newAudioUrl;
    }

    public List<String> getOrphanedAudioFiles() {
        List<String> allFiles = listAllAudioFiles();
        List<String> linkedFiles = foodStallRepository.findAll().stream()
                .map(FoodStall::getAudioUrl)
                .filter(url -> url != null && url.startsWith("/audio/"))
                .map(url -> url.replace("/audio/", ""))
                .collect(Collectors.toList());

        return allFiles.stream()
                .filter(file -> !linkedFiles.contains(file))
                .collect(Collectors.toList());
    }
}
