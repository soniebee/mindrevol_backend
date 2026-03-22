package com.mindrevol.core.modules.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorBackupCodesResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorEnableResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorSetupResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorStatusResponse;
import com.mindrevol.core.modules.auth.entity.MagicLinkToken;
import com.mindrevol.core.modules.auth.entity.SocialAccount;
import com.mindrevol.core.modules.auth.repository.MagicLinkTokenRepository;
import com.mindrevol.core.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.core.modules.auth.service.AuthService;
import com.mindrevol.core.modules.auth.service.SessionService;
import com.mindrevol.core.modules.auth.service.TwoFactorService;
import com.mindrevol.core.modules.auth.service.strategy.SocialLoginFactory;
import com.mindrevol.core.modules.auth.service.strategy.SocialLoginStrategy;
import com.mindrevol.core.modules.auth.service.strategy.SocialProviderData;
import com.mindrevol.core.modules.auth.util.AppleAuthUtil;
import com.mindrevol.core.modules.notification.dto.EmailTask;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.entity.*;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.user.repository.UserSettingsRepository;
import com.mindrevol.core.modules.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    // ĐÃ XÓA OtpTokenRepository
    
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AsyncTaskProducer asyncTaskProducer;
    private final RedisTemplate<String, Object> redisTemplate; // Inject RedisTemplate
    
    // Services tách ra
    private final SessionService sessionService;
    private final TwoFactorService twoFactorService;
    private final SocialLoginFactory socialLoginFactory;
    
    // Inject các bean cũ
    private final RestTemplate restTemplate;
    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;
    
    @Value("${tiktok.client-key:}") private String tiktokClientKey;
    @Value("${tiktok.client-secret:}") private String tiktokClientSecret;
    @Value("${app.two-factor.issuer:MindRevol}") private String twoFactorIssuer;
    @Value("${app.two-factor.challenge-ttl-seconds:300}") private long twoFactorChallengeTtlSeconds;
    @Value("${app.two-factor.verify-window:1}") private int twoFactorVerifyWindow;
    @Value("${app.two-factor.backup-code-count:8}") private int twoFactorBackupCodeCount;

    // Hằng số Key Redis
    private static final String OTP_PREFIX = "auth:otp:code:";       
    private static final String OTP_LIMIT_PREFIX = "auth:otp:limit:"; 
    private static final String OTP_RETRY_PREFIX = "auth:otp:retry:"; 
    private static final String TWO_FACTOR_CHALLENGE_PREFIX = "auth:2fa:challenge:";

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new DisabledException("Account is locked.");
        return completeLoginWithOptionalTwoFactor(user, servletRequest, request.getDeviceId());
    }

    // ==================================================================================
    // SOCIAL LOGIN LOGIC
    // ==================================================================================

    private JwtResponse processUnifiedSocialLogin(String providerName, Object requestData, HttpServletRequest servletRequest) {
        SocialLoginStrategy strategy = socialLoginFactory.getStrategy(providerName);
        SocialProviderData data = strategy.verifyAndGetData(requestData);
        User user = findOrCreateUser(providerName, data);
        return completeLoginWithOptionalTwoFactor(user, servletRequest, null);
    }

    private User findOrCreateUser(String provider, SocialProviderData data) {
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(provider, data.getProviderId());
        if (socialAccountOpt.isPresent()) return socialAccountOpt.get().getUser();

        User user;
        Optional<User> existingUser = Optional.empty();
        if (data.getEmail() != null) existingUser = userRepository.findByEmail(data.getEmail());
        
        if (existingUser.isPresent()) user = existingUser.get();
        else user = createNewSocialUser(data.getEmail(), data.getName(), data.getAvatarUrl());

        SocialAccount newLink = SocialAccount.builder()
                .user(user).provider(provider).providerId(data.getProviderId())
                .email(data.getEmail()).avatarUrl(data.getAvatarUrl()).build();
        socialAccountRepository.save(newLink);
        return user;
    }

    private User createNewSocialUser(String email, String name, String avatarUrl) {
        String safeEmail = (email != null) ? email : "no-email-" + UUID.randomUUID() + "@mindrevol.local";
        String baseHandle = safeEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (baseHandle.isEmpty()) baseHandle = "user";
        
        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) handle = baseHandle + "." + (++suffix);
        
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        
        User newUser = User.builder()
                .email(safeEmail).fullname(name != null ? name : "New User")
                .avatarUrl(avatarUrl).handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE).gender(Gender.PREFER_NOT_TO_SAY)
                .accountType(AccountType.FREE).authProvider("SOCIAL")
                .roles(new HashSet<>(Collections.singletonList(userRole))).build();
        return userRepository.save(newUser);
    }

    @Override
    public JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("tiktok", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("google", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("facebook", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("apple", request, servletRequest);
    }

    // ==================================================================================
    // OTP REDIS IMPLEMENTATION
    // ==================================================================================

    @Override
    public void sendOtpLogin(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("This email is not registered."));

        // 1. Kiểm tra Rate Limit (60s)
        String limitKey = OTP_LIMIT_PREFIX + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long expire = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new BadRequestException("Please wait " + expire + " seconds before requesting another code.");
        }

        // 2. Sinh mã & Key
        String newCode = String.format("%06d", new Random().nextInt(999999));
        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        // 3. Lưu vào Redis
        redisTemplate.opsForValue().set(otpKey, newCode, 5, TimeUnit.MINUTES);
        redisTemplate.delete(retryKey); // Reset retry count
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS); // Set Rate Limit

        // 4. Gửi Email (Async)
        String subject = "MindRevol login verification code";
        String content = "<h1>Your OTP code: " + newCode + "</h1><p>Expires in 5 minutes.</p>";
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject(subject).content(content).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
        
        log.info("Sent OTP login to user: {}", user.getEmail());
    }

    @Override
    public JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        // 1. Kiểm tra OTP tồn tại
        String cachedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null) throw new BadRequestException("OTP code has expired or has not been sent.");

        // 2. Kiểm tra số lần sai
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount == null) retryCount = 0;
        if (retryCount >= 5) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(retryKey);
            throw new BadRequestException("You entered an incorrect OTP more than 5 times. Please request a new code.");
        }

        // 3. So sánh
        if (!cachedOtp.equals(request.getOtpCode())) {
            redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 5, TimeUnit.MINUTES);
            throw new BadRequestException("Incorrect OTP code. (Attempt " + (retryCount + 1) + "/5)");
        }

        // 4. Thành công -> Dọn dẹp
        redisTemplate.delete(otpKey);
        redisTemplate.delete(retryKey);
        redisTemplate.delete(OTP_LIMIT_PREFIX + request.getEmail());

        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        return completeLoginWithOptionalTwoFactor(user, servletRequest, null);
    }

    // ==================================================================================
    // MAGIC LINK & PROFILE
    // ==================================================================================

    @Override
    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Email is not registered"));
        MagicLinkToken magicToken = MagicLinkToken.create(user);
        magicLinkTokenRepository.save(magicToken);
        String link = "http://localhost:5173/magic-login?token=" + magicToken.getToken();
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject("Magic Link").content("Link: " + link).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public JwtResponse loginWithMagicLink(String token, HttpServletRequest request) {
        MagicLinkToken magicToken = magicLinkTokenRepository.findByToken(token).orElseThrow(() -> new BadRequestException("Link invalid"));
        if (magicToken.isExpired()) {
            magicLinkTokenRepository.delete(magicToken);
            throw new BadRequestException("Link expired");
        }
        User user = magicToken.getUser();
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        magicLinkTokenRepository.delete(magicToken);
        return completeLoginWithOptionalTwoFactor(user, request, null);
    }

    @Override
    public TwoFactorSetupResponse setupTwoFactor(String userEmail, boolean revealSecret) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);

        String secret = twoFactorService.generateSecret();
        settings.setTwoFactorTempSecret(secret);
        userSettingsRepository.save(settings);

        String otpAuthUri = twoFactorService.buildOtpAuthUri(twoFactorIssuer, user.getEmail(), secret);
        String qrCode = twoFactorService.generateQrCodeImage(otpAuthUri);
        
        return TwoFactorSetupResponse.builder()
                .otpAuthUri(otpAuthUri)
                .qrCode(qrCode)
                .secret(revealSecret ? secret : null)
                .manualSecretVisible(revealSecret)
                .build();
    }

    @Override
    public TwoFactorEnableResponse enableTwoFactor(String userEmail, TwoFactorEnableRequest request) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);

        if (settings.getTwoFactorTempSecret() == null || settings.getTwoFactorTempSecret().isBlank()) {
            throw new BadRequestException("Two-factor setup is not initialized.");
        }

        boolean valid = twoFactorService.verifyCode(settings.getTwoFactorTempSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        if (!valid) {
            throw new BadRequestException("Invalid OTP code.");
        }

        settings.setTwoFactorEnabled(true);
        settings.setTwoFactorSecret(settings.getTwoFactorTempSecret());
        settings.setTwoFactorTempSecret(null);
        settings.setTwoFactorBackupCodes(null);
        LocalDateTime enabledAt = LocalDateTime.now();
        settings.setTwoFactorEnabledAt(enabledAt);
        userSettingsRepository.save(settings);

        return TwoFactorEnableResponse.builder()
                .enabled(true)
                .enabledAt(enabledAt)
                .build();
    }

    @Override
    public TwoFactorBackupCodesResponse generateTwoFactorBackupCodes(String userEmail, TwoFactorGenerateBackupCodesRequest request) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);

        if (!settings.isTwoFactorEnabled() || settings.getTwoFactorSecret() == null || settings.getTwoFactorSecret().isBlank()) {
            throw new BadRequestException("Two-factor authentication is not enabled.");
        }

        boolean valid = twoFactorService.verifyCode(settings.getTwoFactorSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        if (!valid) {
            throw new BadRequestException("Invalid OTP code.");
        }

        List<String> plainBackupCodes = twoFactorService.generateBackupCodes(twoFactorBackupCodeCount);
        List<String> hashedBackupCodes = plainBackupCodes.stream().map(twoFactorService::hashCode).toList();
        settings.setTwoFactorBackupCodes(writeBackupCodes(hashedBackupCodes));
        userSettingsRepository.save(settings);

        return TwoFactorBackupCodesResponse.builder()
                .backupCodes(plainBackupCodes)
                .build();
    }

    @Override
    public void disableTwoFactor(String userEmail, TwoFactorDisableRequest request) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);

        if (!settings.isTwoFactorEnabled()) {
            return;
        }

        boolean validByOtp = request.getOtpCode() != null && twoFactorService.verifyCode(settings.getTwoFactorSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        boolean validByBackup = consumeBackupCode(settings, request.getBackupCode());
        if (!validByOtp && !validByBackup) {
            throw new BadRequestException("Invalid OTP or backup code.");
        }

        settings.setTwoFactorEnabled(false);
        settings.setTwoFactorSecret(null);
        settings.setTwoFactorTempSecret(null);
        settings.setTwoFactorBackupCodes(null);
        settings.setTwoFactorEnabledAt(null);
        userSettingsRepository.save(settings);
    }

    @Override
    public TwoFactorStatusResponse getTwoFactorStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);
        int backupCodesLeft = readBackupCodes(settings.getTwoFactorBackupCodes()).size();
        return TwoFactorStatusResponse.builder()
                .enabled(settings.isTwoFactorEnabled())
                .backupCodesLeft(backupCodesLeft)
                .build();
    }

    @Override
    public JwtResponse verifyTwoFactorLogin(TwoFactorLoginVerifyRequest request, HttpServletRequest servletRequest) {
        Map<String, Object> challenge = getTwoFactorChallenge(request.getChallengeId());
        String userId = challenge.get("userId") instanceof String value ? value : null;
        String email = challenge.get("email") instanceof String value ? value : null;
        String originalDeviceId = challenge.get("deviceId") instanceof String value ? value : null;

        if (userId == null || email == null) {
            throw new BadRequestException("Invalid two-factor challenge.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);
        if (!settings.isTwoFactorEnabled() || settings.getTwoFactorSecret() == null || settings.getTwoFactorSecret().isBlank()) {
            throw new BadRequestException("Two-factor authentication is not enabled.");
        }

        boolean validByOtp = request.getOtpCode() != null && twoFactorService.verifyCode(settings.getTwoFactorSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        boolean validByBackup = consumeBackupCode(settings, request.getBackupCode());
        if (!validByOtp && !validByBackup) {
            throw new BadRequestException("Invalid OTP or backup code.");
        }

        deleteTwoFactorChallenge(request.getChallengeId());
        String finalDeviceId = request.getDeviceId() != null && !request.getDeviceId().isBlank() ? request.getDeviceId() : originalDeviceId;
        return sessionService.createTokenAndSession(user, servletRequest, finalDeviceId);
    }

    private JwtResponse completeLoginWithOptionalTwoFactor(User user, HttpServletRequest servletRequest, String deviceId) {
        UserSettings settings = getOrCreateSettings(user);
        
        // Skip 2FA for social login users - they are already verified by the social provider
        if ("SOCIAL".equals(user.getAuthProvider()) || !settings.isTwoFactorEnabled()) {
            return sessionService.createTokenAndSession(user, servletRequest, deviceId);
        }

        String challengeId = UUID.randomUUID().toString();
        Map<String, Object> challenge = new HashMap<>();
        challenge.put("userId", user.getId());
        challenge.put("email", user.getEmail());
        challenge.put("deviceId", deviceId != null && !deviceId.isBlank() ? deviceId : resolveHeaderDeviceId(servletRequest));
        challenge.put("createdAt", System.currentTimeMillis());

        redisTemplate.opsForValue().set(TWO_FACTOR_CHALLENGE_PREFIX + challengeId, challenge, twoFactorChallengeTtlSeconds, TimeUnit.SECONDS);

        return JwtResponse.builder()
                .requiresTwoFactor(true)
                .challengeId(challengeId)
                .message("Two-factor verification required.")
                .build();
    }

    private String resolveHeaderDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String headerValue = request.getHeader("X-Device-Id");
        if (headerValue == null || headerValue.isBlank()) {
            headerValue = request.getHeader("x-device-id");
        }
        return headerValue == null ? null : headerValue.trim();
    }

    private UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> userSettingsRepository.save(UserSettings.builder().user(user).build()));
    }

    private String writeBackupCodes(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist backup codes", ex);
        }
    }

    private List<String> readBackupCodes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            log.warn("Cannot parse backup codes JSON, resetting value", ex);
            return new ArrayList<>();
        }
    }

    private boolean consumeBackupCode(UserSettings settings, String backupCode) {
        if (backupCode == null || backupCode.isBlank()) {
            return false;
        }
        List<String> hashedCodes = readBackupCodes(settings.getTwoFactorBackupCodes());
        String incomingHash = twoFactorService.hashCode(backupCode);
        boolean removed = hashedCodes.removeIf(code -> code.equals(incomingHash));
        if (removed) {
            settings.setTwoFactorBackupCodes(writeBackupCodes(hashedCodes));
            userSettingsRepository.save(settings);
        }
        return removed;
    }

    private Map<String, Object> getTwoFactorChallenge(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            throw new BadRequestException("Challenge id is required.");
        }
        Object raw = redisTemplate.opsForValue().get(TWO_FACTOR_CHALLENGE_PREFIX + challengeId);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new BadRequestException("Two-factor challenge expired or invalid.");
        }
        Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private void deleteTwoFactorChallenge(String challengeId) {
        if (challengeId != null && !challengeId.isBlank()) {
            redisTemplate.delete(TWO_FACTOR_CHALLENGE_PREFIX + challengeId);
        }
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String userEmail) {
        return userService.getMyProfile(userEmail);
    }
}
