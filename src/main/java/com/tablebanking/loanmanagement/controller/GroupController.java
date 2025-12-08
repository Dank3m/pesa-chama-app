package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.BankingGroup;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.BankingGroupRepository;
import com.tablebanking.loanmanagement.service.FinancialYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Banking Groups", description = "Group management endpoints")
public class GroupController {

    private final BankingGroupRepository groupRepository;
    private final FinancialYearService financialYearService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new banking group")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        
        if (groupRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Group with this name already exists");
        }

        BankingGroup group = BankingGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contributionAmount(request.getContributionAmount() != null ? 
                        request.getContributionAmount() : new BigDecimal("3500.00"))
                .currency(request.getCurrency() != null ? request.getCurrency() : "KES")
                .interestRate(request.getInterestRate() != null ? 
                        request.getInterestRate() : new BigDecimal("0.10"))
                .financialYearStartMonth(request.getFinancialYearStartMonth() != null ? 
                        request.getFinancialYearStartMonth() : 12)
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 50)
                .isActive(true)
                .build();

        group = groupRepository.save(group);

        // Create default financial year
        financialYearService.createDefaultFinancialYear(group.getId());

        return ResponseEntity.ok(ApiResponse.success("Group created", mapToResponse(group)));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(@PathVariable UUID groupId) {
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(group)));
    }

    @GetMapping
    @Operation(summary = "Get all active groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups() {
        List<GroupResponse> groups = groupRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update group settings")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupRequest request) {
        
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getContributionAmount() != null) {
            group.setContributionAmount(request.getContributionAmount());
        }
        if (request.getInterestRate() != null) {
            group.setInterestRate(request.getInterestRate());
        }
        if (request.getMaxMembers() != null) {
            group.setMaxMembers(request.getMaxMembers());
        }

        group = groupRepository.save(group);

        return ResponseEntity.ok(ApiResponse.success("Group updated", mapToResponse(group)));
    }

    @PatchMapping("/{groupId}/contribution-amount")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update monthly contribution amount")
    public ResponseEntity<ApiResponse<GroupResponse>> updateContributionAmount(
            @PathVariable UUID groupId,
            @RequestParam BigDecimal newAmount) {
        
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Contribution amount must be positive");
        }

        group.setContributionAmount(newAmount);
        group = groupRepository.save(group);

        return ResponseEntity.ok(ApiResponse.success("Contribution amount updated", mapToResponse(group)));
    }

    @PatchMapping("/{groupId}/interest-rate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update loan interest rate")
    public ResponseEntity<ApiResponse<GroupResponse>> updateInterestRate(
            @PathVariable UUID groupId,
            @RequestParam BigDecimal newRate) {
        
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        if (newRate.compareTo(BigDecimal.ZERO) < 0 || newRate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("Interest rate must be between 0 and 1 (0% to 100%)");
        }

        group.setInterestRate(newRate);
        group = groupRepository.save(group);

        return ResponseEntity.ok(ApiResponse.success("Interest rate updated", mapToResponse(group)));
    }

    private GroupResponse mapToResponse(BankingGroup group) {
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
