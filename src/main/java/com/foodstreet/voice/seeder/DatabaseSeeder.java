package com.foodstreet.voice.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodstreet.voice.dto.FoodStallImportDto;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.repository.FoodStallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final FoodStallRepository foodStallRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (foodStallRepository.count() == 0) {
            log.info("Bảng food_stalls đang trống, bắt đầu nạp dữ liệu từ file JSON...");

            try (InputStream is = getClass().getResourceAsStream("/vinh_khanh_places.json")) {
                if (is == null) {
                    log.error("Không tìm thấy file vinh_khanh_places.json trong resources");
                    return;
                }

                List<FoodStallImportDto> dtos = objectMapper.readValue(is,
                        new TypeReference<List<FoodStallImportDto>>() {
                        });
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                int count = 0;

                for (FoodStallImportDto dto : dtos) {
                    if (dto.getLat() == null || dto.getLng() == null) {
                        log.warn("Bỏ qua quán {} vì thiếu tọa độ GPS", dto.getName());
                        continue;
                    }

                    Point location = geometryFactory.createPoint(new Coordinate(dto.getLng(), dto.getLat()));

                    FoodStall stall = FoodStall.builder()
                            .name(dto.getName())
                            .address(dto.getAddress())
                            .description(dto.getDescription())
                            .location(location)
                            .triggerRadius(dto.getTriggerRadius() != null ? dto.getTriggerRadius() : 15)
                            .audioUrl(dto.getAudioUrl())
                            .minPrice(dto.getMinPrice())
                            .maxPrice(dto.getMaxPrice())
                            .audioDuration(dto.getAudioDuration())
                             .featuredReviews(dto.getFeaturedReviews())
                             .rating(dto.getRating())
                             .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                             .build();

                    foodStallRepository.save(stall);
                    count++;
                }

                log.info("Nạp xong {} quán ăn!", count);
            } catch (Exception e) {
                log.error("Có lỗi xảy ra khi nạp mock data", e);
            }
        } else {
            log.info("Dữ liệu food_stalls đã tồn tại, bỏ qua seeder.");
        }
    }
}
