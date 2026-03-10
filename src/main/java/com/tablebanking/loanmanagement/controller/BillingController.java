package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing", description = "Subscription billing and payment endpoints")
public class BillingController {

    private final BillingService billingService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate a subscription payment")
    public ResponseEntity<ApiResponse<PaymentInitiationResponse>> initiatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InitiatePaymentRequest request) {
        UUID groupId = getGroupId(userDetails);
        UUID userId = getUserId(userDetails);

        PaymentInitiationResponse response = billingService.initiatePayment(groupId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @PostMapping("/callback/mpesa")
    @Operation(summary = "M-Pesa payment callback (IPN)")
    public ResponseEntity<ApiResponse<Void>> mpesaCallback(
            @RequestBody MpesaCallbackRequest callback) {
        log.info("Received M-Pesa callback: checkoutRequestId={}", callback.getCheckoutRequestID());
        billingService.processMpesaCallback(callback);
        return ResponseEntity.ok(ApiResponse.success("Callback processed", null));
    }

    @PostMapping("/callback/card")
    @Operation(summary = "Card payment callback (webhook)")
    public ResponseEntity<ApiResponse<Void>> cardCallback(
            @RequestBody CardPaymentCallbackRequest callback) {
        log.info("Received card payment callback: paymentIntentId={}", callback.getPaymentIntentId());
        billingService.processCardCallback(callback);
        return ResponseEntity.ok(ApiResponse.success("Callback processed", null));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually confirm a bank/PesaLink payment")
    public ResponseEntity<ApiResponse<SubscriptionPaymentResponse>> confirmBankPayment(
            @PathVariable UUID paymentId,
            @RequestParam String bankReference) {
        SubscriptionPaymentResponse payment = billingService.confirmBankPayment(paymentId, bankReference);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", payment));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get payment history for the group")
    public ResponseEntity<ApiResponse<PagedResponse<SubscriptionPaymentResponse>>> getPaymentHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID groupId = getGroupId(userDetails);
        Pageable pageable = PageRequest.of(page, size);

        Page<SubscriptionPaymentResponse> payments = billingService.getPaymentHistory(groupId, pageable);

        PagedResponse<SubscriptionPaymentResponse> response = PagedResponse.<SubscriptionPaymentResponse>builder()
                .content(payments.getContent())
                .page(payments.getNumber())
                .size(payments.getSize())
                .totalElements(payments.getTotalElements())
                .totalPages(payments.getTotalPages())
                .first(payments.isFirst())
                .last(payments.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", response));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment details")
    public ResponseEntity<ApiResponse<SubscriptionPaymentResponse>> getPaymentDetails(
            @PathVariable UUID paymentId) {
        SubscriptionPaymentResponse payment = billingService.getPaymentDetails(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved", payment));
    }

    // Helper methods

    private UUID getGroupId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        if (user.getMember() == null || user.getMember().getGroup() == null) {
            throw new BusinessException("User is not associated with a group");
        }
        return user.getMember().getGroup().getId();
    }

    private UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        return user.getId();
    }
}
