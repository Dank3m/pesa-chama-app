package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal API for payment service integration
 * Used by payment service to look up member info and financial status
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Lookup", description = "Member lookup for payment service integration")
public class MemberLookupController {

    private final MemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final ContributionCycleRepository cycleRepository;

    /**
     * Look up member by various identifiers
     */
    @GetMapping("/lookup")
    @Operation(summary = "Look up member by identifier")
    public ResponseEntity<MemberFinancialStatus> lookupMember(
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) String identifier) {
        
        log.info("Member lookup: idNumber={}, phoneNumber={}, memberId={}", 
                idNumber, phoneNumber, memberId);

        Member member = null;

        // Find by ID number
        if (idNumber != null && !idNumber.isBlank()) {
            member = memberRepository.findByNationalId(idNumber).orElse(null);
        }
        // Find by phone number
        else if (phoneNumber != null && !phoneNumber.isBlank()) {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            member = memberRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        }
        // Find by member ID
        else if (memberId != null) {
            member = memberRepository.findById(memberId).orElse(null);
        }
        // Find by generic identifier (try all)
        else if (identifier != null && !identifier.isBlank()) {
            member = memberRepository.findByNationalId(identifier)
                    .or(() -> memberRepository.findByPhoneNumber(normalizePhoneNumber(identifier)))
                    .orElse(null);
        }

        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildMemberFinancialStatus(member));
    }

    /**
     * Get member financial status by ID
     */
    @GetMapping("/{memberId}/financial-status")
    @Operation(summary = "Get member financial status")
    public ResponseEntity<MemberFinancialStatus> getMemberFinancialStatus(@PathVariable UUID memberId) {
        log.info("Getting financial status for member: {}", memberId);

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildMemberFinancialStatus(member));
    }

    private MemberFinancialStatus buildMemberFinancialStatus(Member member) {
        BankingGroup group = member.getGroup();
        
        // Get current contribution cycle
        Optional<ContributionCycle> currentCycleOpt = cycleRepository
                .findLatestOpenCycleByGroupId(group.getId());
        
        BigDecimal outstandingContribution = BigDecimal.ZERO;
        UUID currentContributionId = null;
        
        if (currentCycleOpt.isPresent()) {
            ContributionCycle cycle = currentCycleOpt.get();
            Optional<Contribution> contributionOpt = contributionRepository
                    .findByMemberIdAndCycleId(member.getId(), cycle.getId());
            
            if (contributionOpt.isPresent()) {
                Contribution contribution = contributionOpt.get();
                if (contribution.getStatus() == ContributionStatus.PENDING || 
                    contribution.getStatus() == ContributionStatus.PARTIAL) {
                    outstandingContribution = contribution.getOutstandingAmount();
                    currentContributionId = contribution.getId();
                }
            }
        }

        // Get active loan
        BigDecimal outstandingLoanBalance = BigDecimal.ZERO;
        UUID activeLoanId = null;
        
        var activeLoans = loanRepository.findActiveLoansByMemberId(member.getId());
        if (!activeLoans.isEmpty()) {
            Loan activeLoan = activeLoans.get(0); // Take the first active loan
            outstandingLoanBalance = activeLoan.getOutstandingBalance();
            activeLoanId = activeLoan.getId();
        }

        return MemberFinancialStatus.builder()
                .memberId(member.getId())
                .memberName(member.getFullName())
                .idNumber(member.getNationalId())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .groupId(group.getId())
                .groupName(group.getName())
                .status(member.getStatus().name())
                .outstandingContribution(outstandingContribution)
                .currentContributionId(currentContributionId)
                .outstandingLoanBalance(outstandingLoanBalance)
                .activeLoanId(activeLoanId)
                .build();
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("254")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("+254")) {
            cleaned = "0" + cleaned.substring(4);
        }
        return cleaned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberFinancialStatus {
        private UUID memberId;
        private String memberName;
        private String idNumber;
        private String phoneNumber;
        private String email;
        private UUID groupId;
        private String groupName;
        private String status;
        private BigDecimal outstandingContribution;
        private UUID currentContributionId;
        private BigDecimal outstandingLoanBalance;
        private UUID activeLoanId;
    }
}
