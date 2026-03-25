package com.foodstreet.voice.integration;

import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.service.audio.AudioProviderStrategy;
import com.foodstreet.voice.config.AudioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AudioAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FoodStallRepository foodStallRepository;

    // Mock AudioProviderStrategy de tranh goi edge-tts CLI that trong test
    @MockBean
    private AudioProviderStrategy audioProvider;

    @Autowired
    private AudioProperties audioProperties;

    private String getUploadDir() {
        String path = audioProperties.getLocalPath();
        return path.endsWith("/") ? path : path + "/";
    }

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(Paths.get(getUploadDir()));
        // Mock tra ve bytes gia (MP3-like) de AudioService luu file thanh cong
        when(audioProvider.generateAudio(anyString(), anyString()))
                .thenReturn("fake-mp3-bytes-for-test".getBytes());
        when(audioProvider.getProviderName()).thenReturn("mock_tts");
    }

    @Test
    void listAudioFiles() throws Exception {
        Path testFile = Paths.get(getUploadDir() + "test_audio.mp3");
        Files.write(testFile, "test data".getBytes());

        mockMvc.perform(get("/api/v1/admin/audio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Files.deleteIfExists(testFile);
    }

    @Test
    @SuppressWarnings("null")
    void regenerateAudio() throws Exception {
        FoodStall stall = foodStallRepository.save(FoodStall.builder()
                .name("Test Stall")
                .description("Test Description")
                .triggerRadius(10)
                .build());

        mockMvc.perform(post("/api/v1/admin/audio/regenerate/" + stall.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audio regenerated successfully"))
                .andExpect(jsonPath("$.newUrl").exists());

        // Clean up generated file
        FoodStall updated = foodStallRepository.findById(stall.getId()).orElseThrow();
        if (updated.getAudioUrl() != null) {
            Files.deleteIfExists(Paths.get(getUploadDir() + updated.getAudioUrl().replace("/audio/", "")));
        }
    }

    @Test
    void listOrphanedFiles() throws Exception {
        Path orphanedFile = Paths.get(getUploadDir() + "orphaned_audio.mp3");
        Files.write(orphanedFile, "orphaned data".getBytes());

        mockMvc.perform(get("/api/v1/admin/audio/orphaned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Files.deleteIfExists(orphanedFile);
    }
}

