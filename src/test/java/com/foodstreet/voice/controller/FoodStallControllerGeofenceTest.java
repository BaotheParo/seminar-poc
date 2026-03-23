package com.foodstreet.voice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
    "VIETMAP_API_KEY_SERVICES=dummy",
    "AUDIO_BASE_URL=http://localhost"
})
public class FoodStallControllerGeofenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GF-01: API Geofence trả về priority và distance, sort đúng")
    void testGeofenceEndpoint() throws Exception {
        // Tọa độ test: Đường Vĩnh Khánh
        mockMvc.perform(get("/api/v1/stalls/geofence")
                .param("lat", "10.760852")
                .param("lng", "106.703294")
                .param("radius", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                // Kiểm tra xem JSON có chứa priority và distance không
                .andExpect(jsonPath("$[0].priority", notNullValue()))
                .andExpect(jsonPath("$[0].distance", notNullValue()))
                // Quán đầu tiên phải là quán có Priority cao nhất (Ốc Oanh - 10)
                .andExpect(jsonPath("$[0].name", containsString("Ốc"))); 
    }

    @Test
    @DisplayName("OFF-01: API Pack Info trả về đúng chuẩn")
    void testPackInfoEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/pack-info")
                .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language", is("en")))
                .andExpect(jsonPath("$.totalFiles", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.estimatedSizeMb", notNullValue()))
                .andExpect(jsonPath("$.lastUpdated", notNullValue()));
    }
}
