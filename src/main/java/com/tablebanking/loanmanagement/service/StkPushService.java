package com.tablebanking.loanmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tablebanking.loanmanagement.dto.request.RequestDTOs;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs;
import com.tablebanking.loanmanagement.entity.Member;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.event.StkPushRequestEvent;
import com.tablebanking.loanmanagement.repository.ContributionRepository;
import com.tablebanking.loanmanagement.repository.LoanRepository;
import com.tablebanking.loanmanagement.repository.MemberRepository;
import com.tablebanking.loanmanagement.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StkPushService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final ContributionRepository contributionRepository;

    @Value("${app.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    private static final String REDIS_KEY_PREFIX = "stk:";
    private static final long REDIS_TTL_MINUTES = 30;

    /**
     * Initiate an STK Push request for loan repayment or contribution payment.
     */
    public ResponseDTOs.StkPushResponse initiateStkPush(RequestDTOs.StkPushRequest request, UUID userId) {
        log.info("Initiating STK Push: sourceType={}, sourceId={}, amount={}",
                request.getSourceType(), request.getSourceId(), request.getAmount());

        // Validate source exists
        validateSource(request.getSourceType(), request.getSourceId());

        // Look up user and member info
        User user = userRepository.findByIdWithMemberAndGroup(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Member member = user.getMember();
        if (member == null) {
            throw new RuntimeException("User has no linked member profile");
        }

        // Generate collection reference
        String collectionRef = "COL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Build event
        StkPushRequestEvent event = StkPushRequestEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("STK_PUSH_REQUEST")
                .collectionRef(collectionRef)
                .collectionType(request.getSourceType())
                .sourceId(request.getSourceId())
                .memberId(member.getId())
                .memberName(member.getFullName())
                .groupId(member.getGroup().getId())
                .amount(request.getAmount())
                .originalAmount(request.getAmount())
                .phoneNumber(request.getPhoneNumber())
                .timestamp(Instant.now())
                .build();

        // Store tracking info in Redis
        try {
            StkTrackingData trackingData = StkTrackingData.builder()
                    .collectionRef(collectionRef)
                    .collectionType(request.getSourceType())
                    .sourceId(request.getSourceId())
                    .amount(request.getAmount())
                    .originalAmount(request.getAmount())
                    .status("INITIATED")
                    .phoneNumber(request.getPhoneNumber())
                    .build();

            String trackingJson = objectMapper.writeValueAsString(trackingData);
            String redisKey = REDIS_KEY_PREFIX + collectionRef;
            redisTemplate.opsForValue().set(redisKey, trackingJson, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Stored STK tracking data in Redis: key={}", redisKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize STK tracking data: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate STK Push", e);
        }

        // Publish event to Kafka
        kafkaTemplate.send(paymentEventsTopic, collectionRef, event);
        log.info("Published STK Push request event: collectionRef={}, topic={}", collectionRef, paymentEventsTopic);

        return ResponseDTOs.StkPushResponse.builder()
                .collectionRef(collectionRef)
                .status("INITIATED")
                .message("STK Push request initiated. Please check your phone for the M-Pesa prompt.")
                .build();
    }

    /**
     * Get the current status of an STK Push request.
     */
    public ResponseDTOs.StkPushStatusResponse getStatus(String collectionRef) {
        String redisKey = REDIS_KEY_PREFIX + collectionRef;
        String trackingJson = redisTemplate.opsForValue().get(redisKey);

        if (trackingJson == null) {
            return ResponseDTOs.StkPushStatusResponse.builder()
                    .collectionRef(collectionRef)
                    .status("NOT_FOUND")
                    .statusDescription("No STK Push request found for the given reference")
                    .build();
        }

        try {
            StkTrackingData trackingData = objectMapper.readValue(trackingJson, StkTrackingData.class);
            return ResponseDTOs.StkPushStatusResponse.builder()
                    .collectionRef(trackingData.getCollectionRef())
                    .collectionType(trackingData.getCollectionType())
                    .sourceId(trackingData.getSourceId())
                    .amount(trackingData.getAmount())
                    .originalAmount(trackingData.getOriginalAmount())
                    .status(trackingData.getStatus())
                    .statusDescription(trackingData.getStatusDescription())
                    .mpesaReceiptNumber(trackingData.getMpesaReceiptNumber())
                    .completedAt(trackingData.getCompletedAt())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse STK tracking data from Redis: {}", e.getMessage());
            return ResponseDTOs.StkPushStatusResponse.builder()
                    .collectionRef(collectionRef)
                    .status("ERROR")
                    .statusDescription("Failed to retrieve status")
                    .build();
        }
    }

    /**
     * Update the status of an STK Push request based on a collection result event.
     */
    public void updateStatus(CollectionResultEvent event) {
        String redisKey = REDIS_KEY_PREFIX + event.getCollectionRef();
        String trackingJson = redisTemplate.opsForValue().get(redisKey);

        if (trackingJson == null) {
            log.warn("No tracking data found for collectionRef={}, creating new entry", event.getCollectionRef());
        }

        try {
            StkTrackingData trackingData;
            if (trackingJson != null) {
                trackingData = objectMapper.readValue(trackingJson, StkTrackingData.class);
            } else {
                trackingData = StkTrackingData.builder()
                        .collectionRef(event.getCollectionRef())
                        .collectionType(event.getCollectionType())
                        .sourceId(event.getSourceId())
                        .amount(event.getAmount())
                        .phoneNumber(event.getPhoneNumber())
                        .build();
            }

            // Map event type to status
            String status;
            switch (event.getEventType()) {
                case "COLLECTION_COMPLETED":
                    status = "COMPLETED";
                    break;
                case "COLLECTION_FAILED":
                    status = "FAILED";
                    break;
                case "STK_SENT":
                    status = "STK_SENT";
                    break;
                case "COLLECTION_CANCELLED":
                    status = "CANCELLED";
                    break;
                default:
                    status = event.getEventType();
            }

            trackingData.setStatus(status);
            trackingData.setMpesaReceiptNumber(event.getMpesaReceiptNumber());
            trackingData.setStatusDescription(event.getStatusDescription());
            if ("COLLECTION_COMPLETED".equals(event.getEventType()) ||
                "COLLECTION_FAILED".equals(event.getEventType()) ||
                "COLLECTION_CANCELLED".equals(event.getEventType())) {
                trackingData.setCompletedAt(event.getTimestamp());
            }

            String updatedJson = objectMapper.writeValueAsString(trackingData);
            redisTemplate.opsForValue().set(redisKey, updatedJson, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Updated STK tracking status: collectionRef={}, status={}", event.getCollectionRef(), status);
        } catch (JsonProcessingException e) {
            log.error("Failed to update STK tracking data: {}", e.getMessage());
        }
    }

    private void validateSource(String sourceType, UUID sourceId) {
        if ("LOAN_REPAYMENT".equals(sourceType)) {
            if (!loanRepository.existsById(sourceId)) {
                throw new RuntimeException("Loan not found: " + sourceId);
            }
        } else if ("CONTRIBUTION".equals(sourceType)) {
            if (!contributionRepository.existsById(sourceId)) {
                throw new RuntimeException("Contribution not found: " + sourceId);
            }
        } else {
            throw new RuntimeException("Invalid source type: " + sourceType + ". Must be LOAN_REPAYMENT or CONTRIBUTION");
        }
    }

    // Inner tracking data class for Redis storage
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StkTrackingData {
        private String collectionRef;
        private String collectionType;
        private UUID sourceId;
        private BigDecimal amount;
        private BigDecimal originalAmount;
        private String status;
        private String phoneNumber;
        private String mpesaReceiptNumber;
        private String statusDescription;
        private Instant completedAt;
    }

    // Inner DTO for collection result events
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
}
