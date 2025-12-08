package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.Member;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.MemberRepository;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
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
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        // Get member
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("Member not found"));

        // Check if member already has a user account
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
