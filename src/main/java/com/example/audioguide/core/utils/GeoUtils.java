package com.example.audioguide.core.utils;

/**
 * Class tiện ích chứa các công thức toán học tính toán địa lý.
 * Đây là CORE LOGIC của ứng dụng LBS (Location Based Service) sơ khai.
 */
public class GeoUtils {

    // Bán kính Trái Đất trung bình (Khoảng 6371 km)
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Tính khoảng cách giữa 2 điểm tọa độ bằng công thức Haversine.
     * Công thức này tính khoảng cách đường chim bay trên mặt cầu (không tính địa
     * hình đồi núi).
     *
     * @param lat1 Vĩ độ điểm 1 (User)
     * @param lng1 Kinh độ điểm 1 (User)
     * @param lat2 Vĩ độ điểm 2 (Quán)
     * @param lng2 Kinh độ điểm 2 (Quán)
     * @return Khoảng cách tính bằng mét (m)
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // -- Bước 1: Chuyển đổi tọa độ từ Độ (Degrees) sang Radian --
        // Vì các hàm lượng giác trong Java (Math.sin, Math.cos) dùng Radian
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        // Tính độ chênh lệch giữa 2 tọa độ
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        // -- Bước 2: Áp dụng công thức Haversine --
        // a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
        // Trong đó: φ là vĩ độ (latitude), λ là kinh độ (longitude)

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        // c = 2 ⋅ atan2( √a, √(1−a) )
        // Tính góc trung tâm (angular distance) bằng radian
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // -- Bước 3: Tính khoảng cách d = R ⋅ c --
        double distanceKm = EARTH_RADIUS_KM * c;

        // Trả về mét cho chính xác (nhân với 1000)
        return distanceKm * 1000;
    }
}
