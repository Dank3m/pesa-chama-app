package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.StkPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stk-push")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "STK Push", description = "M-Pesa STK Push payment endpoints")
public class StkPushController {

    private final StkPushService stkPushService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate an M-Pesa STK Push payment")
    public ResponseEntity<ResponseDTOs.ApiResponse<ResponseDTOs.StkPushResponse>> initiateStkPush(
            @Valid @RequestBody RequestDTOs.StkPushRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        ResponseDTOs.StkPushResponse response = stkPushService.initiateStkPush(request, userId);
        return ResponseEntity.ok(ResponseDTOs.ApiResponse.success("STK Push initiated", response));
    }

    @GetMapping("/status/{collectionRef}")
    @Operation(summary = "Get the status of an STK Push payment")
    public ResponseEntity<ResponseDTOs.ApiResponse<ResponseDTOs.StkPushStatusResponse>> getStatus(
            @PathVariable String collectionRef) {
        ResponseDTOs.StkPushStatusResponse response = stkPushService.getStatus(collectionRef);
        return ResponseEntity.ok(ResponseDTOs.ApiResponse.success(response));
    }

    private UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        return user.getId();
    }
}
