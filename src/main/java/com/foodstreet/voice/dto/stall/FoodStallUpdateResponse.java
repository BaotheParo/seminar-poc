package com.foodstreet.voice.dto.stall;

import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FoodStallUpdateResponse {
    private Long id;
    private FoodStallUpdateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reason;
    private String stallName;
    private String ownerUsername;
    private Map<String, Object> changes;
}
