package com.tablebanking.loanmanagement.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tablebanking.loanmanagement.dto.request.RequestDTOs;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.repository.*;
import com.tablebanking.loanmanagement.service.LoanService;
import com.tablebanking.loanmanagement.service.StkPushService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for payment events from the Payment Service
 * Handles contribution payments and loan repayments
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final LoanService loanService;
    private final StkPushService stkPushService;
    private final ObjectMapper objectMapper;

    /**
     * Handle contribution payment events from payment service
     */
    @KafkaListener(
            topics = "${app.kafka.topics.contribution-events:contribution-events}",
            groupId = "${spring.kafka.consumer.group-id:pesa-chama-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleContributionPayment(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received contribution payment event: key={}", record.key());

        try {
            ContributionPaymentEvent event = objectMapper.readValue(
                    record.value(), ContributionPaymentEvent.class);

            // Only handle CONTRIBUTION_PAYMENT events
            if (!"CONTRIBUTION_PAYMENT".equals(event.getEventType())) {
                log.debug("Ignoring non-payment event: {}", event.getEventType());
                ack.acknowledge();
                return;
            }

            log.info("Processing contribution payment: contributionId={}, amount={}", 
                    event.getContributionId(), event.getAmount());

            // Find the contribution
            Contribution contribution = contributionRepository.findById(event.getContributionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Contribution not found: " + event.getContributionId()));

            // Update contribution with payment
            BigDecimal newAmountPaid = contribution.getPaidAmount().add(event.getAmount());
            contribution.setPaidAmount(newAmountPaid);

            // Calculate outstanding
            BigDecimal outstanding = contribution.getExpectedAmount().subtract(newAmountPaid);

            // Update status based on payment
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                contribution.setStatus(ContributionStatus.PAID);
                contribution.setPaymentDate(Instant.now());
                log.info("Contribution fully paid: contributionId={}", contribution.getId());
            } else {
                contribution.setStatus(ContributionStatus.PARTIAL);
                log.info("Partial contribution payment: contributionId={}, outstanding={}", 
                        contribution.getId(), outstanding);
            }

            // Record payment reference
            String notes = contribution.getNotes() != null ? contribution.getNotes() : "";
            contribution.setNotes(notes + " | Payment: " + event.getPaymentReference() + 
                    " (" + event.getAmount() + ")");

            contributionRepository.save(contribution);

            ack.acknowledge();
            log.info("Contribution payment processed successfully: contributionId={}", 
                    event.getContributionId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse contribution payment event: {}", e.getMessage());
            ack.acknowledge(); // Don't retry malformed messages
        } catch (Exception e) {
            log.error("Error processing contribution payment: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }

    /**
     * Handle loan repayment events from payment service
     */
    @KafkaListener(
            topics = "${app.kafka.topics.loan-events:loan-events}",
            groupId = "${spring.kafka.consumer.group-id:table-banking-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleLoanRepayment(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received loan repayment event: key={}", record.key());

        try {
            LoanRepaymentEvent event = objectMapper.readValue(
                    record.value(), LoanRepaymentEvent.class);

            // Only handle LOAN_REPAYMENT events
            if (!"LOAN_REPAYMENT".equals(event.getEventType())) {
                log.debug("Ignoring non-repayment event: {}", event.getEventType());
                ack.acknowledge();
                return;
            }

            log.info("Processing loan repayment: loanId={}, amount={}", 
                    event.getLoanId(), event.getAmount());

            // Use the existing loan service to record repayment

            RequestDTOs.LoanRepaymentRequest repaymentRequest = RequestDTOs.LoanRepaymentRequest.builder()
                    .loanId(event.getLoanId())
                    .amount(event.getAmount())
                    .paymentMethod(event.getPaymentMode())
                    .referenceNumber(event.getPaymentReference())
                    .notes("Repayment via "+ event.getPaymentMode() +" payment event")
                    .build();

            loanService.makeRepayment(repaymentRequest, null);
            ack.acknowledge();
            log.info("Loan repayment processed successfully: loanId={}", event.getLoanId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse loan repayment event: {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing loan repayment: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }

    /**
     * Handle STK Push collection result events from payment service
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-events:payment-events}",
            groupId = "pesa-chama-group-stk",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCollectionResult(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received collection result event: key={}", record.key());
        try {
            CollectionResultEvent event = objectMapper.readValue(record.value(), CollectionResultEvent.class);

            // Only handle collection result events
            if (!List.of("COLLECTION_COMPLETED", "COLLECTION_FAILED", "STK_SENT", "COLLECTION_CANCELLED").contains(event.getEventType())) {
                log.debug("Ignoring non-collection event: {}", event.getEventType());
                ack.acknowledge();
                return;
            }

            // Update Redis tracking via StkPushService
            StkPushService.CollectionResultEvent stkEvent = new StkPushService.CollectionResultEvent();
            stkEvent.setEventId(event.getEventId());
            stkEvent.setEventType(event.getEventType());
            stkEvent.setCollectionRef(event.getCollectionRef());
            stkEvent.setCollectionType(event.getCollectionType());
            stkEvent.setSourceId(event.getSourceId());
            stkEvent.setMemberId(event.getMemberId());
            stkEvent.setGroupId(event.getGroupId());
            stkEvent.setAmount(event.getAmount());
            stkEvent.setOriginalAmount(event.getOriginalAmount());
            stkEvent.setPhoneNumber(event.getPhoneNumber());
            stkEvent.setMpesaReceiptNumber(event.getMpesaReceiptNumber());
            stkEvent.setStatus(event.getStatus());
            stkEvent.setStatusDescription(event.getStatusDescription());
            stkEvent.setTimestamp(event.getTimestamp());
            stkPushService.updateStatus(stkEvent);

            if ("COLLECTION_COMPLETED".equals(event.getEventType())) {
                // Use originalAmount (the exact requested amount) for recording.
                // The M-Pesa amount (event.getAmount()) is rounded up to the nearest whole number.
                // The excess (mpesaAmount - originalAmount) is credited to member balance as a contribution.
                BigDecimal mpesaAmount = event.getAmount();
                BigDecimal originalAmount = event.getOriginalAmount() != null ? event.getOriginalAmount() : mpesaAmount;
                BigDecimal excess = mpesaAmount.subtract(originalAmount);

                if ("LOAN_REPAYMENT".equals(event.getCollectionType())) {
                    // Auto-record loan repayment with original amount
                    RequestDTOs.LoanRepaymentRequest repaymentRequest = RequestDTOs.LoanRepaymentRequest.builder()
                            .loanId(event.getSourceId())
                            .amount(originalAmount)
                            .paymentMethod("MPESA")
                            .referenceNumber(event.getMpesaReceiptNumber())
                            .notes("STK Push payment - " + event.getCollectionRef())
                            .build();
                    loanService.makeRepayment(repaymentRequest, null);
                    log.info("Auto-recorded loan repayment: loanId={}, originalAmount={}, mpesaAmount={}",
                            event.getSourceId(), originalAmount, mpesaAmount);
                } else if ("CONTRIBUTION".equals(event.getCollectionType())) {
                    // Auto-record contribution with original amount
                    Contribution contribution = contributionRepository.findById(event.getSourceId())
                            .orElseThrow(() -> new RuntimeException("Contribution not found: " + event.getSourceId()));
                    BigDecimal newAmountPaid = contribution.getPaidAmount().add(originalAmount);
                    contribution.setPaidAmount(newAmountPaid);
                    BigDecimal outstanding = contribution.getExpectedAmount().subtract(newAmountPaid);
                    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                        contribution.setStatus(ContributionStatus.PAID);
                        contribution.setPaymentDate(Instant.now());
                    } else {
                        contribution.setStatus(ContributionStatus.PARTIAL);
                    }
                    String notes = contribution.getNotes() != null ? contribution.getNotes() : "";
                    contribution.setNotes(notes + " | M-Pesa STK: " + event.getMpesaReceiptNumber() +
                            " (paid " + mpesaAmount + ", applied " + originalAmount + ")");
                    contributionRepository.save(contribution);
                    log.info("Auto-recorded contribution: contributionId={}, originalAmount={}, mpesaAmount={}",
                            event.getSourceId(), originalAmount, mpesaAmount);
                }

                // Credit the rounding excess to member balance (adds to contributions/share value)
                if (excess.compareTo(BigDecimal.ZERO) > 0 && event.getMemberId() != null) {
                    creditExcessToMemberBalance(event.getMemberId(), excess, event.getCollectionRef());
                }
            }

            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse collection result event: {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing collection result: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }

    /**
     * Credit the rounding excess from M-Pesa to the member's balance as a contribution.
     * This handles the difference between the rounded-up M-Pesa amount and the original requested amount.
     */
    private void creditExcessToMemberBalance(UUID memberId, BigDecimal excess, String collectionRef) {
        try {
            Optional<MemberBalance> balanceOpt = memberBalanceRepository.findLatestByMemberId(memberId);
            if (balanceOpt.isPresent()) {
                MemberBalance balance = balanceOpt.get();
                balance.addContribution(excess);
                memberBalanceRepository.save(balance);
                log.info("Credited M-Pesa rounding excess to member balance: memberId={}, excess={}, collectionRef={}",
                        memberId, excess, collectionRef);
            } else {
                log.warn("No member balance found for memberId={}, unable to credit rounding excess of {}",
                        memberId, excess);
            }
        } catch (Exception e) {
            log.error("Failed to credit rounding excess to member balance: memberId={}, excess={}, error={}",
                    memberId, excess, e.getMessage());
        }
    }

    // Event DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionResultEvent {
        private String eventId;
        private String eventType;
        private String collectionRef;
        private String collectionType;
        private UUID sourceId;
        private UUID memberId;
        private UUID groupId;
        private BigDecimal amount;
        private BigDecimal originalAmount;
        private String phoneNumber;
        private String mpesaReceiptNumber;
        private String status;
        private String statusDescription;
        private Instant timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionPaymentEvent {
        private String eventId;
        private String eventType;
        private UUID contributionId;
        private UUID memberId;
        private String memberName;
        private UUID groupId;
        private BigDecimal amount;
        private String paymentReference;
        private String paymentMode;
        private Instant timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanRepaymentEvent {
        private String eventId;
        private String eventType;
        private UUID loanId;
        private UUID memberId;
        private String memberName;
        private UUID groupId;
        private BigDecimal amount;
        private String paymentReference;
        private String paymentMode;
        private Instant timestamp;
    }
}
