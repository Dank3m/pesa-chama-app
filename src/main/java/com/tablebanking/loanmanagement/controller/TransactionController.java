package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.service.TransactionExportService;
import com.tablebanking.loanmanagement.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExportService exportService;

    @GetMapping
    @Operation(summary = "Get transactions with pagination, filtering and search")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        PagedResponse<TransactionResponse> transactions;

        if (memberId != null) {
            transactions = transactionService.getTransactionsByMember(memberId, type, transactionType, search, pageable);
        } else {
            transactions = transactionService.getTransactionsByGroup(groupId, type, transactionType, search, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", transactions));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID transactionId) {

        TransactionResponse transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved", transaction));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get transaction summary")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> getTransactionSummary(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID financialYearId) {

        TransactionSummaryResponse summary = transactionService.getTransactionSummary(groupId, financialYearId);
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved", summary));
    }

    // ==================== EXPORT ENDPOINTS ====================

    @GetMapping("/export/csv")
    @Operation(summary = "Export transactions to CSV")
    public ResponseEntity<Resource> exportToCsv(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        byte[] data = exportService.exportToCsv(groupId, memberId, type, search);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_" + getTimestamp() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export transactions to Excel")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        byte[] data = exportService.exportToExcel(groupId, memberId, type, search);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_" + getTimestamp() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export transactions to PDF")
    public ResponseEntity<Resource> exportToPdf(
            @RequestParam UUID groupId,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        byte[] data = exportService.exportToPdf(groupId, memberId, type, search);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_" + getTimestamp() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionSummaryResponse {
        private java.math.BigDecimal totalCredits;
        private java.math.BigDecimal totalDebits;
        private java.math.BigDecimal netBalance;
        private long totalTransactions;
    }
}