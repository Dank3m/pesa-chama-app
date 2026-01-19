package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SettingsService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== PROFILE METHODS ====================

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findByIdWithMemberAndGroup(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        return mapToProfileResponse(user);
    }

    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdWithMemberAndGroup(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Member member = user.getMember();
        if (member == null) {
            throw new BusinessException("User has no associated member profile");
        }

        // Update fields if provided
        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            // Validate unique email within group
            if (!request.getEmail().equals(member.getEmail()) &&
                    memberRepository.existsByGroupIdAndEmail(member.getGroup().getId(), request.getEmail())) {
                throw new BusinessException("Email already registered in this group");
            }
            member.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            // Validate unique phone within group
            if (!request.getPhoneNumber().equals(member.getPhoneNumber()) &&
                    memberRepository.existsByGroupIdAndPhoneNumber(member.getGroup().getId(), request.getPhoneNumber())) {
                throw new BusinessException("Phone number already registered in this group");
            }
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
        }

        memberRepository.save(member);
        log.info("Profile updated for user: {}", userId);

        return mapToProfileResponse(user);
    }

    // ==================== SETTINGS/PREFERENCES METHODS ====================

    @Transactional(readOnly = true)
    public UserSettingsResponse getUserSettings(UUID userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return mapToUserSettingsResponse(settings);
    }

    public UserSettingsResponse updateUserSettings(UUID userId, UpdateUserSettingsRequest request) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        if (request.getCurrency() != null) {
            settings.setCurrency(request.getCurrency());
        }
        if (request.getTimezone() != null) {
            settings.setTimezone(request.getTimezone());
        }
        if (request.getNotifyDigitalPayment() != null) {
            settings.setNotifyDigitalPayment(request.getNotifyDigitalPayment());
        }
        if (request.getNotifyRecommendations() != null) {
            settings.setNotifyRecommendations(request.getNotifyRecommendations());
        }

        settings = userSettingsRepository.save(settings);
        log.info("Settings updated for user: {}", userId);

        return mapToUserSettingsResponse(settings);
    }

    private UserSettings createDefaultSettings(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        UserSettings settings = UserSettings.builder()
                .user(user)
                .build();
        return userSettingsRepository.save(settings);
    }

    // ==================== SECURITY METHODS ====================

    @Transactional(readOnly = true)
    public SecuritySettingsResponse getSecuritySettings(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        return SecuritySettingsResponse.builder()
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .lastLogin(user.getLastLogin())
                .build();
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Prevent using the same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    public SecuritySettingsResponse toggle2FA(UUID userId, Toggle2FARequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (request.getEnabled()) {
            // Enable 2FA
            user.setTwoFactorEnabled(true);
            log.info("2FA enabled for user: {}", userId);
        } else {
            // Disable 2FA - require password verification
            if (request.getPassword() == null ||
                    !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BusinessException("Password required to disable 2FA");
            }
            user.setTwoFactorEnabled(false);
            user.setTwoFactorSecret(null);
            log.info("2FA disabled for user: {}", userId);
        }

        userRepository.save(user);

        return SecuritySettingsResponse.builder()
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .lastLogin(user.getLastLogin())
                .build();
    }

    // ==================== MAPPING METHODS ====================

    private ProfileResponse mapToProfileResponse(User user) {
        Member member = user.getMember();

        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(user.getId());

        if (member != null) {
            builder.firstName(member.getFirstName())
                    .lastName(member.getLastName())
                    .fullName(member.getFullName())
                    .email(member.getEmail())
                    .phoneNumber(member.getPhoneNumber())
                    .address(member.getAddress())
                    .dateOfBirth(member.getDateOfBirth())
                    .memberNumber(member.getMemberNumber())
                    .groupId(member.getGroup().getId())
                    .groupName(member.getGroup().getName());
        }

        return builder.build();
    }

    private UserSettingsResponse mapToUserSettingsResponse(UserSettings settings) {
        return UserSettingsResponse.builder()
                .id(settings.getId())
                .currency(settings.getCurrency())
                .timezone(settings.getTimezone())
                .notifyDigitalPayment(settings.getNotifyDigitalPayment())
                .notifyRecommendations(settings.getNotifyRecommendations())
                .build();
    }
}
