package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview")
    public ResponseEntity<ApiResponse<DashboardResponse>> getOverview(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID financialYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard retrieved",
                dashboardService.getOverview(groupId, financialYearId)));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get member dashboard")
    public ResponseEntity<ApiResponse<MemberDashboardResponse>> getMemberDashboard(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Member dashboard retrieved",
                dashboardService.getMemberDashboard(memberId)));
    }
}






















