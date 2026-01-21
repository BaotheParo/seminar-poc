package com.example.audioguide.modules.store.controller;

import com.example.audioguide.modules.store.entity.Store;
import com.example.audioguide.modules.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API Controller để Frontend gọi tới.
 */
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * API Tìm quán gần đây.
     * URL: GET /api/v1/stores/nearby?lat=10.7769&lng=106.7009
     *
     * @param lat Vĩ độ user gửi lên
     * @param lng Kinh độ user gửi lên
     * @return Danh sách quán
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<Store>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng) {
        // Gọi Service xử lý
        List<Store> nearbyStores = storeService.findNearbyStores(lat, lng);

        // Trả về JSON cho Client (HTTP 200 OK)
        return ResponseEntity.ok(nearbyStores);
    }
}
