package com.foodstreet.voice.service;

import com.foodstreet.voice.dto.vietmap.VietMapPlaceResponse;
import com.foodstreet.voice.dto.vietmap.VietMapSearchResponse;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class VietMapSyncService {

    private final RestClient restClient;
    private final FoodStallRepository foodStallRepository;
    private final AudioService audioService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${vietmap.api-key}")
    private String apiKey;

    public VietMapSyncService(RestClient.Builder restClientBuilder,
            FoodStallRepository foodStallRepository,
            AudioService audioService,
            @Value("${vietmap.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.foodStallRepository = foodStallRepository;
        this.audioService = audioService;
    }

    public int syncStallsFromVietMap(double lat, double lng, String keyword) {
        log.info("Bat dau sync tu VietMap cho tu khoa '{}' gan {},{}", keyword, lat, lng);

        try {
            // Goi api
            List<VietMapSearchResponse> searchResults = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/v3")
                            .queryParam("apikey", apiKey)
                            .queryParam("text", keyword)
                            .queryParam("focus", lat + "," + lng)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<VietMapSearchResponse>>() {
                    });

            if (searchResults == null || searchResults.isEmpty()) {
                log.info("Khong tim thay ket qua tu VietMap.");
                return 0;
            }

            int savedCount = 0;

            for (VietMapSearchResponse item : searchResults) {
                try {
                    // kt trung
                    if (foodStallRepository.existsByName(item.name())) {
                        log.debug("Bo qua trung: {}", item.name());
                        continue;
                    }

                    Double itemLat = item.lat();
                    Double itemLng = item.lng();

                    // lay toa do bi thieu
                    if (itemLat == null || itemLng == null) {
                        log.info("Lay chi tiet cho item: {} (ref_id: {})", item.name(), item.refId());
                        VietMapPlaceResponse detail = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/place/v3")
                                        .queryParam("apikey", apiKey)
                                        .queryParam("refid", item.refId())
                                        .build())
                                .retrieve()
                                .body(VietMapPlaceResponse.class);

                        if (detail != null) {
                            itemLat = detail.lat();
                            itemLng = detail.lng();
                        }
                    }

                    // van null thi bo qua
                    if (itemLat == null || itemLng == null) {
                        log.warn("Bo qua item '{}' do thieu toa do.", item.name());
                        continue;
                    }

                    // luu vao db
                    Point location = geometryFactory.createPoint(new Coordinate(itemLng, itemLat)); // Note: X=Lng,
                                                                                                    // Y=Lat

                    // tao audio
                    String description = item.address() != null ? item.address() : "Am thuc duong pho";
                    String audioUrl = audioService
                            .getOrCreateAudio("Gioi thieu quan " + item.name() + ". " + description, "vi");

                    FoodStall stall = FoodStall.builder()
                            .name(item.name())
                            .description(description)
                            .location(location)
                            .audioUrl(audioUrl)
                            .imageUrl("")
                            .triggerRadius(8)
                            .build();

                    foodStallRepository.save(stall);
                    savedCount++;
                    log.debug("Luu: {}", item.name());

                } catch (Exception e) {
                    log.error("Loi khi xu ly item '{}': {}", item.name(), e.getMessage());
                }
            }

            log.info("Ket thuc sync. Da luu {} quan moi.", savedCount);
            return savedCount;

        } catch (Exception e) {
            log.error("Loi khi sync tu VietMap: {}", e.getMessage(), e);
            throw new RuntimeException("Sync failed", e);
        }
    }
}
