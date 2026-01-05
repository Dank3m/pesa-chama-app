package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.Member;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.entity.enums.NotificationChannel;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.MemberRepository;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.security.JwtService;
import com.tablebanking.loanmanagement.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsernameWithMember(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setLastLogin(Instant.now());
        user.resetFailedAttempts();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserResponse(user))
                .build();

        log.info("User logged in: {}", request.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account for an existing member")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("Member not found"));

        if (userRepository.findByMemberId(member.getId()).isPresent()) {
            throw new BusinessException("Member already has a user account");
        }

        UserRole role = UserRole.MEMBER;
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid role specified");
            }
        }

        User user = User.builder()
                .member(member)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isEnabled(true)
                .build();

        user = userRepository.save(user);

        log.info("User registered: {} for member: {}", user.getUsername(), member.getMemberNumber());

        return ResponseEntity.ok(ApiResponse.success("Registration successful", mapToUserResponse(user)));
    }

    // ==================== MEMBER SELF-REGISTRATION ENDPOINTS ====================

    /**
     * Validate registration token and get member info
     * Called when user opens registration link
     */
    @GetMapping("/register/validate")
    @Operation(summary = "Validate registration token and get member information")
    public ResponseEntity<ApiResponse<MemberRegistrationInfoResponse>> validateRegistrationToken(
            @RequestParam String memberId,
            @RequestParam String token) {

        log.info("Validating registration token for member: {}", memberId);

        UUID memberUuid = parseUuid(memberId);

        Member member = memberRepository.findById(memberUuid)
                .orElseThrow(() -> new BusinessException("Member not found"));

        // Validate token using Member entity
        if (!member.isRegistrationTokenValid(token)) {
            if (member.getRegistrationToken() == null) {
                throw new BusinessException("No registration token found for this member");
            }
            if (Boolean.TRUE.equals(member.getRegistrationTokenUsed())) {
                throw new BusinessException("Registration token has already been used");
            }
            if (!member.isRegistrationTokenValid()) {
                throw new BusinessException("Registration token has expired");
            }
            throw new BusinessException("Invalid registration token");
        }

        if (userRepository.findByMemberId(member.getId()).isPresent()) {
            throw new BusinessException("Member already has a registered account");
        }

        MemberRegistrationInfoResponse response = MemberRegistrationInfoResponse.builder()
                .memberId(member.getId().toString())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .groupId(member.getGroup().getId().toString())
                .groupName(member.getGroup().getName())
                .tokenValid(true)
                .tokenExpiry(member.getRegistrationTokenExpiry().toString())
                .suggestedRole(member.getIsAdmin() ? "ADMIN" : "MEMBER")
                .build();

        return ResponseEntity.ok(ApiResponse.success("Token validated", response));
    }

    /**
     * Complete member registration using token from link
     */
    @PostMapping("/register/member")
    @Operation(summary = "Register user account using registration token")
    public ResponseEntity<ApiResponse<AuthResponse>> registerWithToken(
            @Valid @RequestBody MemberRegistrationRequest request) {

        log.info("Processing member registration for memberId: {}", request.getMemberId());

        UUID memberUuid = parseUuid(request.getMemberId());

        Member member = memberRepository.findById(memberUuid)
                .orElseThrow(() -> new BusinessException("Member not found"));

        // Validate token using Member entity
        if (!member.isRegistrationTokenValid(request.getToken())) {
            if (Boolean.TRUE.equals(member.getRegistrationTokenUsed())) {
                throw new BusinessException("Registration token has already been used");
            }
            if (!member.isRegistrationTokenValid()) {
                throw new BusinessException("Registration token has expired");
            }
            throw new BusinessException("Invalid registration token");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        if (userRepository.findByMemberId(member.getId()).isPresent()) {
            throw new BusinessException("Member already has a user account");
        }

        UserRole role = UserRole.MEMBER;
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid role specified");
            }
        }

        User user = User.builder()
                .member(member)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isEnabled(true)
                .build();

        user = userRepository.save(user);

        // Mark token as used on Member entity
        member.markRegistrationTokenUsed();
        memberRepository.save(member);

        log.info("User registered via token: {} for member: {}", user.getUsername(), member.getMemberNumber());

        // Return auth response with tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserResponse(user))
                .build();

        return ResponseEntity.ok(ApiResponse.success("Account created successfully", authResponse));
    }

    /**
     * Resend registration notification to member
     */
    @PostMapping("/register/resend")
    @Operation(summary = "Resend registration notification to member")
    public ResponseEntity<ApiResponse<Void>> resendRegistrationNotification(
            @Valid @RequestBody ResendRegistrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Resending registration notification for member: {}", request.getMemberId());

        UUID memberUuid = parseUuid(request.getMemberId());

        NotificationChannel channel = NotificationChannel.BOTH;
        if (request.getChannel() != null) {
            try {
                channel = NotificationChannel.valueOf(request.getChannel().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid notification channel. Use EMAIL, SMS, or BOTH");
            }
        }

        memberService.resendRegistrationNotification(memberUuid, channel);

        return ResponseEntity.ok(ApiResponse.success("Registration notification sent", null));
    }

    // ==================== OTHER ENDPOINTS ====================

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate token")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("User logged out: {}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Invalid refresh token");
        }

        String refreshToken = authHeader.substring(7);
        String username = jwtService.extractUsername(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        User user = userRepository.findByUsernameWithMember(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        String newAccessToken = jwtService.generateToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserResponse(user))
                .build();

        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid ID format");
        }
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
}