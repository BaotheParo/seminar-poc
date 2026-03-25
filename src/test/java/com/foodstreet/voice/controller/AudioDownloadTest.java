package com.foodstreet.voice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "VIETMAP_API_KEY_SERVICES=dummy",
        "AUDIO_BASE_URL=http://localhost",
        "spring.datasource.username=postgres",
        "spring.datasource.password=password"
})
public class AudioDownloadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.foodstreet.voice.service.VietMapSyncService vietMapSyncService;

    @BeforeEach
    void setUp() throws Exception {
        // Create dummy files for testing
        File dir = new File("uploads/audio");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Files.writeString(Path.of("uploads/audio/dummy_vi.mp3"), "fake content vi");
        Files.writeString(Path.of("uploads/audio/dummy_en.mp3"), "fake content en");
    }

    @Test
    @DisplayName("Test Bulk Audio Download Pack API")
    void testDownloadPack() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/audio/download-pack?lang=vi"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Test Download All Audio API")
    void testDownloadAll() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/audio/download-all"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }
}
