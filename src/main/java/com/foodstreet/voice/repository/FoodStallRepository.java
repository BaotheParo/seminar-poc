package com.foodstreet.voice.repository;

import com.foodstreet.voice.dto.projection.GeofenceMatchProjection;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.StallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodStallRepository extends JpaRepository<FoodStall, Long>, JpaSpecificationExecutor<FoodStall> {

        // ST_DWithin: hoạt động như một bộ lọc chỉ quét những điểm nằm trong vùng index
        // => rat tot khi dữ liệu lớn
        @Query(value = "SELECT * FROM food_stalls f " +
                        "WHERE (f.status IS NULL OR f.status = 'ACTIVE') " +
                        "AND ST_DWithin(CAST(f.location AS geography), CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), :radiusInMeters) " +
                        "ORDER BY f.id ASC", nativeQuery = true)
        List<FoodStall> findStallsWithinRadius(@Param("latitude") double latitude,
                        @Param("longitude") double longitude,
                        @Param("radiusInMeters") double radiusInMeters);

        // ST_Distance: Tính toán khoảng cách chính xác cho từng dòng trong DB, sau đó
        // sort.
        // Độ phức tạp cao (O(N\log N)), không tận dụng tốt Index
        // van giữ lại nếu cần check khoảng cách chính xác 1 điểm
        @Query(value = """
                        SELECT * FROM food_stalls
                        WHERE (status IS NULL OR status = 'ACTIVE')
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
        // MUST SORT by: priority ASC (first), then distance ASC (second).
        @Query(value = """
            SELECT 
                id, 
                name, 
                description, 
                address,
                CAST(id AS varchar) || '_vi.mp3' as "audioUrl", 
                trigger_radius as "triggerRadius", 
                priority,
                localization_status as "localizationStatus",
                ST_Y(CAST(location AS geometry)) as "latitude", 
                ST_X(CAST(location AS geometry)) as "longitude",
                ST_Distance(CAST(location AS geography), CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distance"
            FROM food_stalls
            WHERE ST_DWithin(CAST(location AS geography), CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), :radius)
                            AND (status IS NULL OR status = 'ACTIVE')
            ORDER BY distance ASC, priority ASC
            LIMIT 5
            """, nativeQuery = true)
        List<GeofenceMatchProjection> findGeofenceMatches(
                @Param("latitude") double latitude, 
                @Param("longitude") double longitude,
                @Param("radius") double radius);

        @Query("SELECT MAX(f.createdAt) FROM FoodStall f")
        Optional<java.time.LocalDateTime> findMaxCreatedAt();

        Optional<FoodStall> findByIdAndOwnerId(Long id, Long ownerId);

        List<FoodStall> findByStatus(StallStatus status);
}
