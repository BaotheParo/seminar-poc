package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.stall.StallOwnerUpsertRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.service.FoodStallService;
import com.foodstreet.voice.service.StallOwnerService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stall-owner")
@RequiredArgsConstructor
public class StallOwnerController {

    private final StallOwnerService stallOwnerService;
    private final FoodStallService foodStallService;

    @GetMapping("/my-stall")
    public ResponseEntity<?> getMyStall(Authentication authentication) {
        FoodStall stall = stallOwnerService.getMyStall(authentication.getName());
        if (stall == null) {
            return ResponseEntity.ok(Map.of(
                    "hasStall", false,
                    "message", "You have not created a stall yet"
            ));
        }

        FoodStallResponse response = foodStallService.getStallById(stall.getId());
        response.setStatus(stall.getStatus() == null ? null : stall.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-stall")
    public ResponseEntity<Map<String, String>> submitUpdate(
            @Valid @RequestBody StallOwnerUpsertRequest request,
            Authentication authentication
    ) {
        stallOwnerService.submitStallUpdate(authentication.getName(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Yeu cau da duoc gui. Vui long cho admin phe duyet"
        ));
    }
}
