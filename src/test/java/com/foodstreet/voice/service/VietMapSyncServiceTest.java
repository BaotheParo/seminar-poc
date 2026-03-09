package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.vietmap.VietMapPlaceResponse;
import com.foodstreet.voice.dto.vietmap.VietMapSearchResponse;
import com.foodstreet.voice.repository.FoodStallRepository;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class VietMapSyncServiceTest {

    @Mock
    private FoodStallRepository foodStallRepository;

    @Mock
    private AudioService audioService;

    private VietMapSyncService syncService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Setup RestClient with MockServer
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter()));

        mockServer = MockRestServiceServer.bindTo(builder).build();

        // Initialize Service
        syncService = new VietMapSyncService(builder, foodStallRepository, audioService, "https://mock.api");
    }

    @Test
    void syncStallsFromVietMap_ShouldSaveNewStalls() {
        // Given
        String keyword = "quan com";
        double lat = 10.0;
        double lng = 106.0;

        String mockJsonResponse = """
                [
                    {
                        "ref_id": "ref1",
                        "name": "Quan Com 1",
                        "address": "Address 1",
                        "lat": 10.1,
                        "lng": 106.1
                    }
                ]
                """;

        // Mock API Response
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/v3")))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // Mock Repository
        when(foodStallRepository.existsByName("Quan Com 1")).thenReturn(false);
        when(audioService.getOrCreateAudio(anyString(), anyString())).thenReturn("/audio/mock.mp3");

        // When
        int result = syncService.syncStallsFromVietMap(lat, lng, keyword);

        // Then
        assertEquals(1, result);
        verify(foodStallRepository, times(1)).save(any());
    }

    @Test
    void syncStallsFromVietMap_ShouldSkipDuplicates() {
        // Given
        String mockJsonResponse = """
                [
                    {
                        "ref_id": "ref1",
                        "name": "Quan Duplicate",
                        "address": "Address 1",
                        "lat": 10.1,
                        "lng": 106.1
                    }
                ]
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/v3")))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // Mock Duplicate exists
        when(foodStallRepository.existsByName("Quan Duplicate")).thenReturn(true);

        // When
        int result = syncService.syncStallsFromVietMap(10.0, 106.0, "keyword");

        // Then
        assertEquals(0, result);
        verify(foodStallRepository, never()).save(any());
    }

    @Test
    void syncStallsFromVietMap_ShouldFetchDetailsExample() {
        // Given: Item with NULL coordinates
        String mockSearchResponse = """
                [
                    {
                        "ref_id": "ref_detail",
                        "name": "Quan Detail",
                        "address": "Address 2",
                        "lat": null,
                        "lng": null
                    }
                ]
                """;

        // Mock Search API
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/v3")))
                .andRespond(withSuccess(mockSearchResponse, MediaType.APPLICATION_JSON));

        // Mock Detail API
        String mockDetailResponse = """
                {
                    "ref_id": "ref_detail",
                    "name": "Quan Detail",
                    "address": "Address 2",
                    "lat": 10.5,
                    "lng": 106.5
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/place/v3")))
                .andRespond(withSuccess(mockDetailResponse, MediaType.APPLICATION_JSON));

        when(foodStallRepository.existsByName("Quan Detail")).thenReturn(false);
        when(audioService.getOrCreateAudio(anyString(), anyString())).thenReturn("/audio/mock.mp3");

        // When
        int result = syncService.syncStallsFromVietMap(10.0, 106.0, "keyword");

        // Then
        assertEquals(1, result);
        verify(foodStallRepository, times(1)).save(any());
    }
}
