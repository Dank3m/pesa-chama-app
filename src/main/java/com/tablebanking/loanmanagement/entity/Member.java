package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.MemberStatus;
import com.tablebanking.loanmanagement.entity.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BankingGroup group;

    @Column(name = "member_number", nullable = false, length = 20)
    private String memberNumber;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "join_date", nullable = false)
    @Builder.Default
    private LocalDate joinDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    // ==================== REGISTRATION TOKEN FIELDS ====================

    @Column(name = "registration_token", length = 64)
    private String registrationToken;

    @Column(name = "registration_token_expiry")
    private LocalDateTime registrationTokenExpiry;

    @Column(name = "registration_token_used")
    @Builder.Default
    private Boolean registrationTokenUsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_notification_channel", length = 10)
    private NotificationChannel registrationNotificationChannel;

    // ==================== RELATIONSHIPS ====================

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Contribution> contributions = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberBalance> balances = new ArrayList<>();

    // ==================== HELPER METHODS ====================

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /**
     * Generate and set a new registration token
     */
    public void generateRegistrationToken(int expiryDays) {
        this.registrationToken = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        this.registrationTokenExpiry = LocalDateTime.now().plusDays(expiryDays);
        this.registrationTokenUsed = false;
    }

    /**
     * Check if registration token is valid (exists, not used, not expired)
     */
    public boolean isRegistrationTokenValid() {
        return registrationToken != null
                && !Boolean.TRUE.equals(registrationTokenUsed)
                && registrationTokenExpiry != null
                && LocalDateTime.now().isBefore(registrationTokenExpiry);
    }

    /**
     * Check if provided token matches and is valid
     */
    public boolean isRegistrationTokenValid(String token) {
        return isRegistrationTokenValid() && registrationToken.equals(token);
    }

    /**
     * Mark registration token as used
     */
    public void markRegistrationTokenUsed() {
        this.registrationTokenUsed = true;
    }

    /**
     * Build registration link
     */
    public String getRegistrationLink(String baseUrl) {
        return String.format("%s/register?memberId=%s&token=%s", baseUrl, getId(), registrationToken);
    }
}