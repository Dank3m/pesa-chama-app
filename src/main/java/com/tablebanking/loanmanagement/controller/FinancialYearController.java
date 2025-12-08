package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.FinancialYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial-years")
@RequiredArgsConstructor
@Tag(name = "Financial Years", description = "Financial year management endpoints")
public class FinancialYearController {

    private final FinancialYearService financialYearService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new financial year")
    public ResponseEntity<ApiResponse<FinancialYearResponse>> createFinancialYear(
            @Valid @RequestBody CreateFinancialYearRequest request) {
        FinancialYearResponse year = financialYearService.createFinancialYear(request);
        return ResponseEntity.ok(ApiResponse.success("Financial year created", year));
    }

    @PostMapping("/default")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create default financial year (December - November)")
    public ResponseEntity<ApiResponse<FinancialYearResponse>> createDefaultFinancialYear(
            @RequestParam UUID groupId) {
        FinancialYearResponse year = financialYearService.createDefaultFinancialYear(groupId);
        return ResponseEntity.ok(ApiResponse.success("Financial year created", year));
    }

    @GetMapping("/{yearId}")
    @Operation(summary = "Get financial year by ID")
    public ResponseEntity<ApiResponse<FinancialYearResponse>> getFinancialYearById(@PathVariable UUID yearId) {
        FinancialYearResponse year = financialYearService.getFinancialYearById(yearId);
        return ResponseEntity.ok(ApiResponse.success(year));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current financial year for a group")
    public ResponseEntity<ApiResponse<FinancialYearResponse>> getCurrentFinancialYear(@RequestParam UUID groupId) {
        FinancialYearResponse year = financialYearService.getCurrentFinancialYear(groupId);
        return ResponseEntity.ok(ApiResponse.success(year));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all financial years for a group")
    public ResponseEntity<ApiResponse<List<FinancialYearResponse>>> getFinancialYearsByGroup(
            @PathVariable UUID groupId) {
        List<FinancialYearResponse> years = financialYearService.getFinancialYearsByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/{yearId}/summary")
    @Operation(summary = "Get financial year summary with statistics")
    public ResponseEntity<ApiResponse<FinancialYearSummary>> getFinancialYearSummary(@PathVariable UUID yearId) {
        FinancialYearSummary summary = financialYearService.getFinancialYearSummary(yearId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/{yearId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close a financial year")
    public ResponseEntity<ApiResponse<FinancialYearResponse>> closeFinancialYear(@PathVariable UUID yearId) {
        FinancialYearResponse year = financialYearService.closeFinancialYear(yearId);
        return ResponseEntity.ok(ApiResponse.success("Financial year closed", year));
    }
}
