package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.enums.ExternalBorrowerStatus;
import com.tablebanking.loanmanagement.service.GuaranteedLoanService;
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
@RequestMapping("/api/v1/guaranteed-loans")
@RequiredArgsConstructor
@Tag(name = "Guaranteed Loans", description = "Manage loans to non-members with member guarantors")
public class GuaranteedLoanController {

    private final GuaranteedLoanService guaranteedLoanService;

    // ==================== External Borrower Endpoints ====================

    @PostMapping("/borrowers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Register a new external borrower")
    public ResponseEntity<ApiResponse<ExternalBorrowerResponse>> createExternalBorrower(
            @Valid @RequestBody CreateExternalBorrowerRequest request) {
        ExternalBorrowerResponse borrower = guaranteedLoanService.createExternalBorrower(request);
        return ResponseEntity.ok(ApiResponse.success("External borrower registered", borrower));
    }

    @GetMapping("/borrowers/{borrowerId}")
    @Operation(summary = "Get external borrower by ID")
    public ResponseEntity<ApiResponse<ExternalBorrowerResponse>> getExternalBorrower(
            @PathVariable UUID borrowerId) {
        ExternalBorrowerResponse borrower = guaranteedLoanService.getExternalBorrower(borrowerId);
        return ResponseEntity.ok(ApiResponse.success(borrower));
    }

    @GetMapping("/borrowers")
    @Operation(summary = "Get all external borrowers for a group")
    public ResponseEntity<ApiResponse<List<ExternalBorrowerResponse>>> getExternalBorrowers(
            @RequestParam UUID groupId) {
        List<ExternalBorrowerResponse> borrowers = guaranteedLoanService.getExternalBorrowers(groupId);
        return ResponseEntity.ok(ApiResponse.success(borrowers));
    }

    @PatchMapping("/borrowers/{borrowerId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Update external borrower status")
    public ResponseEntity<ApiResponse<ExternalBorrowerResponse>> updateBorrowerStatus(
            @PathVariable UUID borrowerId,
            @RequestParam ExternalBorrowerStatus status) {
        ExternalBorrowerResponse borrower = guaranteedLoanService.updateBorrowerStatus(borrowerId, status);
        return ResponseEntity.ok(ApiResponse.success("Borrower status updated", borrower));
    }

    // ==================== Guaranteed Loan Endpoints ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Create a new guaranteed loan for an external borrower")
    public ResponseEntity<ApiResponse<GuaranteedLoanResponse>> createGuaranteedLoan(
            @Valid @RequestBody CreateGuaranteedLoanRequest request) {
        GuaranteedLoanResponse loan = guaranteedLoanService.createGuaranteedLoan(request);
        return ResponseEntity.ok(ApiResponse.success("Guaranteed loan created", loan));
    }

    @GetMapping
    @Operation(summary = "Get all guaranteed loans for a group")
    public ResponseEntity<ApiResponse<List<GuaranteedLoanResponse>>> getGuaranteedLoans(
            @RequestParam UUID groupId) {
        List<GuaranteedLoanResponse> loans = guaranteedLoanService.getGuaranteedLoans(groupId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/borrower/{borrowerId}")
    @Operation(summary = "Get loans by external borrower")
    public ResponseEntity<ApiResponse<List<GuaranteedLoanResponse>>> getLoansByBorrower(
            @PathVariable UUID borrowerId) {
        List<GuaranteedLoanResponse> loans = guaranteedLoanService.getLoansByExternalBorrower(borrowerId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    // ==================== Guarantor Endpoints ====================

    @PostMapping("/guarantors")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Add a guarantor to an existing loan")
    public ResponseEntity<ApiResponse<LoanGuarantorResponse>> addGuarantor(
            @Valid @RequestBody AddGuarantorRequest request) {
        LoanGuarantorResponse guarantor = guaranteedLoanService.addGuarantor(request);
        return ResponseEntity.ok(ApiResponse.success("Guarantor added", guarantor));
    }

    @GetMapping("/guarantors/member/{memberId}/exposure")
    @Operation(summary = "Get member's guarantor exposure summary")
    public ResponseEntity<ApiResponse<GuarantorExposureResponse>> getGuarantorExposure(
            @PathVariable UUID memberId) {
        GuarantorExposureResponse exposure = guaranteedLoanService.getGuarantorExposure(memberId);
        return ResponseEntity.ok(ApiResponse.success(exposure));
    }

    @PostMapping("/{loanId}/process-default")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process guarantor liability when borrower defaults")
    public ResponseEntity<ApiResponse<Void>> processGuarantorLiability(
            @PathVariable UUID loanId) {
        guaranteedLoanService.processGuarantorLiability(loanId);
        return ResponseEntity.ok(ApiResponse.success("Guarantor liability processed", null));
    }
}
