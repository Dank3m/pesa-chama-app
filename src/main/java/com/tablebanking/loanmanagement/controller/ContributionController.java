package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
@Tag(name = "Contributions", description = "Contribution management endpoints")
public class ContributionController {

    private final ContributionService contributionService;

    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Record a contribution payment")
    public ResponseEntity<ApiResponse<ContributionResponse>> recordContribution(
            @Valid @RequestBody RecordContributionRequest request) {
        ContributionResponse contribution = contributionService.recordContribution(request);
        return ResponseEntity.ok(ApiResponse.success("Contribution recorded", contribution));
    }

    @GetMapping("/{contributionId}")
    @Operation(summary = "Get contribution by ID")
    public ResponseEntity<ApiResponse<ContributionResponse>> getContributionById(
            @PathVariable UUID contributionId) {
        ContributionResponse contribution = contributionService.getContributionById(contributionId);
        return ResponseEntity.ok(ApiResponse.success(contribution));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get contributions by member")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributionsByMember(
            @PathVariable UUID memberId) {
        List<ContributionResponse> contributions = contributionService.getContributionsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(contributions));
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get all contributions for a cycle")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributionsByCycle(
            @PathVariable UUID cycleId) {
        List<ContributionResponse> contributions = contributionService.getContributionsByCycle(cycleId);
        return ResponseEntity.ok(ApiResponse.success(contributions));
    }

    @GetMapping("/cycle/{cycleId}/pending")
    @Operation(summary = "Get pending contributions for a cycle")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getPendingContributions(
            @PathVariable UUID cycleId) {
        List<ContributionResponse> contributions = contributionService.getPendingContributions(cycleId);
        return ResponseEntity.ok(ApiResponse.success(contributions));
    }

    @GetMapping("/member/{memberId}/cycle/{cycleId}")
    @Operation(summary = "Get member's contribution for a specific cycle")
    public ResponseEntity<ApiResponse<ContributionResponse>> getMemberContributionForCycle(
            @PathVariable UUID memberId,
            @PathVariable UUID cycleId) {
        ContributionResponse contribution = contributionService.getMemberContributionForCycle(memberId, cycleId);
        return ResponseEntity.ok(ApiResponse.success(contribution));
    }

    @PostMapping("/cycles")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Create a new contribution cycle")
    public ResponseEntity<ApiResponse<ContributionCycleResponse>> createCycle(
            @RequestParam UUID financialYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleMonth) {
        ContributionCycleResponse cycle = contributionService.createContributionCycle(financialYearId, cycleMonth);
        return ResponseEntity.ok(ApiResponse.success("Cycle created", cycle));
    }

    @GetMapping("/cycles/current")
    @Operation(summary = "Get current contribution cycle for a group")
    public ResponseEntity<ApiResponse<ContributionCycleResponse>> getCurrentCycle(
            @RequestParam UUID groupId) {
        ContributionCycleResponse cycle = contributionService.getCurrentCycle(groupId);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @GetMapping("/cycles/year/{financialYearId}")
    @Operation(summary = "Get all cycles for a financial year")
    public ResponseEntity<ApiResponse<List<ContributionCycleResponse>>> getCyclesByYear(
            @PathVariable UUID financialYearId) {
        List<ContributionCycleResponse> cycles = contributionService.getCyclesByYear(financialYearId);
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    @PostMapping("/cycles/{cycleId}/process-defaults")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Process defaulted contributions for a cycle (convert to loans)")
    public ResponseEntity<ApiResponse<Integer>> processDefaultedContributions(@PathVariable UUID cycleId) {
        int count = contributionService.processDefaultedContributions(cycleId);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("%d contributions converted to loans", count), count));
    }
}
