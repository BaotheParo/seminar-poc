package com.foodstreet.voice.controller;

import com.foodstreet.voice.dto.stall.FoodStallUpdateResponse;
import com.foodstreet.voice.dto.stall.StallUpdateReviewRequest;
import com.foodstreet.voice.entity.FoodStallUpdateStatus;
import com.foodstreet.voice.service.AdminApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    @GetMapping("/pending")
    public List<FoodStallUpdateResponse> pending() {
        return adminApprovalService.getPendingApprovals();
    }

    @GetMapping("/history")
    public List<FoodStallUpdateResponse> history(
            @RequestParam(defaultValue = "APPROVED") FoodStallUpdateStatus status
    ) {
        return adminApprovalService.getHistory(status);
    }

    @PostMapping("/{id}/approve")
    public FoodStallUpdateResponse approve(@PathVariable Long id, Authentication authentication) {
        return adminApprovalService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public FoodStallUpdateResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) StallUpdateReviewRequest request,
            Authentication authentication
    ) {
        String reason = request == null ? "" : request.getReason();
        return adminApprovalService.reject(id, authentication.getName(), reason);
    }
}
