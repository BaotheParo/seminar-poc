package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.FoodStallResponse;
import com.foodstreet.voice.dto.stall.StallOwnerUpsertRequest;
import com.foodstreet.voice.entity.FoodStall;
import com.foodstreet.voice.entity.FoodStallUpdate;
import com.foodstreet.voice.service.FoodStallService;
import com.foodstreet.voice.service.StallOwnerService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/my-stalls")
    public ResponseEntity<Map<String, Object>> getMyStalls(Authentication authentication) {
        String username = authentication.getName();
        List<FoodStall> stalls = stallOwnerService.getMyStalls(username);

        List<Map<String, Object>> stallItems = stalls.stream().map(stall -> {
            FoodStallResponse response = foodStallService.getStallById(stall.getId());
            response.setStatus(stall.getStatus() == null ? null : stall.getStatus().name());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stall", response);
            item.put("pendingUpdate", stallOwnerService
                    .getLatestPendingUpdateForStall(username, stall.getId())
                    .map(this::toPendingPayload)
                    .orElse(null));
            return item;
        }).toList();

        List<Map<String, Object>> pendingCreateRequests = stallOwnerService.getPendingCreateRequests(username)
                .stream()
                .map(this::toPendingPayload)
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hasStall", !stalls.isEmpty());
        payload.put("stalls", stallItems);
        payload.put("pendingCreateRequests", pendingCreateRequests);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/my-stall")
    public ResponseEntity<?> getMyStall(Authentication authentication) {
        String username = authentication.getName();
        FoodStall stall = stallOwnerService.getMyStall(username);
        if (stall == null) {
            return ResponseEntity.ok(Map.of(
                    "hasStall", false,
                    "message", "You have not created a stall yet"
            ));
        }

        FoodStallResponse response = foodStallService.getStallById(stall.getId());
        response.setStatus(stall.getStatus() == null ? null : stall.getStatus().name());

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("hasStall", true);
    payload.put("stall", response);
    payload.put("pendingUpdate", stallOwnerService
        .getLatestPendingUpdateForStall(username, stall.getId())
        .map(this::toPendingPayload)
        .orElse(null));
    return ResponseEntity.ok(payload);
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

    @DeleteMapping("/request/{requestId}")
    public ResponseEntity<Map<String, String>> cancelRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        stallOwnerService.cancelRequest(authentication.getName(), requestId);
        return ResponseEntity.ok(Map.of(
                "message", "Yeu cau da duoc huy"
        ));
    }

    private Map<String, Object> toPendingPayload(FoodStallUpdate update) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", update.getId());
        payload.put("status", update.getStatus() == null ? null : update.getStatus().name());
        payload.put("changes", update.getChanges());
        payload.put("reason", update.getReason());
        payload.put("createdAt", update.getCreatedAt());
        return payload;
    }
}
