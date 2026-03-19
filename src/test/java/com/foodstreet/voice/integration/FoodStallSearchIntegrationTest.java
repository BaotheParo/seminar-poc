package com.foodstreet.voice.integration;

import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FoodStallSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FoodStallRepository foodStallRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        foodStallRepository.deleteAll();

        Point location = geometryFactory.createPoint(new Coordinate(106.700174, 10.762622));

        foodStallRepository.save(FoodStall.builder()
                .name("Com Tam Cali")
                .description("Delicious broken rice")
                .address("District 4")
                .minPrice(30000)
                .maxPrice(60000)
                .rating(4.5)
                .triggerRadius(15)
                .location(location)
                .build());

        foodStallRepository.save(FoodStall.builder()
                .name("Pho Hung")
                .description("Beef noodle soup")
                .address("District 1")
                .minPrice(50000)
                .maxPrice(100000)
                .rating(4.8)
                .triggerRadius(15)
                .location(location)
                .build());

        foodStallRepository.save(FoodStall.builder()
                .name("Banh Mi Huynh Hoa")
                .description("Famous banh mi")
                .address("District 1")
                .minPrice(40000)
                .maxPrice(50000)
                .rating(4.7)
                .triggerRadius(15)
                .location(location)
                .build());
    }

    @Test
    void searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/search")
                .param("keyword", "com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Com Tam Cali"));
    }

    @Test
    void filterByPriceRange() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/search")
                .param("minPrice", "45000")
                .param("maxPrice", "70000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3))); // All 3 stalls overlap with [45k, 70k]
    }

    @Test
    void filterByRating() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/search")
                .param("minRating", "4.7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void verifyPagination() throws Exception {
        mockMvc.perform(get("/api/v1/stalls/search")
                .param("size", "1")
                .param("page", "0")
                .param("sort", "name,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }
}
