package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.CreateFinancialYearRequest;
import com.tablebanking.loanmanagement.dto.request.RequestDTOs.CreateGroupWithSettingsRequest;
import com.tablebanking.loanmanagement.dto.request.RequestDTOs.PublicRegistrationRequest;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.InterestCalculationMethod;
import com.tablebanking.loanmanagement.entity.enums.InterestRatePeriod;
import com.tablebanking.loanmanagement.entity.enums.MemberStatus;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import com.tablebanking.loanmanagement.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegistrationService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final BankingGroupRepository groupRepository;
    private final GroupSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final FinancialYearService financialYearService;
    private final GroupSettingsService groupSettingsService;
    private final SubscriptionService subscriptionService;

    // Counter for generating member numbers
    private static final AtomicInteger memberCounter = new AtomicInteger(1000);

    /**
     * Public registration flow - creates user with optional group creation.
     */
    public RegistrationResponse registerPublic(PublicRegistrationRequest request) {
        // Validate username is not taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        BankingGroup group = null;
        GroupSettings settings = null;
        Member member = null;
        User user;

        if (Boolean.TRUE.equals(request.getCreateGroup())) {
            // Create group flow
            if (request.getGroupDetails() == null || request.getGroupDetails().getName() == null) {
                throw new BusinessException("Group details are required when creating a group");
            }

            if (groupRepository.existsByNameIgnoreCase(request.getGroupDetails().getName())) {
                throw new BusinessException("A group with this name already exists");
            }

            // Create the banking group
            group = createBankingGroup(request.getGroupDetails());
            group = groupRepository.save(group);

            // Create group settings
            settings = createGroupSettings(group, request.getGroupDetails());
            settings = settingsRepository.save(settings);

            // Create financial year for the group using custom start/end months
            createFinancialYearFromSettings(group.getId(), settings);

            // Create member (as admin)
            member = createMember(request, group, true);
            member = memberRepository.save(member);

            // Create user with ADMIN role
            user = createUser(request, member, UserRole.ADMIN);
            user = userRepository.save(user);

            // Update group with creator ID
            group.setCreatedBy(user.getId());
            groupRepository.save(group);

            // Assign FREE subscription plan to new group
            subscriptionService.createFreeSubscription(group);

            log.info("Public registration completed with group creation: user={}, group={}",
                    user.getUsername(), group.getName());
        } else {
            // User-only registration (without group) - typically shouldn't happen
            // but we handle it for flexibility
            throw new BusinessException("Group creation is required for public registration");
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return RegistrationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserResponse(user))
                .group(mapToGroupResponse(group))
                .groupSettings(groupSettingsService.mapToResponse(settings))
                .build();
    }

    /**
     * Create financial year using custom start/end months from group settings.
     */
    private void createFinancialYearFromSettings(java.util.UUID groupId, GroupSettings settings) {
        int startMonth = settings.getFinancialYearStartMonth();
        int endMonth = settings.getFinancialYearEndMonth();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Determine the start year based on current date and start month
        int startYear;
        int endYear;

        if (startMonth <= endMonth) {
            // Financial year within same calendar year (e.g., Jan-Dec or Apr-Mar where Apr is 4 and Mar is 3)
            // This case is for when start month > end month numerically (e.g., start=4 Apr, end=3 Mar)
            // Actually if startMonth <= endMonth, it means the year doesn't cross calendar years
            // e.g., startMonth=1 (Jan) endMonth=12 (Dec) - same year
            // e.g., startMonth=4 (Apr) endMonth=3 (Mar) - this would be startMonth > endMonth
            if (currentMonth >= startMonth) {
                startYear = currentYear;
                endYear = currentYear;
            } else {
                startYear = currentYear - 1;
                endYear = currentYear - 1;
            }
        } else {
            // Financial year crosses calendar years (e.g., Dec-Nov where Dec is 12 and Nov is 11)
            if (currentMonth >= startMonth) {
                startYear = currentYear;
                endYear = currentYear + 1;
            } else if (currentMonth <= endMonth) {
                startYear = currentYear - 1;
                endYear = currentYear;
            } else {
                // Between end of previous FY and start of new FY
                startYear = currentYear;
                endYear = currentYear + 1;
            }
        }

        LocalDate startDate = LocalDate.of(startYear, Month.of(startMonth), 1);
        LocalDate endDate = LocalDate.of(endYear, Month.of(endMonth), 1)
                .with(TemporalAdjusters.lastDayOfMonth());

        String yearName = startYear + "/" + endYear;
        if (startYear == endYear) {
            yearName = String.valueOf(startYear);
        }

        CreateFinancialYearRequest request = CreateFinancialYearRequest.builder()
                .groupId(groupId)
                .startDate(startDate)
                .endDate(endDate)
                .yearName(yearName)
                .build();

        financialYearService.createFinancialYear(request);

        log.info("Created financial year {} ({} to {}) for group {}",
                yearName, startDate, endDate, groupId);
    }

    private BankingGroup createBankingGroup(CreateGroupWithSettingsRequest request) {
        return BankingGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contributionAmount(request.getDefaultContributionAmount() != null ?
                        request.getDefaultContributionAmount() : new BigDecimal("3500.00"))
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .interestRate(request.getInterestRate() != null ?
                        request.getInterestRate() : new BigDecimal("0.10"))
                .financialYearStartMonth(request.getFinancialYearStartMonth() != null ?
                        request.getFinancialYearStartMonth() : 12)
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 50)
                .isActive(true)
                .build();
    }

    private GroupSettings createGroupSettings(BankingGroup group, CreateGroupWithSettingsRequest request) {
        GroupSettings.GroupSettingsBuilder builder = GroupSettings.builder()
                .group(group)
                .financialYearStartMonth(request.getFinancialYearStartMonth() != null ?
                        request.getFinancialYearStartMonth() : 12)
                .financialYearEndMonth(request.getFinancialYearEndMonth() != null ?
                        request.getFinancialYearEndMonth() : 11)
                .defaultContributionAmount(request.getDefaultContributionAmount() != null ?
                        request.getDefaultContributionAmount() : new BigDecimal("3500.00"))
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .allowPartialContributions(request.getAllowPartialContributions() != null ?
                        request.getAllowPartialContributions() : true)
                .interestRate(request.getInterestRate() != null ?
                        request.getInterestRate() : new BigDecimal("0.10"))
                .maxLoanDurationMonths(request.getMaxLoanDurationMonths() != null ?
                        request.getMaxLoanDurationMonths() : 12)
                .gracePeriodDays(request.getGracePeriodDays() != null ?
                        request.getGracePeriodDays() : 0)
                .maxLoanMultiplier(request.getMaxLoanMultiplier() != null ?
                        request.getMaxLoanMultiplier() : new BigDecimal("3.00"))
                .requireGuarantors(request.getRequireGuarantors() != null ?
                        request.getRequireGuarantors() : false)
                .minGuarantors(request.getMinGuarantors() != null ?
                        request.getMinGuarantors() : 1)
                .latePenaltyRate(request.getLatePenaltyRate() != null ?
                        request.getLatePenaltyRate() : new BigDecimal("0.05"))
                .enablePenalties(request.getEnablePenalties() != null ?
                        request.getEnablePenalties() : true);

        // Set interest rate period
        if (request.getInterestRatePeriod() != null) {
            try {
                builder.interestRatePeriod(
                        InterestRatePeriod.valueOf(request.getInterestRatePeriod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                builder.interestRatePeriod(InterestRatePeriod.MONTHLY);
            }
        }

        // Set interest calculation method
        if (request.getInterestCalculationMethod() != null) {
            try {
                builder.interestCalculationMethod(
                        InterestCalculationMethod.valueOf(request.getInterestCalculationMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                builder.interestCalculationMethod(InterestCalculationMethod.SIMPLE);
            }
        }

        return builder.build();
    }

    private Member createMember(PublicRegistrationRequest request, BankingGroup group, boolean isAdmin) {
        String memberNumber = generateMemberNumber(group);

        return Member.builder()
                .group(group)
                .memberNumber(memberNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .joinDate(LocalDate.now())
                .status(MemberStatus.ACTIVE)
                .isAdmin(isAdmin)
                .build();
    }

    private User createUser(PublicRegistrationRequest request, Member member, UserRole role) {
        return User.builder()
                .member(member)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isEnabled(true)
                .build();
    }

    private String generateMemberNumber(BankingGroup group) {
        String prefix = group.getName().substring(0, Math.min(3, group.getName().length())).toUpperCase();
        int number = memberCounter.incrementAndGet();
        return String.format("%s-%04d", prefix, number);
    }

    private UserResponse mapToUserResponse(User user) {
        MemberResponse memberResponse = null;
        if (user.getMember() != null) {
            Member member = user.getMember();
            memberResponse = MemberResponse.builder()
                    .id(member.getId())
                    .groupId(member.getGroup().getId())
                    .memberNumber(member.getMemberNumber())
                    .firstName(member.getFirstName())
                    .lastName(member.getLastName())
                    .fullName(member.getFullName())
                    .email(member.getEmail())
                    .phoneNumber(member.getPhoneNumber())
                    .status(member.getStatus())
                    .isAdmin(member.getIsAdmin())
                    .build();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .member(memberResponse)
                .build();
    }

    private GroupResponse mapToGroupResponse(BankingGroup group) {
        int memberCount = groupRepository.countActiveMembers(group.getId());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contributionAmount(group.getContributionAmount())
                .currency(group.getCurrency())
                .interestRate(group.getInterestRate())
                .financialYearStartMonth(group.getFinancialYearStartMonth())
                .maxMembers(group.getMaxMembers())
                .currentMemberCount(memberCount)
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
