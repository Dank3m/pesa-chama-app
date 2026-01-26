package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.Member;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.entity.UserSettings;
import com.tablebanking.loanmanagement.entity.enums.NotificationChannel;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.MemberRepository;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.repository.UserSettingsRepository;
import com.tablebanking.loanmanagement.security.JwtService;
import com.tablebanking.loanmanagement.service.MemberService;
import com.tablebanking.loanmanagement.service.RegistrationService;
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
import java.util.ArrayList;
import java.util.List;
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
    private final UserSettingsRepository userSettingsRepository;
    private final MemberService memberService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final RegistrationService registrationService;

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

        // Check for multi-group membership by email
        List<GroupMembershipResponse> availableGroups = new ArrayList<>();
        UUID defaultGroupId = null;
        boolean hasMultipleGroups = false;

        Member primaryMember = user.getMember();
        if (primaryMember != null && primaryMember.getEmail() != null) {
            List<Member> membersWithSameEmail = memberRepository.findAllByEmailWithGroup(primaryMember.getEmail());

            if (membersWithSameEmail.size() > 1) {
                hasMultipleGroups = true;

                // Get user's default group setting
                UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
                defaultGroupId = settings != null ? settings.getDefaultGroupId() : null;

                // If no default set, use the primary member's group
                if (defaultGroupId == null) {
                    defaultGroupId = primaryMember.getGroup().getId();
                }

                // Build available groups list
                for (Member m : membersWithSameEmail) {
                    availableGroups.add(GroupMembershipResponse.builder()
                            .groupId(m.getGroup().getId())
                            .groupName(m.getGroup().getName())
                            .memberId(m.getId())
                            .memberNumber(m.getMemberNumber())
                            .role(m.getIsAdmin() ? "ADMIN" : "MEMBER")
                            .isDefault(m.getGroup().getId().equals(defaultGroupId))
                            .build());
                }

                log.info("User {} has access to {} groups via email {}",
                        user.getUsername(), membersWithSameEmail.size(), primaryMember.getEmail());
            }
        }

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .user(mapToUserResponse(user))
                .availableGroups(availableGroups.isEmpty() ? null : availableGroups)
                .defaultGroupId(defaultGroupId)
                .hasMultipleGroups(hasMultipleGroups)
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

    // ==================== PUBLIC REGISTRATION ENDPOINT ====================

    @PostMapping("/register/public")
    @Operation(summary = "Public registration - create user and optionally a new banking group")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerPublic(
            @Valid @RequestBody PublicRegistrationRequest request) {

        log.info("Public registration attempt for username: {}", request.getUsername());

        RegistrationResponse response = registrationService.registerPublic(request);

        log.info("Public registration successful: user={}, group={}",
                request.getUsername(),
                response.getGroup() != null ? response.getGroup().getName() : "none");

        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
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

    /**
     * Get current authenticated user's profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }

        User user = userRepository.findByUsernameWithMember(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        log.debug("Fetching profile for user: {}", userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", mapToUserResponse(user)));
    }

    /**
     * Get all groups the user has access to via email
     */
    @GetMapping("/groups")
    @Operation(summary = "Get all groups user has access to")
    public ResponseEntity<ApiResponse<List<GroupMembershipResponse>>> getAvailableGroups(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }

        User user = userRepository.findByUsernameWithMember(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        Member primaryMember = user.getMember();
        if (primaryMember == null || primaryMember.getEmail() == null) {
            return ResponseEntity.ok(ApiResponse.success("No groups available", new ArrayList<>()));
        }

        List<Member> membersWithSameEmail = memberRepository.findAllByEmailWithGroup(primaryMember.getEmail());

        // Get user's default group setting
        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        UUID defaultGroupId = settings != null ? settings.getDefaultGroupId() : primaryMember.getGroup().getId();

        List<GroupMembershipResponse> groups = new ArrayList<>();
        for (Member m : membersWithSameEmail) {
            groups.add(GroupMembershipResponse.builder()
                    .groupId(m.getGroup().getId())
                    .groupName(m.getGroup().getName())
                    .memberId(m.getId())
                    .memberNumber(m.getMemberNumber())
                    .role(m.getIsAdmin() ? "ADMIN" : "MEMBER")
                    .isDefault(m.getGroup().getId().equals(defaultGroupId))
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success("Available groups retrieved", groups));
    }

    /**
     * Set the default group for user
     */
    @PutMapping("/default-group/{groupId}")
    @Operation(summary = "Set default group for user")
    public ResponseEntity<ApiResponse<Void>> setDefaultGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }

        User user = userRepository.findByUsernameWithMember(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Verify user has access to this group via email
        Member primaryMember = user.getMember();
        if (primaryMember == null || primaryMember.getEmail() == null) {
            throw new BusinessException("User has no email associated");
        }

        List<Member> membersWithSameEmail = memberRepository.findAllByEmailWithGroup(primaryMember.getEmail());
        boolean hasAccessToGroup = membersWithSameEmail.stream()
                .anyMatch(m -> m.getGroup().getId().equals(groupId));

        if (!hasAccessToGroup) {
            throw new BusinessException("User does not have access to this group");
        }

        // Update or create user settings with default group
        UserSettings settings = userSettingsRepository.findByUserId(user.getId())
                .orElse(UserSettings.builder().user(user).build());

        settings.setDefaultGroupId(groupId);
        userSettingsRepository.save(settings);

        log.info("User {} set default group to {}", user.getUsername(), groupId);

        return ResponseEntity.ok(ApiResponse.success("Default group updated", null));
    }

    /**
     * Switch to a different group (returns member info for that group)
     */
    @GetMapping("/switch-group/{groupId}")
    @Operation(summary = "Get member info for a specific group")
    public ResponseEntity<ApiResponse<MemberResponse>> switchGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }

        User user = userRepository.findByUsernameWithMember(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Find member in the specified group by email
        Member primaryMember = user.getMember();
        if (primaryMember == null || primaryMember.getEmail() == null) {
            throw new BusinessException("User has no email associated");
        }

        List<Member> membersWithSameEmail = memberRepository.findAllByEmailWithGroup(primaryMember.getEmail());
        Member targetMember = membersWithSameEmail.stream()
                .filter(m -> m.getGroup().getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("User does not have access to this group"));

        MemberResponse memberResponse = MemberResponse.builder()
                .id(targetMember.getId())
                .groupId(targetMember.getGroup().getId())
                .memberNumber(targetMember.getMemberNumber())
                .firstName(targetMember.getFirstName())
                .lastName(targetMember.getLastName())
                .fullName(targetMember.getFullName())
                .email(targetMember.getEmail())
                .phoneNumber(targetMember.getPhoneNumber())
                .status(targetMember.getStatus())
                .isAdmin(targetMember.getIsAdmin())
                .build();

        log.info("User {} switched to group {}", user.getUsername(), groupId);

        return ResponseEntity.ok(ApiResponse.success("Switched to group", memberResponse));
    }
}