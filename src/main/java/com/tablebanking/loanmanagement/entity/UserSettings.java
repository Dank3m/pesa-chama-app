package com.tablebanking.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Africa/Nairobi";

    @Column(name = "notify_digital_payment", nullable = false)
    @Builder.Default
    private Boolean notifyDigitalPayment = true;

    @Column(name = "notify_recommendations", nullable = false)
    @Builder.Default
    private Boolean notifyRecommendations = true;
}
