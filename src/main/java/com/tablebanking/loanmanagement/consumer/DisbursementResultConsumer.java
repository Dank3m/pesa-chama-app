package com.tablebanking.loanmanagement.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tablebanking.loanmanagement.entity.Loan;
import com.tablebanking.loanmanagement.entity.enums.DisbursementStatus;
import com.tablebanking.loanmanagement.repository.LoanRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Consumes disbursement result events from the payment service to update loan disbursement status.
 * Uses a separate consumer group so both this and the payment service receive messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DisbursementResultConsumer {

    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.disbursement-events:disbursement-events}",
            groupId = "pesa-chama-group-disbursement"
    )
    @Transactional
    public void consumeDisbursementResult(String message) {
        try {
            DisbursementResultEvent event = objectMapper.readValue(message, DisbursementResultEvent.class);

            // Ignore request events - only process results
            if ("LOAN_DISBURSEMENT_REQUEST".equals(event.getEventType())) {
                return;
            }

            // Only process loan disbursement results
            if (!"LOAN_DISBURSEMENT".equals(event.getSourceType())) {
                return;
            }

            UUID loanId = event.getSourceId();
            if (loanId == null) {
                log.warn("Disbursement result event has no sourceId: {}", event.getEventType());
                return;
            }

            loanRepository.findById(loanId).ifPresent(loan -> {
                if ("DISBURSEMENT_COMPLETED".equals(event.getEventType())) {
                    handleDisbursementCompleted(loan, event);
                } else if ("DISBURSEMENT_FAILED".equals(event.getEventType())) {
                    handleDisbursementFailed(loan, event);
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to parse disbursement result event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing disbursement result: {}", e.getMessage(), e);
        }
    }

    private void handleDisbursementCompleted(Loan loan, DisbursementResultEvent event) {
        loan.setDisbursementStatus(DisbursementStatus.COMPLETED);
        loan.setDisbursementReference(event.getExternalRef() != null ? event.getExternalRef() : event.getPaymentRef());
        loanRepository.save(loan);
        log.info("Loan disbursement completed: loanId={}, ref={}", loan.getId(), loan.getDisbursementReference());
    }

    private void handleDisbursementFailed(Loan loan, DisbursementResultEvent event) {
        loan.setDisbursementStatus(DisbursementStatus.FAILED);
        loan.setDisbursementFailureReason(event.getStatusDescription());
        loanRepository.save(loan);
        log.warn("Loan disbursement failed: loanId={}, reason={}", loan.getId(), event.getStatusDescription());
    }

    /**
     * Inner DTO matching the payment service's DisbursementEvent structure.
     */
    @Data
    static class DisbursementResultEvent {
        private String eventId;
        private String eventType;
        private UUID disbursementId;
        private String batchRef;
        private String paymentRef;
        private String externalRef;
        private UUID memberId;
        private String memberName;
        private UUID groupId;
        private String sourceType;
        private UUID sourceId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String statusDescription;
        private Instant timestamp;
    }
}
