package com.tablebanking.loanmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tablebanking.loanmanagement.entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ResponseDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupResponse {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal contributionAmount;
        private String currency;
        private BigDecimal interestRate;
        private Integer financialYearStartMonth;
        private Integer maxMembers;
        private Integer currentMemberCount;
        private Boolean isActive;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse {
        private UUID id;
        private UUID groupId;
        private String memberNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String nationalId;
        private LocalDate dateOfBirth;
        private String address;
        private LocalDate joinDate;
        private MemberStatus status;
        private Boolean isAdmin;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDetailResponse {
        private MemberResponse member;
        private MemberBalanceResponse currentBalance;
        private List<ContributionResponse> recentContributions;
        private List<LoanSummaryResponse> activeLoans;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialYearResponse {
        private UUID id;
        private UUID groupId;
        private String yearName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private Boolean isClosed;
        private BigDecimal totalContributions;
        private BigDecimal totalLoansDisbursed;
        private BigDecimal totalInterestEarned;
        private BigDecimal totalExpenses;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionCycleResponse {
        private UUID id;
        private UUID financialYearId;
        private LocalDate cycleMonth;
        private LocalDate dueDate;
        private BigDecimal expectedAmount;
        private CycleStatus status;
        private BigDecimal totalCollected;
        private Integer totalMembers;
        private Integer paidCount;
        private Integer pendingCount;
        private Boolean isProcessed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionResponse {
        private UUID id;
        private UUID memberId;
        private String memberName;
        private UUID cycleId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate cycleMonth;
        private BigDecimal expectedAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingAmount;
        private ContributionStatus status;
        private Instant paymentDate;
        private Boolean convertedToLoan;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanResponse {
        private UUID id;
        private String loanNumber;
        private UUID memberId;
        private String memberName;
        private LoanType loanType;
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private BigDecimal dailyInterestRate;
        private LocalDate disbursementDate;
        private LocalDate expectedEndDate;
        private LocalDate actualEndDate;
        private BigDecimal totalInterestAccrued;
        private BigDecimal totalAmountDue;
        private BigDecimal totalAmountPaid;
        private BigDecimal outstandingBalance;
        private LoanStatus status;
        private Integer daysActive;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanSummaryResponse {
        private UUID id;
        private String loanNumber;
        private LoanType loanType;
        private BigDecimal principalAmount;
        private BigDecimal outstandingBalance;
        private LoanStatus status;
        private LocalDate disbursementDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanDetailResponse {
        private LoanResponse loan;
        private List<LoanRepaymentResponse> repayments;
        private List<InterestAccrualResponse> recentAccruals;
        private LoanScheduleResponse schedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanRepaymentResponse {
        private UUID id;
        private UUID loanId;
        private Integer paymentNumber;
        private Instant paymentDate;
        private BigDecimal amount;
        private BigDecimal principalPortion;
        private BigDecimal interestPortion;
        private BigDecimal balanceAfter;
        private String paymentMethod;
        private String referenceNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestAccrualResponse {
        private UUID id;
        private LocalDate accrualDate;
        private BigDecimal openingBalance;
        private BigDecimal interestAmount;
        private BigDecimal closingBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanScheduleResponse {
        private BigDecimal totalPrincipal;
        private BigDecimal estimatedTotalInterest;
        private BigDecimal estimatedTotalPayable;
        private LocalDate estimatedEndDate;
        private BigDecimal dailyInterestAmount;
        private BigDecimal monthlyInterestRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberBalanceResponse {
        private UUID id;
        private UUID memberId;
        private UUID financialYearId;
        private String yearName;
        private BigDecimal totalContributions;
        private BigDecimal totalLoansTaken;
        private BigDecimal totalLoanRepayments;
        private BigDecimal outstandingLoanBalance;
        private BigDecimal shareValue;
        private Instant lastCalculatedAt;
    }

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class TransactionResponse {
//        private UUID id;
//        private String transactionNumber;
//        private UUID memberId;
//        private String memberName;
//        private TransactionType transactionType;
//        private Instant transactionDate;
//        private BigDecimal amount;
//        private String debitCredit;
//        private String description;
//    }

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class DashboardResponse {
//        private GroupSummary groupSummary;
//        private FinancialYearSummary currentYearSummary;
//        private ContributionCycleSummary currentCycleSummary;
//        private List<MemberBalanceResponse> topContributors;
//        private List<LoanSummaryResponse> recentLoans;
//    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSummary {
        private UUID groupId;
        private String groupName;
        private Integer totalMembers;
        private Integer activeMembers;
        private BigDecimal contributionAmount;
        private BigDecimal interestRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialYearSummary {
        private UUID yearId;
        private String yearName;
        private BigDecimal totalContributions;
        private BigDecimal totalLoansDisbursed;
        private BigDecimal totalInterestEarned;
        private BigDecimal totalOutstandingLoans;
        private BigDecimal netPosition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionCycleSummary {
        private UUID cycleId;
        private LocalDate cycleMonth;
        private LocalDate dueDate;
        private BigDecimal expectedTotal;
        private BigDecimal collectedTotal;
        private Double collectionRate;
        private Integer pendingCount;
        private Integer daysRemaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserResponse user;
        private List<GroupMembershipResponse> availableGroups;
        private UUID defaultGroupId;
        private Boolean hasMultipleGroups;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMembershipResponse {
        private UUID groupId;
        private String groupName;
        private UUID memberId;
        private String memberNumber;
        private String role;
        private Boolean isDefault;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleSummaryResponse {
        private UUID cycleId;
        private LocalDate cycleMonth;
        private BigDecimal totalExpected;
        private BigDecimal totalCollected;
        private BigDecimal outstandingAmount;
        private int paidCount;
        private int partialCount;
        private int pendingCount;
        private int defaultedCount;
        private BigDecimal collectionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String username;
        private UserRole role;
        private MemberResponse member;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private Instant timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("Success")
                    .data(data)
                    .timestamp(Instant.now())
                    .build();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(Instant.now())
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvancePaymentStatusResponse {
        private UUID memberId;
        private UUID financialYearId;
        private int monthsPaidAhead;
        private BigDecimal totalPaidAhead;
        private LocalDate lastPaidMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalBorrowerResponse {
        private UUID id;
        private UUID groupId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phoneNumber;
        private String email;
        private String nationalId;
        private String address;
        private String employer;
        private String occupation;
        private String status;
        private int activeLoansCount;
        private BigDecimal totalOutstanding;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanGuarantorResponse {
        private UUID id;
        private UUID loanId;
        private String loanNumber;
        private UUID memberId;
        private String memberName;
        private String memberPhone;
        private BigDecimal guaranteedAmount;
        private BigDecimal guaranteePercentage;
        private BigDecimal effectiveGuaranteedAmount;
        private String status;
        private Instant acceptedAt;
        private Instant releasedAt;
        private BigDecimal amountPaidOnBehalf;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuarantorExposureResponse {
        private UUID memberId;
        private String memberName;
        private int activeGuaranteesCount;
        private BigDecimal totalGuaranteedAmount;
        private BigDecimal totalPaidOnBehalf;
        private List<LoanGuarantorResponse> activeGuarantees;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuaranteedLoanResponse {
        private UUID id;
        private String loanNumber;

        // Borrower info (external)
        private UUID externalBorrowerId;
        private String borrowerName;
        private String borrowerPhone;

        // Loan details
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private LocalDate disbursementDate;
        private LocalDate expectedEndDate;
        private BigDecimal totalInterestAccrued;
        private BigDecimal totalAmountDue;
        private BigDecimal totalAmountPaid;
        private BigDecimal outstandingBalance;
        private String status;

        // Guarantor info
        private List<LoanGuarantorResponse> guarantors;
        private String primaryGuarantorName;

        private String notes;
        private Instant createdAt;
    }
/*
     * Response for validating registration token
 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberRegistrationInfoResponse {

        private String memberId;
        private String memberNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String groupId;
        private String groupName;
        private boolean tokenValid;
        private String tokenExpiry;
        private String suggestedRole;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private BigDecimal totalBalance;
        private BigDecimal totalContributions;
        private int activeLoans;
        private int memberCount;
        private BigDecimal collectionRate;
        private List<MonthlyActivityDTO> monthlyActivity;
        private List<FundAllocationDTO> fundAllocation;
        private List<TransactionDTO> recentTransactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDashboardResponse {
        private UUID memberId;
        private String memberName;
        private BigDecimal totalContributions;
        private BigDecimal outstandingLoans;
        private int activeLoansCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberLoanStatsResponse {
        private UUID memberId;
        private BigDecimal totalOutstanding;
        private int activeLoansCount;
        private BigDecimal totalBorrowed;
        private BigDecimal totalRepaid;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisbursementStatsResponse {
        private int pendingCount;
        private BigDecimal pendingAmount;
        private int approvedCount;
        private BigDecimal approvedAmount;
        private BigDecimal totalDisbursed;
        private int activeLoansCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyActivityDTO {
        private String name;        // "Jan", "Feb", etc.
        private String month;       // "2025-01"
        private BigDecimal contributions;
        private BigDecimal disbursements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundAllocationDTO {
        private String name;        // "Loans Disbursed", "Interest Earned", etc.
        private int value;          // Percentage
        private String fill;        // Color hex
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDTO {
        private String id;
        private String type;        // "CONTRIBUTION", "DISBURSEMENT"
        private BigDecimal amount;
        private String date;
        private String description;
        private String memberName;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeCycleResponse {
        private UUID cycleId;
        private LocalDate cycleMonth;
        private int contributionsCreated;
        private BigDecimal expectedAmountPerMember;
        private BigDecimal totalExpected;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private UUID id;
        private String transactionNumber;
        private UUID groupId;
        private UUID memberId;
        private String memberName;
        private TransactionType transactionType;
        private Instant transactionDate;
        private BigDecimal amount;
        private String debitCredit;  // "CREDIT" or "DEBIT"
        private String description;
        private String referenceType;
        private UUID referenceId;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseResponse {
        private UUID id;
        private UUID groupId;
        private UUID financialYearId;
        private String yearName;
        private String category;
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String description;
        private String vendor;
        private String receiptNumber;
        private UUID loanId;
        private String loanNumber;
        private String notes;
        private UUID createdBy;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSummaryResponse {
        private UUID groupId;
        private UUID financialYearId;
        private BigDecimal totalExpenses;
        private int expenseCount;
        private BigDecimal transactionFees;
        private BigDecimal agmExpenses;
        private BigDecimal administrativeExpenses;
        private BigDecimal otherExpenses;
    }

    // ==================== SETTINGS RESPONSE DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResponse {
        private UUID id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String address;
        private LocalDate dateOfBirth;
        private String memberNumber;
        private UUID groupId;
        private String groupName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSettingsResponse {
        private UUID id;
        private String currency;
        private String timezone;
        private Boolean notifyDigitalPayment;
        private Boolean notifyRecommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySettingsResponse {
        private Boolean twoFactorEnabled;
        private Instant lastLogin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStatsResponse {
        private long total;
        private long active;
        private long inactive;
        private long suspended;
        private long left;
        private long admins;
    }

    // ==================== PUBLIC REGISTRATION & GROUP SETTINGS DTOs ====================

    /**
     * Response for public registration with group creation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserResponse user;
        private GroupResponse group;
        private GroupSettingsResponse groupSettings;
    }

    /**
     * Complete group settings response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSettingsResponse {
        private UUID id;
        private UUID groupId;
        private String groupName;

        // Financial Year Settings
        private Integer financialYearStartMonth;
        private Integer financialYearEndMonth;

        // Contribution Settings
        private BigDecimal defaultContributionAmount;
        private String currency;
        private Boolean allowPartialContributions;

        // Loan Settings
        private BigDecimal interestRate;
        private String interestRatePeriod;
        private String interestCalculationMethod;
        private Integer maxLoanDurationMonths;
        private Integer gracePeriodDays;
        private BigDecimal maxLoanMultiplier;
        private Boolean requireGuarantors;
        private Integer minGuarantors;

        // Scheduler Settings
        private String contributionCheckCron;
        private String interestAccrualCron;
        private String overdueCheckCron;
        private Integer reminderDaysBeforeDue;

        // Penalty Settings
        private BigDecimal latePenaltyRate;
        private Boolean enablePenalties;

        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Summary of all groups for SUPER_ADMIN dashboard.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllGroupsSummaryResponse {
        private int totalGroups;
        private int activeGroups;
        private int totalMembers;
        private int activeMembers;
        private BigDecimal totalContributions;
        private BigDecimal totalLoansOutstanding;
        private List<GroupSummaryItem> groups;
    }

    /**
     * Individual group summary for super admin view.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSummaryItem {
        private UUID id;
        private String name;
        private Boolean isActive;
        private int memberCount;
        private int activeMemberCount;
        private BigDecimal contributionAmount;
        private BigDecimal interestRate;
        private String currency;
        private BigDecimal totalContributions;
        private BigDecimal totalLoansOutstanding;
        private Instant createdAt;
    }

    // ==================== SUBSCRIPTION & BILLING DTOs ====================

    /**
     * Subscription plan details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionPlanResponse {
        private UUID id;
        private String name;
        private String displayName;
        private String description;
        private BigDecimal price;
        private String currency;
        private String billingPeriod;
        private Integer maxMembers;
        private Boolean unlimitedMembers;
        private java.util.Map<String, Boolean> features;
        private Integer sortOrder;
        private Boolean isCurrentPlan;
    }

    /**
     * Group's current subscription status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSubscriptionResponse {
        private UUID id;
        private UUID groupId;
        private String groupName;
        private SubscriptionPlanResponse plan;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean autoRenew;
        private Boolean isGrandfathered;
        private Boolean isExpiringSoon;
        private Integer daysUntilExpiry;
        private Instant createdAt;
    }

    /**
     * Payment initiation response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInitiationResponse {
        private String paymentNumber;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;
        private String status;

        // M-Pesa specific
        private String checkoutRequestId;
        private String merchantRequestId;

        // Bank transfer specific
        private String bankAccountNumber;
        private String bankName;
        private String paymentReference;
        private String instructions;

        // Card payment specific
        private String checkoutUrl;
        private String clientSecret;
    }

    /**
     * Subscription payment record.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionPaymentResponse {
        private UUID id;
        private String paymentNumber;
        private UUID subscriptionId;
        private String planName;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String paymentReference;
        private String status;
        private String paidByName;
        private Instant paidAt;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String failureReason;
        private Instant createdAt;
    }

    /**
     * Feature access check response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureAccessResponse {
        private String feature;
        private Boolean hasAccess;
        private String currentPlan;
        private String requiredPlan;
        private String message;
    }

    /**
     * Subscription overview for billing page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionOverviewResponse {
        private GroupSubscriptionResponse currentSubscription;
        private List<SubscriptionPlanResponse> availablePlans;
        private List<SubscriptionPaymentResponse> recentPayments;
        private Integer currentMemberCount;
        private Integer maxMembersAllowed;
        private Boolean canAddMoreMembers;
    }
}
