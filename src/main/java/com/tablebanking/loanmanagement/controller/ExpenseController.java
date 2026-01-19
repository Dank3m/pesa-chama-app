package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management endpoints")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // In production, extract user ID from security context
        UUID createdBy = null; // TODO: Get from authenticated user
        ExpenseResponse expense = expenseService.createExpense(request, createdBy);
        return ResponseEntity.ok(ApiResponse.success("Expense created successfully", expense));
    }

    @PutMapping("/{expenseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Update an existing expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        request.setExpenseId(expenseId);
        UUID updatedBy = null; // TODO: Get from authenticated user
        ExpenseResponse expense = expenseService.updateExpense(request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", expense));
    }

    @DeleteMapping("/{expenseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Delete an expense (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID deletedBy = null; // TODO: Get from authenticated user
        expenseService.deleteExpense(expenseId, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(@PathVariable UUID expenseId) {
        ExpenseResponse expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get expenses by group with pagination")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByGroup(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseResponse> expenses = expenseService.getExpensesByGroup(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/financial-year/{financialYearId}")
    @Operation(summary = "Get expenses by financial year with pagination")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByFinancialYear(
            @PathVariable UUID financialYearId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseResponse> expenses = expenseService.getExpensesByFinancialYear(financialYearId, pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/group/{groupId}/category/{category}")
    @Operation(summary = "Get expenses by category")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpensesByCategory(
            @PathVariable UUID groupId,
            @PathVariable String category) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategory(groupId, category);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/group/{groupId}/date-range")
    @Operation(summary = "Get expenses by date range")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpensesByDateRange(
            @PathVariable UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByDateRange(groupId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/group/{groupId}/summary")
    @Operation(summary = "Get expense summary for a group")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getExpenseSummary(
            @PathVariable UUID groupId,
            @RequestParam(required = false) UUID financialYearId) {
        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(groupId, financialYearId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
