package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.FoodStallImportDto;
import com.foodstreet.voice.service.FoodStallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Data Management", description = "Internal APIs for syncing and importing POI data")
public class AdminImportController {

    private final FoodStallService foodStallService;

    @PostMapping("/import-json")
    @Operation(summary = "Import curated data from JSON")
    public ResponseEntity<?> importCuratedData(@RequestBody List<FoodStallImportDto> request) {
        log.info("Da nhan du lieu import {} quan an", request.size());
        try {
            int count = foodStallService.importStalls(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "imported_count", count,
                    "message", "Da import " + count + " quan an"));
        } catch (Exception e) {
            log.error("Import that bai", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }
}
