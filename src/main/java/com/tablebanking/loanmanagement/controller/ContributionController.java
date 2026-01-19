package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.Contribution;
import com.tablebanking.loanmanagement.entity.ContributionCycle;
import com.tablebanking.loanmanagement.entity.enums.ContributionStatus;
import com.tablebanking.loanmanagement.entity.enums.CycleStatus;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.ContributionCycleRepository;
import com.tablebanking.loanmanagement.repository.ContributionRepository;
import com.tablebanking.loanmanagement.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
@Tag(name = "Contributions", description = "Contribution management endpoints")
public class ContributionController {

    private final ContributionService contributionService;
    private final ContributionRepository contributionRepository;
    private final ContributionCycleRepository cycleRepository;

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

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Get all contributions by group (Admin/Treasurer only)")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributionsByGroup(
            @PathVariable UUID groupId) {
        List<ContributionResponse> contributions = contributionService.getContributionsByGroup(groupId);
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

    @GetMapping("/member/{memberId}/advance-status")
    @Operation(summary = "Get member's advance payment status - shows how many months are paid ahead")
    public ResponseEntity<ApiResponse<AdvancePaymentStatusResponse>> getAdvancePaymentStatus(
            @PathVariable UUID memberId,
            @RequestParam UUID financialYearId) {
        AdvancePaymentStatusResponse status = contributionService.getAdvancePaymentStatus(memberId, financialYearId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    @PostMapping("/record-advance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Record an advance contribution payment for multiple months")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> recordAdvanceContribution(
            @RequestParam UUID memberId,
            @RequestParam UUID startCycleId,
            @RequestParam int numberOfMonths,
            @RequestParam(required = false) String referenceNumber) {
        List<ContributionResponse> contributions = contributionService.recordAdvanceContribution(
                memberId, startCycleId, numberOfMonths, referenceNumber);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Advance contribution recorded for %d months", numberOfMonths), contributions));
    }
    @GetMapping("/member/{memberId}/year/{financialYearId}")
    @Operation(summary = "Get all contributions by member for a specific financial year")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributionsByMemberForYear(
            @PathVariable UUID memberId,
            @PathVariable UUID financialYearId) {
        List<ContributionResponse> contributions = contributionService.getContributionsByMemberForYear(memberId, financialYearId);
        return ResponseEntity.ok(ApiResponse.success(contributions));
    }

    @GetMapping("/cycles/{cycleId}/summary")
    @Operation(summary = "Get contribution cycle summary with payment statistics")
    public ResponseEntity<ApiResponse<CycleSummaryResponse>> getCycleSummary(
            @PathVariable UUID cycleId) {

        ContributionCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new BusinessException("Contribution cycle not found"));

        // Count contributions by status
        long paidCount = contributionRepository.countByCycleIdAndStatus(cycleId, ContributionStatus.PAID);
        long partialCount = contributionRepository.countByCycleIdAndStatus(cycleId, ContributionStatus.PARTIAL);
        long pendingCount = contributionRepository.countByCycleIdAndStatus(cycleId, ContributionStatus.PENDING);
        long defaultedCount = contributionRepository.countByCycleIdAndStatus(cycleId, ContributionStatus.DEFAULTED);

        // Calculate totals
        BigDecimal totalExpected = contributionRepository.sumExpectedByCycle(cycleId);
        BigDecimal totalCollected = contributionRepository.sumPaidByCycle(cycleId);

        totalExpected = totalExpected != null ? totalExpected : BigDecimal.ZERO;
        totalCollected = totalCollected != null ? totalCollected : BigDecimal.ZERO;

        CycleSummaryResponse response = CycleSummaryResponse.builder()
                .cycleId(cycleId)
                .cycleMonth(cycle.getCycleMonth())
                .totalExpected(totalExpected)
                .totalCollected(totalCollected)
                .outstandingAmount(totalExpected.subtract(totalCollected))
                .paidCount((int) paidCount)
                .partialCount((int) partialCount)
                .pendingCount((int) pendingCount)
                .defaultedCount((int) defaultedCount)
                .collectionRate(totalExpected.compareTo(BigDecimal.ZERO) > 0
                        ? totalCollected.divide(totalExpected, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Cycle summary retrieved", response));
    }

    private ContributionCycleResponse mapToCycleResponse(ContributionCycle cycle) {
        return ContributionCycleResponse.builder()
                .id(cycle.getId())
                .cycleMonth(cycle.getCycleMonth())
                .dueDate(cycle.getDueDate())
                .expectedAmount(cycle.getExpectedAmount())
                .totalCollected(cycle.getTotalCollected())
                .status(cycle.getStatus())
                .build();
    }

    @PostMapping("/cycles/{cycleId}/initialize")
    @Operation(summary = "Initialize contribution records for all active members in a cycle")
    public ResponseEntity<ApiResponse<InitializeCycleResponse>> initializeCycleContributions(
            @PathVariable UUID cycleId) {

        ContributionCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new BusinessException("Contribution cycle not found"));

        if (cycle.getStatus() != CycleStatus.OPEN) {
            throw new BusinessException("Cannot initialize contributions for a closed cycle");
        }

        List<Contribution> contributions = contributionService.initializeCycleContributions(cycle);

        InitializeCycleResponse response = InitializeCycleResponse.builder()
                .cycleId(cycleId)
                .cycleMonth(cycle.getCycleMonth())
                .contributionsCreated(contributions.size())
                .expectedAmountPerMember(cycle.getExpectedAmount())
                .totalExpected(cycle.getExpectedAmount().multiply(BigDecimal.valueOf(contributions.size())))
                .message(contributions.isEmpty()
                        ? "All active members already have contribution records"
                        : contributions.size() + " contribution records created")
                .build();

        return ResponseEntity.ok(ApiResponse.success("Cycle contributions initialized", response));
    }
}
