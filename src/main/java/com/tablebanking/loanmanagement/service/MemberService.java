package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.event.MemberRegistrationEvent;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final BankingGroupRepository groupRepository;
    private final FinancialYearRepository financialYearRepository;
    private final MemberBalanceRepository balanceRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.kafka.topics.member-registration:member-registration-events}")
    private String memberRegistrationTopic;

    @Value("${app.registration.token-expiry-days:7}")
    private int tokenExpiryDays;

    /**
     * Create a new member.
     */
    @CacheEvict(value = "groupMembers", key = "#request.groupId")
    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {

        // Validate group exists
        BankingGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("Banking group not found"));

        // Validate group capacity
        int currentMembers = groupRepository.countActiveMembers(group.getId());
        if (group.getMaxMembers() != null && currentMembers >= group.getMaxMembers()) {
            throw new BusinessException("Group has reached maximum member capacity");
        }

        // Validate unique phone number
        if (memberRepository.existsByGroupIdAndPhoneNumber(group.getId(), request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered in this group");
        }

        // Validate unique email if provided
        if (request.getEmail() != null &&
                memberRepository.existsByGroupIdAndEmail(group.getId(), request.getEmail())) {
            throw new BusinessException("Email already registered in this group");
        }

        // Generate member number
        String memberNumber = generateMemberNumber(group.getId());

        // Determine notification channel
        NotificationChannel channel = determineNotificationChannel(request);

        Member member = Member.builder()
                .group(group)
                .memberNumber(memberNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .joinDate(LocalDate.now())
                .status(MemberStatus.ACTIVE)
                .isAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false)
                .registrationNotificationChannel(channel)
                .build();

        // Generate registration token
        member.generateRegistrationToken(tokenExpiryDays);

        member = memberRepository.save(member);

        // Initialize member balance for current financial year if exists
        initializeMemberBalance(member);

        log.info("Created member: {} ({}) in group: {}",
                member.getFullName(), member.getMemberNumber(), group.getName());

        // Send registration notification
        sendRegistrationNotification(member, group);

        return mapToMemberResponse(member);
    }

    /**
     * Send registration notification via Kafka
     */
    private void sendRegistrationNotification(Member member, BankingGroup group) {
        try {
            // Create and publish event
            MemberRegistrationEvent event = MemberRegistrationEvent.create(
                    member.getId().toString(),
                    member.getMemberNumber(),
                    member.getFirstName(),
                    member.getLastName(),
                    member.getEmail(),
                    member.getPhoneNumber(),
                    group.getId().toString(),
                    group.getName(),
                    member.getRegistrationToken(),
                    baseUrl,
                    member.getRegistrationNotificationChannel()
            );

            // Publish to Kafka
            kafkaTemplate.send(memberRegistrationTopic, member.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish member registration event for member: {}",
                                    member.getId(), ex);
                        } else {
                            log.info("Member registration event published for member: {} to topic: {}",
                                    member.getId(), memberRegistrationTopic);
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending registration notification for member: {}", member.getId(), e);
            // Don't fail the member creation if notification fails
        }
    }

    /**
     * Resend registration notification for existing member
     */
    @Transactional
    public void resendRegistrationNotification(UUID memberId, NotificationChannel channel) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        // Check if member already has a user account
        if (userRepository.findByMemberId(memberId).isPresent()) {
            throw new BusinessException("Member already has a registered account");
        }

        // Generate new token (invalidates old one)
        member.generateRegistrationToken(tokenExpiryDays);
        member.setRegistrationNotificationChannel(channel);
        memberRepository.save(member);

        // Send notification
        sendRegistrationNotification(member, member.getGroup());
        log.info("Registration notification resent for member: {}", memberId);
    }

    /**
     * Determine notification channel based on available contact info
     */
    private NotificationChannel determineNotificationChannel(CreateMemberRequest request) {
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank();

        if (hasEmail && hasPhone) {
            return NotificationChannel.BOTH;
        } else if (hasEmail) {
            return NotificationChannel.EMAIL;
        } else if (hasPhone) {
            return NotificationChannel.SMS;
        }
        return NotificationChannel.EMAIL; // Default
    }

    /**
     * Update member details.
     */
    @CacheEvict(value = {"member", "groupMembers"}, allEntries = true)
    public MemberResponse updateMember(UUID memberId, CreateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(member.getEmail()) &&
                    memberRepository.existsByGroupIdAndEmail(member.getGroup().getId(), request.getEmail())) {
                throw new BusinessException("Email already registered in this group");
            }
            member.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            if (!request.getPhoneNumber().equals(member.getPhoneNumber()) &&
                    memberRepository.existsByGroupIdAndPhoneNumber(member.getGroup().getId(), request.getPhoneNumber())) {
                throw new BusinessException("Phone number already registered in this group");
            }
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getNationalId() != null) {
            member.setNationalId(request.getNationalId());
        }
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }
        if (request.getIsAdmin() != null) {
            member.setIsAdmin(request.getIsAdmin());
        }

        member = memberRepository.save(member);
        log.info("Updated member: {} ({})", member.getFullName(), member.getMemberNumber());
        return mapToMemberResponse(member);
    }

    /**
     * Change member status.
     */
    @CacheEvict(value = {"member", "groupMembers"}, allEntries = true)
    public MemberResponse changeMemberStatus(UUID memberId, MemberStatus newStatus) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        if (newStatus == MemberStatus.LEFT || newStatus == MemberStatus.INACTIVE) {
            long activeLoans = loanRepository.countActiveLoansByMember(memberId);
            if (activeLoans > 0) {
                throw new BusinessException("Cannot deactivate member with active loans");
            }
        }

        MemberStatus oldStatus = member.getStatus();
        member.setStatus(newStatus);
        member = memberRepository.save(member);

        log.info("Changed member {} status from {} to {}",
                member.getMemberNumber(), oldStatus, newStatus);

        return mapToMemberResponse(member);
    }

    /**
     * Get member by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "member", key = "#memberId")
    public MemberResponse getMemberById(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));
        return mapToMemberResponse(member);
    }

    /**
     * Get member entity by ID (for internal use).
     */
    @Transactional(readOnly = true)
    public Member getMemberEntityById(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));
    }

    /**
     * Get member details with balance and recent activity.
     */
    @Transactional(readOnly = true)
    public MemberDetailResponse getMemberDetails(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        FinancialYear currentYear = financialYearRepository
                .findCurrentByGroupId(member.getGroup().getId())
                .orElse(null);

        MemberBalanceResponse balanceResponse = null;
        if (currentYear != null) {
            MemberBalance balance = balanceRepository
                    .findByMemberIdAndFinancialYearId(memberId, currentYear.getId())
                    .orElse(null);
            if (balance != null) {
                balanceResponse = mapToBalanceResponse(balance);
            }
        }

        List<ContributionResponse> recentContributions = contributionRepository
                .findByMemberId(memberId)
                .stream()
                .limit(6)
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList());

        List<LoanSummaryResponse> activeLoans = loanRepository
                .findActiveLoansByMemberId(memberId)
                .stream()
                .map(this::mapToLoanSummary)
                .collect(Collectors.toList());

        return MemberDetailResponse.builder()
                .member(mapToMemberResponse(member))
                .currentBalance(balanceResponse)
                .recentContributions(recentContributions)
                .activeLoans(activeLoans)
                .build();
    }

    /**
     * Get all members in a group.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "groupMembers", key = "#groupId")
    public List<MemberResponse> getMembersByGroup(UUID groupId) {
        return memberRepository.findByGroupId(groupId).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active members in a group.
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> getActiveMembersByGroup(UUID groupId) {
        return memberRepository.findActiveMembers(groupId).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get members with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getMembersByGroupPaginated(UUID groupId, Pageable pageable) {
        Page<Member> page = memberRepository.findByGroupId(groupId, pageable);

        List<MemberResponse> content = page.getContent().stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());

        return PageResponse.<MemberResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Get member by phone number.
     */
    @Transactional(readOnly = true)
    public MemberResponse getMemberByPhone(String phoneNumber) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException("Member not found with phone: " + phoneNumber));
        return mapToMemberResponse(member);
    }

    /**
     * Get member balance for a specific financial year.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "memberBalance", key = "#memberId + '_' + #financialYearId")
    public MemberBalanceResponse getMemberBalance(UUID memberId, UUID financialYearId) {
        MemberBalance balance = balanceRepository
                .findByMemberIdAndFinancialYearId(memberId, financialYearId)
                .orElseThrow(() -> new BusinessException("Balance not found for this financial year"));
        return mapToBalanceResponse(balance);
    }

    /**
     * Get all balances for a member across financial years.
     */
    @Transactional(readOnly = true)
    public List<MemberBalanceResponse> getMemberBalanceHistory(UUID memberId) {
        return balanceRepository.findByMemberId(memberId).stream()
                .map(this::mapToBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get member statistics for a group.
     */
    @Transactional(readOnly = true)
    public MemberStatsResponse getMemberStats(UUID groupId) {
        long total = memberRepository.findByGroupId(groupId).size();
        long active = memberRepository.countByGroupIdAndStatus(groupId, MemberStatus.ACTIVE);
        long inactive = memberRepository.countByGroupIdAndStatus(groupId, MemberStatus.INACTIVE);
        long suspended = memberRepository.countByGroupIdAndStatus(groupId, MemberStatus.SUSPENDED);
        long left = memberRepository.countByGroupIdAndStatus(groupId, MemberStatus.LEFT);
        long admins = memberRepository.findByGroupId(groupId).stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsAdmin()))
                .count();

        return MemberStatsResponse.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .suspended(suspended)
                .left(left)
                .admins(admins)
                .build();
    }

    // Private helper methods

    private String generateMemberNumber(UUID groupId) {
        int maxNumber = memberRepository.getMaxMemberNumber(groupId);
        return String.format("PESA-%03d", maxNumber + 1);
    }

    private void initializeMemberBalance(Member member) {
        financialYearRepository.findCurrentByGroupId(member.getGroup().getId())
                .ifPresent(year -> {
                    if (!balanceRepository.findByMemberIdAndFinancialYearId(member.getId(), year.getId()).isPresent()) {
                        MemberBalance balance = MemberBalance.builder()
                                .member(member)
                                .financialYear(year)
                                .build();
                        balanceRepository.save(balance);
                    }
                });
    }

    private MemberResponse mapToMemberResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .groupId(member.getGroup().getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .nationalId(member.getNationalId())
                .dateOfBirth(member.getDateOfBirth())
                .address(member.getAddress())
                .joinDate(member.getJoinDate())
                .status(member.getStatus())
                .isAdmin(member.getIsAdmin())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private MemberBalanceResponse mapToBalanceResponse(MemberBalance balance) {
        return MemberBalanceResponse.builder()
                .id(balance.getId())
                .memberId(balance.getMember().getId())
                .financialYearId(balance.getFinancialYear().getId())
                .yearName(balance.getFinancialYear().getYearName())
                .totalContributions(balance.getTotalContributions())
                .totalLoansTaken(balance.getTotalLoansTaken())
                .totalLoanRepayments(balance.getTotalLoanRepayments())
                .outstandingLoanBalance(balance.getOutstandingLoanBalance())
                .shareValue(balance.getShareValue())
                .lastCalculatedAt(balance.getLastCalculatedAt())
                .build();
    }

    private ContributionResponse mapToContributionResponse(Contribution contribution) {
        return ContributionResponse.builder()
                .id(contribution.getId())
                .memberId(contribution.getMember().getId())
                .memberName(contribution.getMember().getFullName())
                .cycleId(contribution.getCycle().getId())
                .cycleMonth(contribution.getCycle().getCycleMonth())
                .expectedAmount(contribution.getExpectedAmount())
                .paidAmount(contribution.getPaidAmount())
                .outstandingAmount(contribution.getOutstandingAmount())
                .status(contribution.getStatus())
                .paymentDate(contribution.getPaymentDate())
                .convertedToLoan(contribution.getConvertedToLoan())
                .notes(contribution.getNotes())
                .build();
    }

    private LoanSummaryResponse mapToLoanSummary(Loan loan) {
        return LoanSummaryResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .loanType(loan.getLoanType())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus())
                .disbursementDate(loan.getDisbursementDate())
                .build();
    }
}