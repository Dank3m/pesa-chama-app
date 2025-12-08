package com.tablebanking.loanmanagement.dto.response;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private UUID id;
        private String transactionNumber;
        private UUID memberId;
        private String memberName;
        private TransactionType transactionType;
        private Instant transactionDate;
        private BigDecimal amount;
        private String debitCredit;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private GroupSummary groupSummary;
        private FinancialYearSummary currentYearSummary;
        private ContributionCycleSummary currentCycleSummary;
        private List<MemberBalanceResponse> topContributors;
        private List<LoanSummaryResponse> recentLoans;
    }

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
}
