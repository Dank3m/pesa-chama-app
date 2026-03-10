package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPaymentMethod;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPaymentStatus;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPlanType;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {

    private final SubscriptionPlanRepository planRepository;
    private final GroupSubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final BankingGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Value("${app.billing.bank-account:123456789}")
    private String bankAccountNumber;

    @Value("${app.billing.bank-name:Family Bank}")
    private String bankName;

    @Value("${app.billing.mpesa-business-number:174379}")
    private String mpesaBusinessNumber;

    private static final AtomicLong paymentSequence = new AtomicLong(System.currentTimeMillis() % 1000000);

    /**
     * Initiate a payment for subscription upgrade.
     */
    public PaymentInitiationResponse initiatePayment(UUID groupId, InitiatePaymentRequest request, UUID userId) {
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new BusinessException("Plan not found"));

        if (plan.getName() == SubscriptionPlanType.FREE) {
            throw new BusinessException("FREE plan doesn't require payment");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Get or create current subscription
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElseGet(() -> subscriptionService.createFreeSubscription(group));

        // Calculate payment period
        LocalDate periodStart = LocalDate.now();
        LocalDate periodEnd;
        if ("YEARLY".equals(plan.getBillingPeriod())) {
            periodEnd = periodStart.plusYears(1).minusDays(1);
        } else {
            periodEnd = periodStart.plusMonths(1).minusDays(1);
        }

        // Create payment record
        String paymentNumber = generatePaymentNumber();
        SubscriptionPaymentMethod paymentMethod = SubscriptionPaymentMethod.valueOf(
                request.getPaymentMethod().toUpperCase());

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .paymentNumber(paymentNumber)
                .subscription(subscription)
                .group(group)
                .plan(plan)
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .paymentMethod(paymentMethod)
                .status(SubscriptionPaymentStatus.PENDING)
                .paidBy(user)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .metadata(new HashMap<>())
                .build();

        PaymentInitiationResponse.PaymentInitiationResponseBuilder responseBuilder =
                PaymentInitiationResponse.builder()
                        .paymentNumber(paymentNumber)
                        .paymentMethod(paymentMethod.name())
                        .amount(plan.getPrice())
                        .currency(plan.getCurrency())
                        .status("PENDING");

        // Handle based on payment method
        switch (paymentMethod) {
            case MPESA -> {
                if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                    throw new BusinessException("Phone number is required for M-Pesa payment");
                }
                Map<String, Object> mpesaResult = initiateMpesaSTKPush(
                        plan.getPrice(), request.getPhoneNumber(), paymentNumber);

                payment.setPaymentReference((String) mpesaResult.get("checkoutRequestId"));
                payment.getMetadata().put("merchantRequestId", mpesaResult.get("merchantRequestId"));
                payment.getMetadata().put("phoneNumber", request.getPhoneNumber());

                responseBuilder
                        .checkoutRequestId((String) mpesaResult.get("checkoutRequestId"))
                        .merchantRequestId((String) mpesaResult.get("merchantRequestId"));
            }
            case BANK -> {
                String reference = generateBankReference(group.getId());
                payment.setPaymentReference(reference);

                responseBuilder
                        .bankAccountNumber(bankAccountNumber)
                        .bankName(bankName)
                        .paymentReference(reference)
                        .instructions(String.format(
                                "Transfer KES %s to %s Account %s. Use reference: %s",
                                plan.getPrice(), bankName, bankAccountNumber, reference));
            }
            case PESALINK -> {
                String reference = generateBankReference(group.getId());
                payment.setPaymentReference(reference);

                responseBuilder
                        .bankAccountNumber(bankAccountNumber)
                        .bankName(bankName)
                        .paymentReference(reference)
                        .instructions(String.format(
                                "Send KES %s via PesaLink to %s Account %s. Use reference: %s",
                                plan.getPrice(), bankName, bankAccountNumber, reference));
            }
            case CARD -> {
                // In production, integrate with Stripe/Flutterwave
                String sessionId = UUID.randomUUID().toString();
                payment.setPaymentReference(sessionId);
                payment.getMetadata().put("stripeSessionId", sessionId);

                // Placeholder checkout URL - in production this would be from Stripe
                String checkoutUrl = String.format("/billing/checkout?session=%s", sessionId);

                responseBuilder
                        .checkoutUrl(checkoutUrl)
                        .clientSecret(sessionId);
            }
        }

        paymentRepository.save(payment);

        log.info("Payment initiated: {} for group {} - {} {} via {}",
                paymentNumber, group.getName(), plan.getCurrency(), plan.getPrice(), paymentMethod);

        return responseBuilder.build();
    }

    /**
     * Process M-Pesa callback (STK Push result).
     */
    public void processMpesaCallback(MpesaCallbackRequest callback) {
        log.info("Processing M-Pesa callback: checkoutRequestId={}, resultCode={}",
                callback.getCheckoutRequestID(), callback.getResultCode());

        SubscriptionPayment payment = paymentRepository
                .findPendingMpesaPayment(callback.getCheckoutRequestID())
                .orElseThrow(() -> new BusinessException("Payment not found for checkout request: "
                        + callback.getCheckoutRequestID()));

        if (callback.getResultCode() == 0) {
            // Success
            payment.markAsCompleted(callback.getMpesaReceiptNumber());
            payment.getMetadata().put("mpesaReceiptNumber", callback.getMpesaReceiptNumber());
            payment.getMetadata().put("transactionDate", callback.getTransactionDate());

            paymentRepository.save(payment);

            // Activate subscription
            activateSubscriptionAfterPayment(payment);

            log.info("M-Pesa payment successful: {} - Receipt: {}",
                    payment.getPaymentNumber(), callback.getMpesaReceiptNumber());
        } else {
            // Failed
            payment.markAsFailed(callback.getResultDesc());
            paymentRepository.save(payment);

            log.warn("M-Pesa payment failed: {} - Reason: {}",
                    payment.getPaymentNumber(), callback.getResultDesc());
        }
    }

    /**
     * Process card payment callback (e.g., from Stripe webhook).
     */
    public void processCardCallback(CardPaymentCallbackRequest callback) {
        log.info("Processing card payment callback: paymentIntentId={}, status={}",
                callback.getPaymentIntentId(), callback.getStatus());

        SubscriptionPayment payment = paymentRepository
                .findByPaymentReference(callback.getPaymentIntentId())
                .orElseThrow(() -> new BusinessException("Payment not found: " + callback.getPaymentIntentId()));

        if ("succeeded".equals(callback.getStatus())) {
            payment.markAsCompleted(callback.getPaymentIntentId());
            if (callback.getReceiptUrl() != null) {
                payment.getMetadata().put("receiptUrl", callback.getReceiptUrl());
            }

            paymentRepository.save(payment);

            // Activate subscription
            activateSubscriptionAfterPayment(payment);

            log.info("Card payment successful: {}", payment.getPaymentNumber());
        } else if ("failed".equals(callback.getStatus())) {
            payment.markAsFailed("Card payment failed");
            paymentRepository.save(payment);

            log.warn("Card payment failed: {}", payment.getPaymentNumber());
        }
    }

    /**
     * Manually confirm a bank/PesaLink payment (admin action).
     */
    public SubscriptionPaymentResponse confirmBankPayment(UUID paymentId, String bankReference) {
        SubscriptionPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            throw new BusinessException("Only pending payments can be confirmed");
        }

        payment.markAsCompleted(bankReference);
        paymentRepository.save(payment);

        // Activate subscription
        activateSubscriptionAfterPayment(payment);

        log.info("Bank payment confirmed: {} - Reference: {}", payment.getPaymentNumber(), bankReference);

        return mapToPaymentResponse(payment);
    }

    /**
     * Get payment history for a group.
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionPaymentResponse> getPaymentHistory(UUID groupId, Pageable pageable) {
        return paymentRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable)
                .map(this::mapToPaymentResponse);
    }

    /**
     * Get payment details.
     */
    @Transactional(readOnly = true)
    public SubscriptionPaymentResponse getPaymentDetails(UUID paymentId) {
        SubscriptionPayment payment = paymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        return mapToPaymentResponse(payment);
    }

    // Private helper methods

    private void activateSubscriptionAfterPayment(SubscriptionPayment payment) {
        BankingGroup group = payment.getGroup();
        SubscriptionPlan plan = payment.getPlan();
        GroupSubscription currentSubscription = subscriptionRepository.findActiveByGroupId(group.getId())
                .orElse(null);

        subscriptionService.activateSubscription(group, plan, currentSubscription);

        log.info("Subscription activated for group {} after payment {}",
                group.getName(), payment.getPaymentNumber());
    }

    private Map<String, Object> initiateMpesaSTKPush(BigDecimal amount, String phoneNumber, String accountRef) {
        // In production, this would call the M-Pesa Daraja API
        // For now, we return mock data for testing
        Map<String, Object> result = new HashMap<>();

        // Format phone number for M-Pesa (remove leading 0, add 254)
        String formattedPhone = phoneNumber;
        if (phoneNumber.startsWith("0")) {
            formattedPhone = "254" + phoneNumber.substring(1);
        }

        // Mock STK Push response
        String checkoutRequestId = "ws_CO_" + System.currentTimeMillis();
        String merchantRequestId = UUID.randomUUID().toString().substring(0, 16);

        result.put("checkoutRequestId", checkoutRequestId);
        result.put("merchantRequestId", merchantRequestId);
        result.put("responseCode", "0");
        result.put("responseDescription", "Success. Request accepted for processing");

        log.info("M-Pesa STK Push initiated: phone={}, amount={}, checkoutRequestId={}",
                formattedPhone, amount, checkoutRequestId);

        // In production:
        // 1. Call M-Pesa Daraja API /mpesa/stkpush/v1/processrequest
        // 2. Return actual CheckoutRequestID and MerchantRequestID

        return result;
    }

    private String generatePaymentNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String seqPart = String.format("%06d", paymentSequence.incrementAndGet() % 1000000);
        return "SUB" + datePart + seqPart;
    }

    private String generateBankReference(UUID groupId) {
        String groupPart = groupId.toString().substring(0, 8).toUpperCase();
        String timePart = String.valueOf(System.currentTimeMillis() % 100000);
        return "PC-" + groupPart + "-" + timePart;
    }

    private SubscriptionPaymentResponse mapToPaymentResponse(SubscriptionPayment payment) {
        return SubscriptionPaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .subscriptionId(payment.getSubscription().getId())
                .planName(payment.getPlan().getDisplayName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentReference(payment.getPaymentReference())
                .status(payment.getStatus().name())
                .paidByName(payment.getPaidBy() != null && payment.getPaidBy().getMember() != null ?
                        payment.getPaidBy().getMember().getFullName() : null)
                .paidAt(payment.getPaidAt())
                .periodStart(payment.getPeriodStart())
                .periodEnd(payment.getPeriodEnd())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
