package com.tablebanking.loanmanagement.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tablebanking.loanmanagement.dto.request.RequestDTOs;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.repository.*;
import com.tablebanking.loanmanagement.service.LoanService;
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
    private final LoanService loanService;
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

    // Event DTOs
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
