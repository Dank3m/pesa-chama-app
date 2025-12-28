package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.ExternalBorrowerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a non-member who can borrow from the group
 * with a member acting as guarantor.
 */
@Entity
@Table(name = "external_borrowers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalBorrower extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BankingGroup group;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "address")
    private String address;

    @Column(name = "employer")
    private String employer;

    @Column(name = "occupation")
    private String occupation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExternalBorrowerStatus status = ExternalBorrowerStatus.ACTIVE;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "externalBorrower", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
