package com.foodstreet.voice.repository;

import com.foodstreet.voice.entity.FoodStall;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodStallRepository extends JpaRepository<FoodStall, Long> {

    // ST_DWithin: hoạt động như một bộ lọc chỉ quét những điểm nằm trong vùng index
    // => rat tot khi dữ liệu lớn
    @Query(value = "SELECT * FROM food_stalls f " +
            "WHERE ST_DWithin(CAST(f.location AS geography), CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), :radiusInMeters)", nativeQuery = true)
    List<FoodStall> findStallsWithinRadius(@Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusInMeters") double radiusInMeters);

        // ST_Distance: Tính toán khoảng cách chính xác cho từng dòng trong DB, sau đó
        // sort.
        // Độ phức tạp cao (O(N\log N)), không tận dụng tốt Index
        // van giữ lại nếu cần check khoảng cách chính xác 1 điểm
        @Query(value = """
                        SELECT * FROM food_stalls
                        ORDER BY ST_Distance(
                            location,
                            ST_GeogFromText('POINT(' || :longitude || ' ' || :latitude || ')')
                        )
                        LIMIT 1
                        """, nativeQuery = true)
        Optional<FoodStall> findNearestStall(
                        @Param("latitude") double latitude,
                        @Param("longitude") double longitude);

    boolean existsByName(String name);

    // Tim danh sach cac quan an ma nguoi dung dang dung trong vung ban kinh cua no
    // (Dynamic Radius)
    // Logic: Distance(User, Stall) <= Stall.triggerRadius
    @Query(value = "SELECT * FROM food_stalls f " +
            "WHERE ST_DWithin(CAST(f.location AS geography), CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), f.trigger_radius)", nativeQuery = true)
    List<FoodStall> findGeofenceMatches(@Param("latitude") double latitude,
            @Param("longitude") double longitude);
}