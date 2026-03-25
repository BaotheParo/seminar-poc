package com.foodstreet.voice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodstreet.voice.dto.TrackEventRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import com.foodstreet.voice.repository.UserActivityRepository;
import com.foodstreet.voice.service.audio.AudioProviderStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "VIETMAP_API_KEY_SERVICES=dummy",
        "AUDIO_BASE_URL=http://localhost",
        "spring.datasource.username=postgres",
        "spring.datasource.password=password"
})
public class FullUserJourneyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private FoodStallRepository foodStallRepository;

    @Autowired
    private com.foodstreet.voice.repository.FoodStallLocalizationRepository localizationRepository;

    @MockBean
    private AudioProviderStrategy audioProvider;

    @MockBean
    private com.foodstreet.voice.service.VietMapSyncService vietMapSyncService;

    @BeforeEach
    void setUp() {
        // Clear all analytics data so we have a clean state for admin dashboard assertions
        userActivityRepository.deleteAll();

        // Mock TTS generation to prevent actual edge-tts CLI execution
        when(audioProvider.generateAudio(anyString(), anyString())).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("End-to-End User Journey: Pack Info -> Geofence -> Audio Race Condition -> Analytics Batch -> Admin Dashboard")
    void shouldCompleteFullUserJourney() throws Exception {
        // ==========================================
        // STEP 1: Pack Info & Initial Load
        // ==========================================
        mockMvc.perform(get("/api/v1/stalls/pack-info")
                .param("lang", "vi")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedSizeMb").exists())
                .andExpect(jsonPath("$.totalFiles").exists());

        mockMvc.perform(get("/api/v1/stalls")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // ==========================================
        // STEP 2: Geofencing Match (Oc Oanh Coordinates)
        // ==========================================
        // Coordinates for Oc Oanh from PRD: lat=10.760852, lng=106.703294
        org.springframework.test.web.servlet.MvcResult geofenceResult = mockMvc.perform(get("/api/v1/stalls/geofence")
                .param("lat", "10.760852")
                .param("lng", "106.703294")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].priority").exists())
                .andExpect(jsonPath("$[0].audioUrl").exists())
                .andReturn();
                
        String content = geofenceResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Long stallId = objectMapper.readTree(content).get(0).get("id").asLong();

        // Modify description directly in DB and Localizations to guarantee audio cache miss
        String randomText = java.util.UUID.randomUUID().toString();
        FoodStall stall = foodStallRepository.findById(stallId).orElseThrow();
        stall.setDescription(randomText);
        foodStallRepository.save(stall);
        localizationRepository.findByFoodStallIdAndLanguageCode(stallId, "vi").ifPresent(loc -> {
            loc.setDescription(randomText);
            localizationRepository.save(loc);
        });

        // ==========================================
        // STEP 3: Audio Generation & Race Condition Protection
        // ==========================================
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await(); // wait for all threads to be ready
                int status = mockMvc.perform(post("/api/v1/stalls/" + stallId + "/audio/generate?lang=vi")
                        .accept(MediaType.APPLICATION_JSON))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                doneLatch.countDown();
                return status;
            }));
        }

        startLatch.countDown(); // Unleash the 5 threads concurrently!
        doneLatch.await(10, TimeUnit.SECONDS);

        // Verify all threads got HTTP 200 OK
        for (Future<Integer> future : futures) {
            org.junit.jupiter.api.Assertions.assertEquals(200, future.get());
        }

        // PRD Requirement: Verify Request Coalescing blocks duplicate TTS provider calls
        verify(audioProvider, times(1)).generateAudio(anyString(), anyString());

        // ==========================================
        // STEP 4: Batch Analytics Sync
        // ==========================================
        List<TrackEventRequest> requests = List.of(
                createEvent("DEVICE_INT_TEST", stallId, "ENTER_REGION", 0),
                createEvent("DEVICE_INT_TEST", stallId, "PLAY_AUDIO", 0),
                createEvent("DEVICE_INT_TEST", stallId, "FINISH_AUDIO", 60)
        );

        mockMvc.perform(post("/api/v1/analytics/track/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.count").value(3));

        // Wait for @Async batch tracking to insert into DB
        Thread.sleep(1000);

        // ==========================================
        // STEP 5: Admin Dashboard Verification
        // ==========================================
        // Heatmap verification
        mockMvc.perform(get("/api/v1/analytics/hourly-heatmap")
                .param("stallId", stallId.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                // Expect that at least one hour bucket has visits > 0 due to our ENTER_REGION event
                .andExpect(jsonPath("$.data[*].visits").value(hasItem(greaterThan(0))));

        // Ranking verification
        String fromDate = LocalDate.now().minusDays(1).toString();
        String toDate = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/v1/analytics/poi-ranking")
                .param("from", fromDate)
                .param("to", toDate)
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                // Verify extracted Stall ID holds engagement metrics
                .andExpect(jsonPath("$.data[0].stallId").value(stallId.intValue()))
                .andExpect(jsonPath("$.data[0].visits").value(1))
                .andExpect(jsonPath("$.data[0].plays").value(1));
    }

    private TrackEventRequest createEvent(String deviceId, Long stallId, String actionStr, Integer duration) {
        com.foodstreet.voice.entity.UserActivity.ActionType action = com.foodstreet.voice.entity.UserActivity.ActionType.valueOf(actionStr);
        return TrackEventRequest.builder()
                .deviceId(deviceId)
                .stallId(stallId)
                .action(action)
                .duration(duration)
                .build();
    }
}
