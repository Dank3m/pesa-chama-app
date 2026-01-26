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
    public static class RejectLoanRequest {
        @NotNull(message = "Loan ID is required")
        private UUID loanId;

        private String reason;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateExpenseRequest {
        @NotNull(message = "Group ID is required")
        private UUID groupId;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "Expense date is required")
        private LocalDate expenseDate;

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;

        @Size(max = 100, message = "Vendor name must not exceed 100 characters")
        private String vendor;

        @Size(max = 50, message = "Receipt number must not exceed 50 characters")
        private String receiptNumber;

        private UUID loanId; // Optional: link to related loan

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateExpenseRequest {
        @NotNull(message = "Expense ID is required")
        private UUID expenseId;

        private String category;

        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        private LocalDate expenseDate;

        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;

        @Size(max = 100, message = "Vendor name must not exceed 100 characters")
        private String vendor;

        @Size(max = 50, message = "Receipt number must not exceed 50 characters")
        private String receiptNumber;

        private UUID loanId;

        private String notes;
    }

    // ==================== SETTINGS DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @Email(message = "Invalid email format")
        private String email;

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        private String phoneNumber;

        private String address;

        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserSettingsRequest {
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        private String currency;

        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        private String timezone;

        private Boolean notifyDigitalPayment;
        private Boolean notifyRecommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase, and number")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Toggle2FARequest {
        @NotNull(message = "Enabled status is required")
        private Boolean enabled;

        private String password;
    }

    // ==================== PUBLIC REGISTRATION DTOs ====================

    /**
     * Request for public registration - creates user, group, and member in one flow.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicRegistrationRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 50, message = "Username must be 4-50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase, and number")
        private String password;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^0[17]\\d{8}$", message = "Invalid Kenyan phone number format")
        private String phoneNumber;

        @Email(message = "Invalid email format")
        private String email;

        /**
         * If true, also create a new banking group with this user as admin.
         */
        private Boolean createGroup;

        /**
         * Group details when creating a new group.
         */
        private CreateGroupWithSettingsRequest groupDetails;
    }

    /**
     * Request to create a group with all settings during registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupWithSettingsRequest {
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name must not exceed 100 characters")
        private String name;

        private String description;

        // Financial Year Settings
        @Min(value = 1, message = "Start month must be between 1 and 12")
        @Max(value = 12, message = "Start month must be between 1 and 12")
        private Integer financialYearStartMonth;

        @Min(value = 1, message = "End month must be between 1 and 12")
        @Max(value = 12, message = "End month must be between 1 and 12")
        private Integer financialYearEndMonth;

        // Contribution Settings
        @DecimalMin(value = "0.01", message = "Contribution amount must be positive")
        private BigDecimal defaultContributionAmount;

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        private String currency;

        private Boolean allowPartialContributions;

        // Loan Settings
        @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
        @DecimalMax(value = "1.0000", message = "Interest rate cannot exceed 100%")
        private BigDecimal interestRate;

        private String interestRatePeriod; // DAILY, WEEKLY, MONTHLY, YEARLY

        private String interestCalculationMethod; // SIMPLE, DAILY_COMPOUND, MONTHLY_COMPOUND, FLAT_RATE

        @Min(value = 1, message = "Max loan duration must be at least 1 month")
        @Max(value = 60, message = "Max loan duration cannot exceed 60 months")
        private Integer maxLoanDurationMonths;

        @Min(value = 0, message = "Grace period cannot be negative")
        private Integer gracePeriodDays;

        @DecimalMin(value = "0.5", message = "Max loan multiplier must be at least 0.5")
        @DecimalMax(value = "10.0", message = "Max loan multiplier cannot exceed 10")
        private BigDecimal maxLoanMultiplier;

        private Boolean requireGuarantors;

        @Min(value = 1, message = "Minimum guarantors must be at least 1")
        private Integer minGuarantors;

        // Penalty Settings
        @DecimalMin(value = "0", message = "Penalty rate cannot be negative")
        @DecimalMax(value = "1.0000", message = "Penalty rate cannot exceed 100%")
        private BigDecimal latePenaltyRate;

        private Boolean enablePenalties;

        @Min(value = 1, message = "Max members must be at least 1")
        private Integer maxMembers;
    }

    /**
     * Request to update group settings (partial update).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGroupSettingsRequest {
        // Financial Year Settings
        @Min(value = 1, message = "Start month must be between 1 and 12")
        @Max(value = 12, message = "Start month must be between 1 and 12")
        private Integer financialYearStartMonth;

        @Min(value = 1, message = "End month must be between 1 and 12")
        @Max(value = 12, message = "End month must be between 1 and 12")
        private Integer financialYearEndMonth;

        // Contribution Settings
        @DecimalMin(value = "0.01", message = "Contribution amount must be positive")
        private BigDecimal defaultContributionAmount;

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        private String currency;

        private Boolean allowPartialContributions;

        // Loan Settings
        @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
        @DecimalMax(value = "1.0000", message = "Interest rate cannot exceed 100%")
        private BigDecimal interestRate;

        private String interestRatePeriod; // DAILY, WEEKLY, MONTHLY, YEARLY

        private String interestCalculationMethod;

        @Min(value = 1, message = "Max loan duration must be at least 1 month")
        @Max(value = 60, message = "Max loan duration cannot exceed 60 months")
        private Integer maxLoanDurationMonths;

        @Min(value = 0, message = "Grace period cannot be negative")
        private Integer gracePeriodDays;

        @DecimalMin(value = "0.5", message = "Max loan multiplier must be at least 0.5")
        @DecimalMax(value = "10.0", message = "Max loan multiplier cannot exceed 10")
        private BigDecimal maxLoanMultiplier;

        private Boolean requireGuarantors;

        @Min(value = 1, message = "Minimum guarantors must be at least 1")
        private Integer minGuarantors;

        // Scheduler Settings
        private String contributionCheckCron;
        private String interestAccrualCron;
        private String overdueCheckCron;

        @Min(value = 1, message = "Reminder days must be at least 1")
        @Max(value = 30, message = "Reminder days cannot exceed 30")
        private Integer reminderDaysBeforeDue;

        // Penalty Settings
        @DecimalMin(value = "0", message = "Penalty rate cannot be negative")
        @DecimalMax(value = "1.0000", message = "Penalty rate cannot exceed 100%")
        private BigDecimal latePenaltyRate;

        private Boolean enablePenalties;
    }

    // ==================== SUBSCRIPTION & BILLING DTOs ====================

    /**
     * Request to initiate subscription payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitiatePaymentRequest {
        @NotNull(message = "Plan ID is required")
        private UUID planId;

        @NotBlank(message = "Payment method is required")
        private String paymentMethod; // MPESA, BANK, PESALINK, CARD

        @Pattern(regexp = "^0[17]\\d{8}$", message = "Invalid Kenyan phone number format")
        private String phoneNumber; // Required for M-Pesa

        private String cardToken; // For card payments (tokenized)
    }

    /**
     * M-Pesa STK Push callback payload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MpesaCallbackRequest {
        private String merchantRequestID;
        private String checkoutRequestID;
        private Integer resultCode;
        private String resultDesc;
        private String mpesaReceiptNumber;
        private BigDecimal amount;
        private String transactionDate;
        private String phoneNumber;
    }

    /**
     * Card payment callback (e.g., from Stripe).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardPaymentCallbackRequest {
        private String paymentIntentId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String receiptUrl;
    }

    /**
     * Request to upgrade subscription plan.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeSubscriptionRequest {
        @NotNull(message = "Plan ID is required")
        private UUID planId;
    }

    /**
     * Request to cancel subscription.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelSubscriptionRequest {
        private String reason;
    }
}
