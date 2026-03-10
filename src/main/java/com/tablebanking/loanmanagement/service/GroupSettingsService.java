package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.UpdateGroupSettingsRequest;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.GroupSettingsResponse;
import com.tablebanking.loanmanagement.entity.BankingGroup;
import com.tablebanking.loanmanagement.entity.GroupSettings;
import com.tablebanking.loanmanagement.entity.enums.InterestCalculationMethod;
import com.tablebanking.loanmanagement.entity.enums.InterestRatePeriod;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.BankingGroupRepository;
import com.tablebanking.loanmanagement.repository.GroupSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupSettingsService {

    private final GroupSettingsRepository settingsRepository;
    private final BankingGroupRepository groupRepository;

    /**
     * Get settings for a group.
     */
    @Transactional(readOnly = true)
    public GroupSettingsResponse getSettings(UUID groupId) {
        GroupSettings settings = settingsRepository.findByGroupIdWithGroup(groupId)
                .orElseThrow(() -> new BusinessException("Group settings not found"));
        return mapToResponse(settings);
    }

    /**
     * Create default settings for a group.
     */
    public GroupSettings createDefaultSettings(BankingGroup group) {
        if (settingsRepository.existsByGroupId(group.getId())) {
            throw new BusinessException("Settings already exist for this group");
        }

        GroupSettings settings = GroupSettings.createDefaultForGroup(group);
        settings = settingsRepository.save(settings);

        log.info("Created default settings for group: {}", group.getName());
        return settings;
    }

    /**
     * Update settings for a group.
     */
    public GroupSettingsResponse updateSettings(UUID groupId, UpdateGroupSettingsRequest request) {
        GroupSettings settings = settingsRepository.findByGroupIdWithGroup(groupId)
                .orElseThrow(() -> new BusinessException("Group settings not found"));

        // Update Financial Year Settings
        if (request.getFinancialYearStartMonth() != null) {
            settings.setFinancialYearStartMonth(request.getFinancialYearStartMonth());
        }
        if (request.getFinancialYearEndMonth() != null) {
            settings.setFinancialYearEndMonth(request.getFinancialYearEndMonth());
        }

        // Update Contribution Settings
        if (request.getDefaultContributionAmount() != null) {
            settings.setDefaultContributionAmount(request.getDefaultContributionAmount());
            // Also update the group's contribution amount for consistency
            settings.getGroup().setContributionAmount(request.getDefaultContributionAmount());
        }
        if (request.getCurrency() != null) {
            settings.setCurrency(request.getCurrency());
            settings.getGroup().setCurrency(request.getCurrency());
        }
        if (request.getAllowPartialContributions() != null) {
            settings.setAllowPartialContributions(request.getAllowPartialContributions());
        }

        // Update Loan Settings
        if (request.getInterestRate() != null) {
            settings.setInterestRate(request.getInterestRate());
            settings.getGroup().setInterestRate(request.getInterestRate());
        }
        if (request.getInterestRatePeriod() != null) {
            try {
                settings.setInterestRatePeriod(
                        InterestRatePeriod.valueOf(request.getInterestRatePeriod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid interest rate period: " + request.getInterestRatePeriod());
            }
        }
        if (request.getInterestCalculationMethod() != null) {
            try {
                settings.setInterestCalculationMethod(
                        InterestCalculationMethod.valueOf(request.getInterestCalculationMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid interest calculation method: " + request.getInterestCalculationMethod());
            }
        }
        if (request.getMaxLoanDurationMonths() != null) {
            settings.setMaxLoanDurationMonths(request.getMaxLoanDurationMonths());
        }
        if (request.getGracePeriodDays() != null) {
            settings.setGracePeriodDays(request.getGracePeriodDays());
        }
        if (request.getMaxLoanMultiplier() != null) {
            settings.setMaxLoanMultiplier(request.getMaxLoanMultiplier());
        }
        if (request.getRequireGuarantors() != null) {
            settings.setRequireGuarantors(request.getRequireGuarantors());
        }
        if (request.getMinGuarantors() != null) {
            settings.setMinGuarantors(request.getMinGuarantors());
        }

        // Update Scheduler Settings
        if (request.getContributionCheckCron() != null) {
            settings.setContributionCheckCron(request.getContributionCheckCron());
        }
        if (request.getInterestAccrualCron() != null) {
            settings.setInterestAccrualCron(request.getInterestAccrualCron());
        }
        if (request.getOverdueCheckCron() != null) {
            settings.setOverdueCheckCron(request.getOverdueCheckCron());
        }
        if (request.getReminderDaysBeforeDue() != null) {
            settings.setReminderDaysBeforeDue(request.getReminderDaysBeforeDue());
        }

        // Update Penalty Settings
        if (request.getLatePenaltyRate() != null) {
            settings.setLatePenaltyRate(request.getLatePenaltyRate());
        }
        if (request.getEnablePenalties() != null) {
            settings.setEnablePenalties(request.getEnablePenalties());
        }

        settings = settingsRepository.save(settings);
        groupRepository.save(settings.getGroup());

        log.info("Updated settings for group: {}", settings.getGroup().getName());
        return mapToResponse(settings);
    }

    /**
     * Ensure settings exist for a group, creating defaults if needed.
     */
    public GroupSettings ensureSettingsExist(BankingGroup group) {
        return settingsRepository.findByGroupId(group.getId())
                .orElseGet(() -> createDefaultSettings(group));
    }

    /**
     * Map entity to response DTO.
     */
    public GroupSettingsResponse mapToResponse(GroupSettings settings) {
        return GroupSettingsResponse.builder()
                .id(settings.getId())
                .groupId(settings.getGroup().getId())
                .groupName(settings.getGroup().getName())
                .financialYearStartMonth(settings.getFinancialYearStartMonth())
                .financialYearEndMonth(settings.getFinancialYearEndMonth())
                .defaultContributionAmount(settings.getDefaultContributionAmount())
                .currency(settings.getCurrency())
                .allowPartialContributions(settings.getAllowPartialContributions())
                .interestRate(settings.getInterestRate())
                .interestRatePeriod(settings.getInterestRatePeriod().name())
                .interestCalculationMethod(settings.getInterestCalculationMethod().name())
                .maxLoanDurationMonths(settings.getMaxLoanDurationMonths())
                .gracePeriodDays(settings.getGracePeriodDays())
                .maxLoanMultiplier(settings.getMaxLoanMultiplier())
                .requireGuarantors(settings.getRequireGuarantors())
                .minGuarantors(settings.getMinGuarantors())
                .contributionCheckCron(settings.getContributionCheckCron())
                .interestAccrualCron(settings.getInterestAccrualCron())
                .overdueCheckCron(settings.getOverdueCheckCron())
                .reminderDaysBeforeDue(settings.getReminderDaysBeforeDue())
                .latePenaltyRate(settings.getLatePenaltyRate())
                .enablePenalties(settings.getEnablePenalties())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
