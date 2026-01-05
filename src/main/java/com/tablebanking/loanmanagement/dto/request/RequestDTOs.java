package com.tablebanking.loanmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class RequestDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupRequest {
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        private String name;

        private String description;

        @DecimalMin(value = "0.01", message = "Contribution amount must be positive")
        private BigDecimal contributionAmount;

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        private String currency;

        @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
        @DecimalMax(value = "1.0000", message = "Interest rate cannot exceed 100%")
        private BigDecimal interestRate;

        @Min(value = 1, message = "Start month must be between 1 and 12")
        @Max(value = 12, message = "Start month must be between 1 and 12")
        private Integer financialYearStartMonth;

        @Min(value = 1, message = "Max members must be at least 1")
        private Integer maxMembers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMemberRequest {
        @NotNull(message = "Group ID is required")
        private UUID groupId;

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        private String phoneNumber;

        @Size(max = 20, message = "National ID must not exceed 20 characters")
        private String nationalId;

        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;

        private String address;

        private Boolean isAdmin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordContributionRequest {
        @NotNull(message = "Member ID is required")
        private UUID memberId;

        @NotNull(message = "Cycle ID is required")
        private UUID cycleId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        private String paymentMethod;
        private String referenceNumber;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanApplicationRequest {
        @NotNull(message = "Member ID is required")
        private UUID memberId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "100", message = "Minimum loan amount is 100")
        private BigDecimal amount;

        @Min(value = 1, message = "Duration must be at least 1 month")
        @Max(value = 12, message = "Maximum duration is 12 months")
        private Integer durationMonths;

        private String purpose;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanRepaymentRequest {
        @NotNull(message = "Loan ID is required")
        private UUID loanId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        private String paymentMethod;
        private String referenceNumber;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproveLoanRequest {
        @NotNull(message = "Loan ID is required")
        private UUID loanId;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFinancialYearRequest {
        @NotNull(message = "Group ID is required")
        private UUID groupId;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        private String yearName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateContributionAmountRequest {
        @NotNull(message = "Group ID is required")
        private UUID groupId;

        @NotNull(message = "New amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal newAmount;

        private LocalDate effectiveFrom;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterUserRequest {
        @NotNull(message = "Member ID is required")
        private UUID memberId;

        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 50, message = "Username must be 4-50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase, and number")
        private String password;

        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateExternalBorrowerRequest {
        @NotNull
        private UUID groupId;

        @NotBlank
        @Size(max = 50)
        private String firstName;

        @NotBlank
        @Size(max = 50)
        private String lastName;

        @NotBlank
        @Pattern(regexp = "^0[17]\\d{8}$", message = "Invalid phone number format")
        private String phoneNumber;

        @Email
        private String email;

        @Size(max = 20)
        private String nationalId;

        private String address;
        private String employer;
        private String occupation;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGuaranteedLoanRequest {
        @NotNull
        private UUID externalBorrowerId;

        @NotNull
        private UUID guarantorMemberId;

        @NotNull
        @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000")
        private BigDecimal principalAmount;

        @NotNull
        private LocalDate disbursementDate;

        /**
         * Optional: percentage of loan this guarantor covers (default 100%).
         * Can add multiple guarantors with different percentages.
         */
        @DecimalMin(value = "1")
        @DecimalMax(value = "100")
        private BigDecimal guaranteePercentage;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddGuarantorRequest {
        @NotNull
        private UUID loanId;

        @NotNull
        private UUID memberId;

        /**
         * Fixed amount to guarantee (optional).
         */
        private BigDecimal guaranteedAmount;

        /**
         * Percentage of loan to guarantee (default 100%).
         */
        @DecimalMin(value = "1")
        @DecimalMax(value = "100")
        private BigDecimal guaranteePercentage;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberRegistrationRequest {

        @NotBlank(message = "Member ID is required")
        private String memberId;

        @NotBlank(message = "Registration token is required")
        private String token;

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Role is required")
        private String role;
    }

    /**
     * Request to resend registration notification
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResendRegistrationRequest {

        @NotBlank(message = "Member ID is required")
        private String memberId;

        private String channel; // EMAIL, SMS, or BOTH
    }
}
