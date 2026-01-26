package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.enums.MemberStatus;
import com.tablebanking.loanmanagement.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member management endpoints")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Create a new member")
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(
            @Valid @RequestBody CreateMemberRequest request) {
        MemberResponse member = memberService.createMember(request);
        return ResponseEntity.ok(ApiResponse.success("Member created", member));
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    @Operation(summary = "Update member details")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
            @PathVariable UUID memberId,
            @Valid @RequestBody CreateMemberRequest request) {
        MemberResponse member = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(ApiResponse.success("Member updated", member));
    }

    @PatchMapping("/{memberId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change member status")
    public ResponseEntity<ApiResponse<MemberResponse>> changeMemberStatus(
            @PathVariable UUID memberId,
            @RequestParam MemberStatus status) {
        MemberResponse member = memberService.changeMemberStatus(memberId, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", member));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberById(@PathVariable UUID memberId) {
        MemberResponse member = memberService.getMemberById(memberId);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping("/{memberId}/details")
    @Operation(summary = "Get member with full details including balance and activity")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> getMemberDetails(@PathVariable UUID memberId) {
        MemberDetailResponse member = memberService.getMemberDetails(memberId);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get all members in a group")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembersByGroup(@PathVariable UUID groupId) {
        List<MemberResponse> members = memberService.getMembersByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @GetMapping("/group/{groupId}/active")
    @Operation(summary = "Get active members in a group")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getActiveMembersByGroup(@PathVariable UUID groupId) {
        List<MemberResponse> members = memberService.getActiveMembersByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @GetMapping("/group/{groupId}/stats")
    @Operation(summary = "Get member statistics for a group")
    public ResponseEntity<ApiResponse<MemberStatsResponse>> getMemberStats(@PathVariable UUID groupId) {
        MemberStatsResponse stats = memberService.getMemberStats(groupId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/group/{groupId}/paginated")
    @Operation(summary = "Get members with pagination")
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> getMembersPaginated(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "memberNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<MemberResponse> members = memberService.getMembersByGroupPaginated(groupId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @GetMapping("/group/{groupId}/phone/{phoneNumber}")
    @Operation(summary = "Get member by phone number within a group")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberByPhone(
            @PathVariable UUID groupId,
            @PathVariable String phoneNumber) {
        MemberResponse member = memberService.getMemberByPhone(groupId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping("/{memberId}/balance/{financialYearId}")
    @Operation(summary = "Get member balance for a financial year")
    public ResponseEntity<ApiResponse<MemberBalanceResponse>> getMemberBalance(
            @PathVariable UUID memberId,
            @PathVariable UUID financialYearId) {
        MemberBalanceResponse balance = memberService.getMemberBalance(memberId, financialYearId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @GetMapping("/{memberId}/balance-history")
    @Operation(summary = "Get member balance history across all years")
    public ResponseEntity<ApiResponse<List<MemberBalanceResponse>>> getMemberBalanceHistory(
            @PathVariable UUID memberId) {
        List<MemberBalanceResponse> balances = memberService.getMemberBalanceHistory(memberId);
        return ResponseEntity.ok(ApiResponse.success(balances));
    }
}
