package com.foodstreet.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeofenceStallResponse {
    @Schema(description = "ID của quán", example = "36")
    private Long id;
    
    @Schema(description = "Tên quán ăn", example = "Quán Ốc Oanh")
    private String name;
    
    @Schema(description = "Mô tả của quán (Dùng để generate Audio Text)", example = "Quán Ốc Oanh là một trong những quán ốc...")
    private String description;
    
    @Schema(description = "Địa chỉ của quán", example = "12 Vĩnh Khánh, Quận 4, TP.HCM")
    private String address;
    
    @Schema(description = "Vĩ độ của quán", example = "10.762622")
    private Double latitude;
    
    @Schema(description = "Kinh độ của quán", example = "106.700174")
    private Double longitude;
    
    @Schema(description = "Bán kính kích hoạt phát Audio (mét). Trả về từ DB. Khi distance <= triggerRadius thì App phát luôn AudioUrl", example = "12")
    private Integer triggerRadius;
    
    @Schema(description = "Đường dẫn File Audio trên Server", example = "/audio/579859624_vi.mp3")
    private String audioUrl;
    
    @Schema(description = "Khoảng cách thực tế (mét) tính từ tọa độ truyền lên đến tọa độ quán. Đã sắp xếp gần theo ưu tiên", example = "25.0")
    private Double distance;
    
    @Schema(description = "Độ ưu tiên của quán (Càng cao xếp càng trên)", example = "10")
    private Integer priority;

    @Schema(description = "Ngôn ngữ thực tế được sử dụng để trả về Name/Description", example = "en")
    private String usedLanguage;

    @Schema(description = "Trạng thái dịch thuật (null nếu khớp ngôn ngữ, 'FALLBACK_TO_VI' nếu phải dùng tiếng Việt thay thế)", example = "FALLBACK_TO_VI")
    private String localizationStatus;
}
