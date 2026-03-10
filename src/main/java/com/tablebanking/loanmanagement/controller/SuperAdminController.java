package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/super")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin", description = "Platform-wide administration endpoints for SUPER_ADMIN role")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/groups")
    @Operation(summary = "Get all groups with details")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups() {
        log.info("Super admin fetching all groups");
        List<GroupResponse> groups = superAdminService.getAllGroups();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/groups/active")
    @Operation(summary = "Get all active groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getActiveGroups() {
        log.info("Super admin fetching active groups");
        List<GroupResponse> groups = superAdminService.getActiveGroups();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/groups/summary")
    @Operation(summary = "Get summary of all groups for dashboard")
    public ResponseEntity<ApiResponse<AllGroupsSummaryResponse>> getAllGroupsSummary() {
        log.info("Super admin fetching groups summary");
        AllGroupsSummaryResponse summary = superAdminService.getAllGroupsSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
