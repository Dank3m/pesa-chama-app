package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@Tag(name = "Investments", description = "Investment management endpoints")
public class InvestmentController {

    private final InvestmentService investmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Create a new investment")
    public ResponseEntity<ApiResponse<InvestmentResponse>> createInvestment(
            @Valid @RequestBody CreateInvestmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID createdBy = null; // TODO: Get from authenticated user
        InvestmentResponse investment = investmentService.createInvestment(request, createdBy);
        return ResponseEntity.ok(ApiResponse.success("Investment created successfully", investment));
    }

    @PutMapping("/{investmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Update an existing investment")
    public ResponseEntity<ApiResponse<InvestmentResponse>> updateInvestment(
            @PathVariable UUID investmentId,
            @Valid @RequestBody UpdateInvestmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        request.setInvestmentId(investmentId);
        UUID updatedBy = null; // TODO: Get from authenticated user
        InvestmentResponse investment = investmentService.updateInvestment(request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success("Investment updated successfully", investment));
    }

    @DeleteMapping("/{investmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an investment (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteInvestment(
            @PathVariable UUID investmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID deletedBy = null; // TODO: Get from authenticated user
        investmentService.deleteInvestment(investmentId, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("Investment deleted successfully", null));
    }

    @GetMapping("/{investmentId}")
    @Operation(summary = "Get investment by ID")
    public ResponseEntity<ApiResponse<InvestmentResponse>> getInvestmentById(@PathVariable UUID investmentId) {
        InvestmentResponse investment = investmentService.getInvestmentById(investmentId);
        return ResponseEntity.ok(ApiResponse.success(investment));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get investments by group with pagination")
    public ResponseEntity<ApiResponse<Page<InvestmentResponse>>> getInvestmentsByGroup(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "investmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InvestmentResponse> investments = investmentService.getInvestmentsByGroup(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(investments));
    }

    @GetMapping("/group/{groupId}/summary")
    @Operation(summary = "Get investment summary for a group")
    public ResponseEntity<ApiResponse<InvestmentSummaryResponse>> getInvestmentSummary(
            @PathVariable UUID groupId) {
        InvestmentSummaryResponse summary = investmentService.getInvestmentSummary(groupId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
