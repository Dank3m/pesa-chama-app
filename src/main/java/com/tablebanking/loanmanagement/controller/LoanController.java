package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management endpoints")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanResponse loan = loanService.applyForLoan(request);
        return ResponseEntity.ok(ApiResponse.success("Loan application submitted", loan));
    }

    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Approve a pending loan")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(
            @PathVariable UUID loanId,
            @RequestBody(required = false) ApproveLoanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ApproveLoanRequest approvalRequest = request != null ? request : 
                ApproveLoanRequest.builder().loanId(loanId).build();
        approvalRequest.setLoanId(loanId);
        
        // In real implementation, get user ID from security context
        LoanResponse loan = loanService.approveLoan(approvalRequest, null);
        return ResponseEntity.ok(ApiResponse.success("Loan approved", loan));
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Disburse an approved loan")
    public ResponseEntity<ApiResponse<LoanResponse>> disburseLoan(@PathVariable UUID loanId) {
        LoanResponse loan = loanService.disburseLoan(loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed", loan));
    }

    @PostMapping("/repay")
    @Operation(summary = "Make a loan repayment")
    public ResponseEntity<ApiResponse<LoanRepaymentResponse>> makeRepayment(
            @Valid @RequestBody LoanRepaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // In real implementation, get user ID from security context
        LoanRepaymentResponse repayment = loanService.makeRepayment(request, null);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", repayment));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable UUID loanId) {
        LoanResponse loan = loanService.getLoanById(loanId);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @GetMapping("/{loanId}/details")
    @Operation(summary = "Get loan with full details including repayments and interest accruals")
    public ResponseEntity<ApiResponse<LoanDetailResponse>> getLoanDetails(@PathVariable UUID loanId) {
        LoanDetailResponse loan = loanService.getLoanDetails(loanId);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get all loans for a member")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getLoansByMember(@PathVariable UUID memberId) {
        List<LoanResponse> loans = loanService.getLoansByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/member/{memberId}/active")
    @Operation(summary = "Get active loans for a member")
    public ResponseEntity<ApiResponse<List<LoanSummaryResponse>>> getActiveLoansByMember(
            @PathVariable UUID memberId) {
        List<LoanSummaryResponse> loans = loanService.getActiveLoansByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Get all overdue loans")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getOverdueLoans() {
        List<LoanResponse> loans = loanService.getOverdueLoans();
        return ResponseEntity.ok(ApiResponse.success(loans));
    }
}
