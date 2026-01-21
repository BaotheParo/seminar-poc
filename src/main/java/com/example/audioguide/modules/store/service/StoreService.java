package com.example.audioguide.modules.store.service;

import com.example.audioguide.core.utils.GeoUtils;
import com.example.audioguide.modules.store.entity.Store;
import com.example.audioguide.modules.store.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý nghiệp vụ chính: Tìm quán xung quanh.
 */
@Service
public class StoreService {

    @Autowired
    private StoreRepository storeRepository;

    /**
     * Lấy danh sách các địa điểm trong bán kính 1km (1000m).
     *
     * @param currentLat Vĩ độ hiện tại của User
     * @param currentLng Kinh độ hiện tại của User
     * @return Danh sách các quán thỏa điều kiện
     */
    public List<Store> findNearbyStores(double currentLat, double currentLng) {
        // Bước 1: Lấy TOÀN BỘ dữ liệu từ Database lên RAM
        // (Lưu ý: Cách này chỉ dùng cho POC hoặc dữ liệu ít. Dữ liệu lớn sẽ bị tràn
        // RAM)
        List<Store> allStores = storeRepository.findAll();

        List<Store> result = new ArrayList<>();

        // Bước 2: Duyệt qua từng quán để tính khoảng cách
        for (Store store : allStores) {
            // Lấy tọa độ của quán
            double storeLat = store.getLat();
            double storeLng = store.getLng();

            // Bước 3: Gọi thuật toán Haversine (đã viết trong GeoUtils)
            double distance = GeoUtils.calculateDistance(currentLat, currentLng, storeLat, storeLng);

            // In log ra để dễ kiểm tra (Optional)
            // System.out.println("Khoảng cách tới " + store.getName() + " là: " + distance
            // + "m");

            // Bước 4: Kiểm tra điều kiện < 1000m
            if (distance <= 1000) {
                result.add(store);
            }
        }

        // Bước 5: Trả về danh sách kết quả
        return result;
    }
}
