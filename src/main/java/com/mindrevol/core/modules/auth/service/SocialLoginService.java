package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.utils.JwtUtil;
import com.mindrevol.core.modules.auth.dto.GoogleLoginDto;
import com.mindrevol.core.modules.auth.dto.SocialLoginResponse;
import com.mindrevol.core.modules.auth.dto.SocialUserInfo;
import com.mindrevol.core.modules.auth.entity.SocialAccount;
import com.mindrevol.core.modules.auth.factory.SocialLoginFactory;
import com.mindrevol.core.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.core.modules.auth.strategy.SocialLoginStrategy;
import com.mindrevol.core.modules.user.entity.Role;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserStatus;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Service xử lý Social Login
 *
 * Flow:
 * 1. Frontend gửi idToken từ Google
 * 2. Backend verify token qua GoogleLoginStrategy
 * 3. Nếu email tồn tại: tạo/cập nhật SocialAccount, trả JWT
 * 4. Nếu email mới: tạo User + SocialAccount, trả JWT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLoginService {

    private final SocialLoginFactory socialLoginFactory;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    /**
     * Google Login
     */
    public SocialLoginResponse loginWithGoogle(GoogleLoginDto request) throws Exception {
        log.info("Processing Google login...");

        // [1] Verify Google token qua Strategy
        SocialLoginStrategy googleStrategy = socialLoginFactory.getStrategy("google");
        SocialUserInfo userInfo = googleStrategy.authenticate(request.getIdToken());

        // [2] Gọi chung xử lý social login
        return processSocialLogin(userInfo);
    }

    /**
     * Generic Social Login (dùng chung cho Google, Apple, Facebook, etc.)
     *
     * Logic:
     * - Nếu email đã tồn tại: link social account + login
     * - Nếu email mới: tạo user + social account
     */
    @Transactional
    public SocialLoginResponse processSocialLogin(SocialUserInfo userInfo) {
        log.info("Processing social login for provider: {}, email: {}", userInfo.getProvider(), userInfo.getEmail());

        // [1] Kiểm tra social account đã tồn tại chưa
        boolean socialAccountExists = socialAccountRepository.existsByProviderAndProviderId(
                userInfo.getProvider(),
                userInfo.getProviderId()
        );

        User user;
        boolean isNewUser = false;

        if (socialAccountExists) {
            // [A] Social account đã tồn tại -> Lấy user từ database
            log.info("Social account exists, logging in existing user...");
            SocialAccount socialAccount = socialAccountRepository
                    .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Social account not found"));

            user = socialAccount.getUser();

        } else {
            // [B] Social account mới -> Kiểm tra email
            log.info("Creating new social login...");

            if (userRepository.existsByEmail(userInfo.getEmail())) {
                // Email đã tồn tại -> Link với user hiện tại
                log.info("Email already exists, linking social account to existing user...");
                user = userRepository.findByEmail(userInfo.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            } else {
                // Email mới -> Tạo user mới
                log.info("Creating new user with email: {}", userInfo.getEmail());
                user = createNewUserFromSocial(userInfo);
                isNewUser = true;
            }

            // Tạo social account mới
            SocialAccount socialAccount = SocialAccount.builder()
                    .user(user)
                    .provider(userInfo.getProvider())
                    .providerId(userInfo.getProviderId())
                    .email(userInfo.getEmail())
                    .avatarUrl(userInfo.getPicture())
                    .build();

            socialAccountRepository.save(socialAccount);
            log.info("Social account created: {} - {}", userInfo.getProvider(), userInfo.getProviderId());
        }

        // [2] Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // [3] Trả về response
        return SocialLoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getFullname())
                .handle(user.getHandle())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .message(isNewUser ? "User created and logged in successfully" : "Logged in successfully")
                .build();
    }

    /**
     * Tạo user mới từ social info
     */
    private User createNewUserFromSocial(SocialUserInfo userInfo) {
        // Generate unique handle từ email (nếu email là abc@gmail.com -> handle: abc)
        String baseHandle = userInfo.getEmail().split("@")[0];
        String handle = baseHandle;

        // Đảm bảo handle unique
        int counter = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = baseHandle + counter;
            counter++;
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role USER not found"));

        User newUser = User.builder()
                .email(userInfo.getEmail())
                .handle(handle)
                .fullname(userInfo.getName() != null ? userInfo.getName() : "User")
                .avatarUrl(userInfo.getPicture())
                .status(UserStatus.ACTIVE)  // Tự động active vì email verified bởi provider
                .authProvider(userInfo.getProvider().toUpperCase())
                .roles(new HashSet<>(Collections.singleton(userRole)))
                .build();

        // Set random password (không dùng vì login qua social)
        newUser.setPassword(UUID.randomUUID().toString());

        return userRepository.save(newUser);
    }
}

