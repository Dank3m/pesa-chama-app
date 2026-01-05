package com.tablebanking.loanmanagement.event;

import com.tablebanking.loanmanagement.entity.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new member is created
 * Triggers notification service to send registration link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRegistrationEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    private String memberId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String groupId;
    private String groupName;
    
    // Registration link details
    private String registrationToken;
    private String registrationLink;
    private LocalDateTime tokenExpiry;

    // Notification preferences
    private NotificationChannel preferredChannel;
    
    public static MemberRegistrationEvent create(
            String memberId,
            String memberNumber,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String groupId,
            String groupName,
            String registrationToken,
            String baseUrl,
            NotificationChannel channel
    ) {
        String registrationLink = String.format("%s/register?memberId=%s&token=%s", 
                baseUrl, memberId, registrationToken);
        
        return MemberRegistrationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MEMBER_REGISTRATION")
                .timestamp(LocalDateTime.now())
                .memberId(memberId)
                .memberNumber(memberNumber)
                .firstName(firstName)
                .lastName(lastName)
                .fullName(firstName + " " + lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .groupId(groupId)
                .groupName(groupName)
                .registrationToken(registrationToken)
                .registrationLink(registrationLink)
                .tokenExpiry(LocalDateTime.now().plusDays(7)) // Token valid for 7 days
                .preferredChannel(channel)
                .build();
    }
}
