package com.mindrevol.core.modules.auth.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.auth.dto.BackupCodeStatusDto;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.*;
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
import com.mindrevol.core.modules.auth.util.BackupCodeUtil;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String OTP_PREFIX = "auth:otp:code:";
    private static final String OTP_LIMIT_PREFIX = "auth:otp:limit:";
    private static final String OTP_RETRY_PREFIX = "auth:otp:retry:";

    private static final String TWO_FACTOR_CHALLENGE_PREFIX = "auth:2fa:challenge:";
    private static final String TWO_FACTOR_SETUP_PREFIX = "auth:2fa:setup:";
    private static final String TWO_FACTOR_BACKUP_CODES_DOWNLOAD_PREFIX = "auth:2fa:backup-codes:download:";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final AsyncTaskProducer asyncTaskProducer;
    private final SessionService sessionService;
    private final TwoFactorService twoFactorService;
    private final SocialLoginFactory socialLoginFactory;
    private final UserService userService;

    private final BackupCodeUtil backupCodeUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.two-factor.issuer:MindRevol}")
    private String twoFactorIssuer;

    @Value("${app.two-factor.challenge-ttl-seconds:300}")
    private long twoFactorChallengeTtlSeconds;

    @Value("${app.two-factor.verify-window:1}")
    private int twoFactorVerifyWindow;

    @Value("${app.two-factor.backup-code-count:8}")
    private int twoFactorBackupCodeCount;

    // -------------------------------------------------------------------------
    // Basic login
    // -------------------------------------------------------------------------

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Account is locked.");
        }

        return completeLoginWithOptionalTwoFactor(user, servletRequest);
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

    @Override
    public JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("tiktok", request, servletRequest);
    }

    private JwtResponse processUnifiedSocialLogin(String providerName, Object requestData, HttpServletRequest servletRequest) {
        SocialLoginStrategy strategy = socialLoginFactory.getStrategy(providerName);
        SocialProviderData data = strategy.verifyAndGetData(requestData);
        User user = findOrCreateUser(providerName, data);
        return completeLoginWithOptionalTwoFactor(user, servletRequest);
    }

    private User findOrCreateUser(String provider, SocialProviderData data) {
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(provider, data.getProviderId());
        if (socialAccountOpt.isPresent()) {
            return socialAccountOpt.get().getUser();
        }

        User user;
        Optional<User> existingUser = Optional.empty();
        if (data.getEmail() != null) {
            existingUser = userRepository.findByEmail(data.getEmail());
        }

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = createNewSocialUser(data.getEmail(), data.getName(), data.getAvatarUrl());
        }

        SocialAccount newLink = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(data.getProviderId())
                .email(data.getEmail())
                .avatarUrl(data.getAvatarUrl())
                .build();
        socialAccountRepository.save(newLink);

        return user;
    }

    private User createNewSocialUser(String email, String name, String avatarUrl) {
        String safeEmail = (email != null) ? email : "no-email-" + UUID.randomUUID() + "@mindrevol.local";
        String baseHandle = safeEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (baseHandle.isEmpty()) {
            baseHandle = "user";
        }

        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = baseHandle + "." + (++suffix);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User newUser = User.builder()
                .email(safeEmail)
                .fullname(name != null ? name : "New User")
                .avatarUrl(avatarUrl)
                .handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE)
                .gender(Gender.PREFER_NOT_TO_SAY)
                .accountType(AccountType.FREE)
                .authProvider("SOCIAL")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        return userRepository.save(newUser);
    }

    // -------------------------------------------------------------------------
    // OTP / Magic link
    // -------------------------------------------------------------------------

    @Override
    public void sendOtpLogin(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("This email is not registered."));

        String limitKey = OTP_LIMIT_PREFIX + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long expire = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new BadRequestException("Please wait " + expire + " seconds before requesting another code.");
        }

        String newCode = String.format("%06d", new Random().nextInt(1_000_000));
        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        redisTemplate.opsForValue().set(otpKey, newCode, 5, TimeUnit.MINUTES);
        redisTemplate.delete(retryKey);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);

        String subject = "MindRevol login verification code";
        String content = "<h1>Your OTP code: " + newCode + "</h1><p>Expires in 5 minutes.</p>";
        EmailTask task = EmailTask.builder()
                .toEmail(user.getEmail())
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);

        log.info("Sent OTP login to user: {}", user.getEmail());
    }

    @Override
    public JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        String cachedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null) {
            throw new BadRequestException("OTP code has expired or has not been sent.");
        }

        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount == null) {
            retryCount = 0;
        }
        if (retryCount >= 5) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(retryKey);
            throw new BadRequestException("You entered an incorrect OTP more than 5 times. Please request a new code.");
        }

        if (!cachedOtp.equals(request.getOtpCode())) {
            redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 5, TimeUnit.MINUTES);
            throw new BadRequestException("Incorrect OTP code. (Attempt " + (retryCount + 1) + "/5)");
        }

        redisTemplate.delete(otpKey);
        redisTemplate.delete(retryKey);
        redisTemplate.delete(OTP_LIMIT_PREFIX + request.getEmail());

        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        return completeLoginWithOptionalTwoFactor(user, servletRequest);
    }

    @Override
    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email is not registered"));

        MagicLinkToken magicToken = MagicLinkToken.create(user);
        magicLinkTokenRepository.save(magicToken);

        String link = "http://localhost:5173/magic-login?token=" + magicToken.getToken();
        EmailTask task = EmailTask.builder()
                .toEmail(user.getEmail())
                .subject("Magic Link")
                .content("Link: " + link)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public JwtResponse loginWithMagicLink(String token, HttpServletRequest request) {
        MagicLinkToken magicToken = magicLinkTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Link invalid"));

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
        return completeLoginWithOptionalTwoFactor(user, request);
    }

    // -------------------------------------------------------------------------
    // 2FA setup / status
    // -------------------------------------------------------------------------

    @Override
    public TwoFactorSetupResponse setupTwoFactor(String userEmail, boolean revealSecret) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        String secret = twoFactorService.generateSecret();
        settings.setTwoFactorTempSecret(secret);
        settings.setTwoFactorMethod("TOTP");
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
    public TwoFactorMethodResponse setupTwoFactorMethod(String userEmail, TwoFactorMethodRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);
        String method = normalizeTwoFactorMethod(request.getMethod());

        if ("EMAIL".equals(method)) {
            String email = resolveEmailForTwoFactor(user, request.getEmail());
            String token = UUID.randomUUID().toString();

            settings.setTwoFactorMethod("EMAIL");
            settings.setTwoFactorEmail(email);
            settings.setTwoFactorEmailVerified(false);
            settings.setTwoFactorEmailVerificationToken(token);
            settings.setTwoFactorEnabled(false);
            userSettingsRepository.save(settings);

            sendTwoFactorEmailVerification(email, token);

            return TwoFactorMethodResponse.builder()
                    .method("EMAIL")
                    .message("Verification email sent.")
                    .verificationToken(token)
                    .build();
        }

        settings.setTwoFactorMethod("TOTP");
        settings.setTwoFactorEmail(null);
        settings.setTwoFactorEmailVerified(false);
        settings.setTwoFactorEmailVerificationToken(null);
        settings.setTwoFactorEnabled(false);
        userSettingsRepository.save(settings);

        return TwoFactorMethodResponse.builder()
                .method("TOTP")
                .message("TOTP selected. Call /api/v1/auth/2fa/setup to start.")
                .build();
    }

    @Override
    public void verifyTwoFactorEmail(String userEmail, TwoFactorVerifyEmailRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        if (!"EMAIL".equalsIgnoreCase(defaultMethod(settings.getTwoFactorMethod()))) {
            throw new BadRequestException("Two-factor method is not EMAIL.");
        }

        if (settings.getTwoFactorEmailVerificationToken() == null
                || !settings.getTwoFactorEmailVerificationToken().equals(request.getToken())) {
            throw new BadRequestException("Invalid or expired verification token.");
        }

        settings.setTwoFactorEmailVerified(true);
        settings.setTwoFactorEmailVerificationToken(null);
        settings.setTwoFactorEnabled(true);
        settings.setTwoFactorEnabledAt(LocalDateTime.now());
        userSettingsRepository.save(settings);
    }

    @Override
    public void resendTwoFactorEmailVerification(String userEmail) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        if (!"EMAIL".equalsIgnoreCase(defaultMethod(settings.getTwoFactorMethod()))) {
            throw new BadRequestException("Two-factor method is not EMAIL.");
        }

        if (settings.getTwoFactorEmail() == null || settings.getTwoFactorEmail().isBlank()) {
            throw new BadRequestException("Two-factor email is missing.");
        }

        String token = UUID.randomUUID().toString();
        settings.setTwoFactorEmailVerificationToken(token);
        settings.setTwoFactorEmailVerified(false);
        userSettingsRepository.save(settings);

        sendTwoFactorEmailVerification(settings.getTwoFactorEmail(), token);
    }

    @Override
    public TwoFactorEnableResponse enableTwoFactor(String userEmail, TwoFactorEnableRequest request) {
        User user = findUserByEmail(userEmail);
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
        settings.setTwoFactorMethod("TOTP");
        settings.setTwoFactorEnabledAt(LocalDateTime.now());
        userSettingsRepository.save(settings);

        return TwoFactorEnableResponse.builder()
                .enabled(true)
                .enabledAt(settings.getTwoFactorEnabledAt())
                .build();
    }

    @Override
    public TwoFactorBackupCodesResponse generateTwoFactorBackupCodes(String userEmail, TwoFactorGenerateBackupCodesRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        if (!settings.isTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is not enabled.");
        }

        if (!"TOTP".equalsIgnoreCase(defaultMethod(settings.getTwoFactorMethod()))
                || settings.getTwoFactorSecret() == null
                || settings.getTwoFactorSecret().isBlank()) {
            throw new BadRequestException("Backup code generation currently requires TOTP to be enabled.");
        }

        boolean valid = twoFactorService.verifyCode(settings.getTwoFactorSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        if (!valid) {
            throw new BadRequestException("Invalid OTP code.");
        }

        List<String> plainBackupCodes = twoFactorService.generateBackupCodes(twoFactorBackupCodeCount);
        List<String> hashedBackupCodes = plainBackupCodes.stream().map(twoFactorService::hashCode).toList();
        settings.setTwoFactorBackupCodes(backupCodeUtil.serializeBackupCodes(backupCodeUtil.createBackupCodes(hashedBackupCodes)));
        userSettingsRepository.save(settings);

        LocalDateTime generatedAt = LocalDateTime.now();
        redisTemplate.opsForValue().set(
                TWO_FACTOR_BACKUP_CODES_DOWNLOAD_PREFIX + user.getId(),
                backupCodeUtil.formatBackupCodesForDownload(plainBackupCodes, generatedAt),
                10,
                TimeUnit.MINUTES
        );

        return TwoFactorBackupCodesResponse.builder()
                .backupCodes(plainBackupCodes)
                .build();
    }

    @Override
    public String downloadTwoFactorBackupCodes(String userEmail) {
        User user = findUserByEmail(userEmail);
        String key = TWO_FACTOR_BACKUP_CODES_DOWNLOAD_PREFIX + user.getId();
        Object content = redisTemplate.opsForValue().get(key);

        if (!(content instanceof String value) || value.isBlank()) {
            throw new BadRequestException("No backup codes available for download. Generate new backup codes first.");
        }

        redisTemplate.delete(key);
        return value;
    }

    @Override
    public void disableTwoFactor(String userEmail, TwoFactorDisableRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        if (!settings.isTwoFactorEnabled()) {
            return;
        }

        String otpCode = request != null ? request.getOtpCode() : null;
        String backupCode = request != null ? request.getBackupCode() : null;
        if (!isValidTwoFactorProof(settings, otpCode, backupCode)) {
            throw new BadRequestException("Invalid OTP or backup code.");
        }

        clearTwoFactor(settings, true);
        userSettingsRepository.save(settings);
    }

    @Override
    public TwoFactorStatusResponse getTwoFactorStatus(String userEmail) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        List<BackupCodeStatusDto> backupCodes = backupCodeUtil.parseBackupCodes(settings.getTwoFactorBackupCodes());
        int backupCodesLeft = backupCodeUtil.countUnusedCodes(backupCodes);
        List<BackupCodeStatusDto> safeBackupCodes = buildSafeBackupCodeStatus(backupCodes);

        List<TwoFactorMethodStatusResponse> methodStatuses = getTwoFactorMethods(userEmail);

        boolean totpEnabled = methodStatuses.stream().anyMatch(s -> "TOTP".equals(s.getMethod()) && s.isEnabled());
        boolean emailEnabled = methodStatuses.stream().anyMatch(s -> "EMAIL".equals(s.getMethod()) && s.isEnabled());
        boolean backupEnabled = backupCodesLeft > 0;

        List<String> enabledMethods = new ArrayList<>();
        if (totpEnabled) {
            enabledMethods.add("TOTP");
        }
        if (emailEnabled) {
            enabledMethods.add("EMAIL");
        }
        if (backupEnabled) {
            enabledMethods.add("BACKUP_CODES");
        }

        String currentMethod = settings.isTwoFactorEnabled() ? defaultMethod(settings.getTwoFactorMethod()) : null;

        return TwoFactorStatusResponse.builder()
                .enabled(settings.isTwoFactorEnabled())
                .method(currentMethod)
                .email(maskEmail(settings.getTwoFactorEmail()))
                .backupCodesLeft(backupCodesLeft)
                .enabledAt(settings.getTwoFactorEnabledAt())
                .backupCodesList(safeBackupCodes)
                .downloadBackupCodesUrl("/api/v1/auth/2fa/backup-codes/download")
                .totpEnabled(totpEnabled)
                .emailOtpEnabled(emailEnabled)
                .backupCodesEnabled(backupEnabled)
                .methodStatuses(methodStatuses)
                .enabledMethods(enabledMethods)
                .enabledMethodCount(enabledMethods.size())
                .build();
    }

    @Override
    public JwtResponse verifyTwoFactorLogin(TwoFactorLoginVerifyRequest request, HttpServletRequest servletRequest) {
        Map<String, Object> challenge = getTwoFactorChallenge(request.getChallengeId());
        String userId = asString(challenge.get("userId"));
        String challengeMethod = defaultMethod(asString(challenge.get("method")));
        String challengeEmailCodeHash = asString(challenge.get("emailCodeHash"));

        if (userId == null) {
            throw new BadRequestException("Invalid two-factor challenge.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserSettings settings = getOrCreateSettings(user);

        boolean validByOtp = false;
        if ("TOTP".equals(challengeMethod)) {
            validByOtp = request.getOtpCode() != null
                    && settings.getTwoFactorSecret() != null
                    && twoFactorService.verifyCode(settings.getTwoFactorSecret(), request.getOtpCode(), twoFactorVerifyWindow);
        } else if ("EMAIL".equals(challengeMethod)) {
            validByOtp = request.getOtpCode() != null
                    && challengeEmailCodeHash != null
                    && challengeEmailCodeHash.equals(twoFactorService.hashCode(request.getOtpCode()));
        }

        boolean validByBackup = consumeBackupCode(settings, request.getBackupCode());
        if (!validByOtp && !validByBackup) {
            throw new BadRequestException("Invalid two-factor code.");
        }

        deleteTwoFactorChallenge(request.getChallengeId());
        return sessionService.createTokenAndSession(user, servletRequest);
    }

    // -------------------------------------------------------------------------
    // New multi-method endpoints (compatibility wrappers)
    // -------------------------------------------------------------------------

    @Override
    public StartTwoFactorMethodSetupResponse startTwoFactorMethodSetup(String userEmail, StartTwoFactorMethodSetupRequest request, boolean revealSecret) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);
        String method = normalizeTwoFactorMethod(request != null ? request.getMethod() : null);

        String setupToken = UUID.randomUUID().toString();
        Map<String, Object> setupPayload = new HashMap<>();
        setupPayload.put("userId", user.getId());
        setupPayload.put("method", method);

        if ("TOTP".equals(method)) {
            String secret = twoFactorService.generateSecret();
            settings.setTwoFactorTempSecret(secret);
            settings.setTwoFactorMethod("TOTP");
            userSettingsRepository.save(settings);

            String otpAuthUri = twoFactorService.buildOtpAuthUri(twoFactorIssuer, user.getEmail(), secret);
            String qrCode = twoFactorService.generateQrCodeImage(otpAuthUri);

            setupPayload.put("tempSecret", secret);
            redisTemplate.opsForValue().set(TWO_FACTOR_SETUP_PREFIX + setupToken, setupPayload, twoFactorChallengeTtlSeconds, TimeUnit.SECONDS);

            return StartTwoFactorMethodSetupResponse.builder()
                    .method("TOTP")
                    .qrCode(qrCode)
                    .otpAuthUri(otpAuthUri)
                    .secret(revealSecret ? secret : null)
                    .setupToken(setupToken)
                    .message("Scan QR code with authenticator app and verify with 6-digit code")
                    .build();
        }

        String email = resolveEmailForTwoFactor(user, request != null ? request.getEmail() : null);
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        setupPayload.put("email", email);
        setupPayload.put("emailCodeHash", twoFactorService.hashCode(code));
        redisTemplate.opsForValue().set(TWO_FACTOR_SETUP_PREFIX + setupToken, setupPayload, twoFactorChallengeTtlSeconds, TimeUnit.SECONDS);

        settings.setTwoFactorMethod("EMAIL");
        settings.setTwoFactorEmail(email);
        settings.setTwoFactorEmailVerified(false);
        settings.setTwoFactorEnabled(false);
        settings.setTwoFactorEmailVerificationToken(null);
        userSettingsRepository.save(settings);

        sendTwoFactorLoginCode(email, code);

        return StartTwoFactorMethodSetupResponse.builder()
                .method("EMAIL")
                .setupToken(setupToken)
                .email(maskEmail(email))
                .message("Verification code sent to email")
                .build();
    }

    @Override
    public ConfirmTwoFactorMethodResponse confirmTwoFactorMethodSetup(String userEmail, EnableTwoFactorMethodRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        if (request.getSetupToken() == null || request.getSetupToken().isBlank()) {
            throw new BadRequestException("Setup not initialized. Call start-setup first.");
        }

        Object raw = redisTemplate.opsForValue().get(TWO_FACTOR_SETUP_PREFIX + request.getSetupToken());
        if (!(raw instanceof Map<?, ?> rawMap)) {
            throw new BadRequestException("Setup not initialized. Call start-setup first.");
        }

        Map<String, Object> payload = new HashMap<>();
        rawMap.forEach((k, v) -> payload.put(String.valueOf(k), v));

        String payloadUserId = asString(payload.get("userId"));
        if (!user.getId().equals(payloadUserId)) {
            throw new BadRequestException("Invalid setup token.");
        }

        String method = defaultMethod(asString(payload.get("method")));
        LocalDateTime now = LocalDateTime.now();

        if ("TOTP".equals(method)) {
            String tempSecret = asString(payload.get("tempSecret"));
            if (tempSecret == null || tempSecret.isBlank()) {
                throw new BadRequestException("Setup not initialized. Call start-setup first.");
            }
            if (request.getOtpCode() == null || !twoFactorService.verifyCode(tempSecret, request.getOtpCode(), twoFactorVerifyWindow)) {
                throw new BadRequestException("Invalid OTP code.");
            }

            settings.setTwoFactorEnabled(true);
            settings.setTwoFactorMethod("TOTP");
            settings.setTwoFactorSecret(tempSecret);
            settings.setTwoFactorTempSecret(null);
            settings.setTwoFactorEnabledAt(now);
            userSettingsRepository.save(settings);

            redisTemplate.delete(TWO_FACTOR_SETUP_PREFIX + request.getSetupToken());

            return ConfirmTwoFactorMethodResponse.builder()
                    .method("TOTP")
                    .enabled(true)
                    .enabledAt(now)
                    .message("TOTP method successfully enabled")
                    .backupCodesRequired(true)
                    .build();
        }

        String emailCodeHash = asString(payload.get("emailCodeHash"));
        String email = asString(payload.get("email"));
        if (request.getOtpCode() == null || emailCodeHash == null
                || !emailCodeHash.equals(twoFactorService.hashCode(request.getOtpCode()))) {
            throw new BadRequestException("Invalid email verification code.");
        }

        settings.setTwoFactorEnabled(true);
        settings.setTwoFactorMethod("EMAIL");
        settings.setTwoFactorEmail(email);
        settings.setTwoFactorEmailVerified(true);
        settings.setTwoFactorEmailVerificationToken(null);
        settings.setTwoFactorTempSecret(null);
        settings.setTwoFactorSecret(null);
        settings.setTwoFactorEnabledAt(now);
        userSettingsRepository.save(settings);

        redisTemplate.delete(TWO_FACTOR_SETUP_PREFIX + request.getSetupToken());

        return ConfirmTwoFactorMethodResponse.builder()
                .method("EMAIL")
                .enabled(true)
                .enabledAt(now)
                .message("EMAIL method successfully enabled")
                .backupCodesRequired(false)
                .build();
    }

    @Override
    public void disableTwoFactorMethod(String userEmail, DisableTwoFactorMethodRequest request) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);
        String method = normalizeTwoFactorMethodWithBackup(request.getMethod());

        if ("BACKUP_CODES".equals(method)) {
            if (!isValidTwoFactorProof(settings, request.getOtpCode(), request.getBackupCode())) {
                throw new BadRequestException("Invalid OTP or backup code.");
            }
            settings.setTwoFactorBackupCodes(null);
            userSettingsRepository.save(settings);
            return;
        }

        String current = defaultMethod(settings.getTwoFactorMethod());
        if (!method.equals(current)) {
            return;
        }

        if (!isValidTwoFactorProof(settings, request.getOtpCode(), request.getBackupCode())) {
            throw new BadRequestException("Invalid OTP or backup code.");
        }

        clearTwoFactor(settings, false);
        userSettingsRepository.save(settings);
    }

    @Override
    public List<TwoFactorMethodStatusResponse> getTwoFactorMethods(String userEmail) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        List<TwoFactorMethodStatusResponse> result = new ArrayList<>();
        String method = defaultMethod(settings.getTwoFactorMethod());

        boolean totpConfigured = settings.getTwoFactorSecret() != null && !settings.getTwoFactorSecret().isBlank();
        boolean totpEnabled = settings.isTwoFactorEnabled() && "TOTP".equals(method) && totpConfigured;
        if (totpConfigured || "TOTP".equals(method)) {
            result.add(TwoFactorMethodStatusResponse.builder()
                    .method("TOTP")
                    .enabled(totpEnabled)
                    .readyToUse(totpEnabled)
                    .enabledAt(totpEnabled ? settings.getTwoFactorEnabledAt() : null)
                    .emailVerificationPending(false)
                    .backupCodesRemaining(0)
                    .build());
        }

        boolean emailConfigured = settings.getTwoFactorEmail() != null && !settings.getTwoFactorEmail().isBlank();
        boolean emailReady = emailConfigured && Boolean.TRUE.equals(settings.getTwoFactorEmailVerified());
        boolean emailEnabled = settings.isTwoFactorEnabled() && "EMAIL".equals(method) && emailReady;
        if (emailConfigured || "EMAIL".equals(method)) {
            result.add(TwoFactorMethodStatusResponse.builder()
                    .method("EMAIL")
                    .enabled(emailEnabled)
                    .readyToUse(emailReady)
                    .email(maskEmail(settings.getTwoFactorEmail()))
                    .enabledAt(emailEnabled ? settings.getTwoFactorEnabledAt() : null)
                    .emailVerificationPending(emailConfigured && !emailReady)
                    .backupCodesRemaining(0)
                    .build());
        }

        int backupCodesRemaining = backupCodeUtil.countUnusedCodes(backupCodeUtil.parseBackupCodes(settings.getTwoFactorBackupCodes()));
        if (backupCodesRemaining > 0) {
            result.add(TwoFactorMethodStatusResponse.builder()
                    .method("BACKUP_CODES")
                    .enabled(true)
                    .readyToUse(true)
                    .backupCodesRemaining(backupCodesRemaining)
                    .enabledAt(settings.getTwoFactorEnabledAt())
                    .emailVerificationPending(false)
                    .build());
        }

        return result;
    }

    @Override
    public void resendEmailMethodVerification(String userEmail) {
        User user = findUserByEmail(userEmail);
        UserSettings settings = getOrCreateSettings(user);

        String email = settings.getTwoFactorEmail();
        if (email == null || email.isBlank()) {
            email = user.getEmail();
            settings.setTwoFactorEmail(email);
            userSettingsRepository.save(settings);
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        sendTwoFactorLoginCode(email, code);
    }

    // -------------------------------------------------------------------------
    // Login challenge
    // -------------------------------------------------------------------------

    private JwtResponse completeLoginWithOptionalTwoFactor(User user, HttpServletRequest servletRequest) {
        UserSettings settings = getOrCreateSettings(user);

        if ("SOCIAL".equals(user.getAuthProvider()) || !settings.isTwoFactorEnabled()) {
            return sessionService.createTokenAndSession(user, servletRequest);
        }

        String method = defaultMethod(settings.getTwoFactorMethod());
        String challengeId = UUID.randomUUID().toString();

        Map<String, Object> challenge = new HashMap<>();
        challenge.put("userId", user.getId());
        challenge.put("email", user.getEmail());
        challenge.put("method", method);
        challenge.put("createdAt", System.currentTimeMillis());

        if ("EMAIL".equals(method)) {
            if (settings.getTwoFactorEmail() == null
                    || settings.getTwoFactorEmail().isBlank()
                    || !Boolean.TRUE.equals(settings.getTwoFactorEmailVerified())) {
                throw new BadRequestException("Two-factor email is not verified.");
            }

            String emailCode = String.format("%06d", new Random().nextInt(1_000_000));
            challenge.put("emailCodeHash", twoFactorService.hashCode(emailCode));
            sendTwoFactorLoginCode(settings.getTwoFactorEmail(), emailCode);
        }

        redisTemplate.opsForValue().set(
                TWO_FACTOR_CHALLENGE_PREFIX + challengeId,
                challenge,
                twoFactorChallengeTtlSeconds,
                TimeUnit.SECONDS
        );

        List<String> methods = new ArrayList<>();
        if ("EMAIL".equals(method)) {
            methods.add("EMAIL_OTP");
        } else {
            methods.add("TOTP");
        }

        int backupCodesLeft = backupCodeUtil.countUnusedCodes(backupCodeUtil.parseBackupCodes(settings.getTwoFactorBackupCodes()));
        if (backupCodesLeft > 0) {
            methods.add("BACKUP_CODE");
        }

        return JwtResponse.builder()
                .requiresTwoFactor(true)
                .challengeId(challengeId)
                .message("Two-factor verification required.")
                .twoFactorMethods(methods)
                .build();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> userSettingsRepository.save(UserSettings.builder().user(user).build()));
    }

    private String resolveEmailForTwoFactor(User user, String requestedEmail) {
        if (requestedEmail != null && !requestedEmail.isBlank()) {
            return requestedEmail.trim().toLowerCase(Locale.ROOT);
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("No email available for EMAIL 2FA method.");
        }
        return user.getEmail().trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTwoFactorMethod(String method) {
        if (method == null || method.isBlank()) {
            return "TOTP";
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        if ("EMAIL_OTP".equals(normalized)) {
            return "EMAIL";
        }
        if (!"TOTP".equals(normalized) && !"EMAIL".equals(normalized)) {
            throw new BadRequestException("Unsupported two-factor method: " + method);
        }
        return normalized;
    }

    private String normalizeTwoFactorMethodWithBackup(String method) {
        if (method == null || method.isBlank()) {
            throw new BadRequestException("Method is required.");
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        if ("EMAIL_OTP".equals(normalized)) {
            return "EMAIL";
        }
        if ("BACKUP_CODE".equals(normalized)) {
            return "BACKUP_CODES";
        }
        if (!"TOTP".equals(normalized) && !"EMAIL".equals(normalized) && !"BACKUP_CODES".equals(normalized)) {
            throw new BadRequestException("Unsupported two-factor method: " + method);
        }
        return normalized;
    }

    private String defaultMethod(String method) {
        return normalizeTwoFactorMethod(method);
    }

    private void clearTwoFactor(UserSettings settings, boolean clearEmailToo) {
        settings.setTwoFactorEnabled(false);
        settings.setTwoFactorSecret(null);
        settings.setTwoFactorTempSecret(null);
        settings.setTwoFactorMethod("TOTP");
        settings.setTwoFactorEnabledAt(null);
        if (clearEmailToo) {
            settings.setTwoFactorEmail(null);
            settings.setTwoFactorEmailVerified(false);
            settings.setTwoFactorEmailVerificationToken(null);
            settings.setTwoFactorBackupCodes(null);
        }
    }

    private boolean isValidTwoFactorProof(UserSettings settings, String otpCode, String backupCode) {
        boolean validByOtp = false;
        if (otpCode != null && !otpCode.isBlank()) {
            String method = defaultMethod(settings.getTwoFactorMethod());
            if ("TOTP".equals(method)) {
                validByOtp = settings.getTwoFactorSecret() != null
                        && twoFactorService.verifyCode(settings.getTwoFactorSecret(), otpCode, twoFactorVerifyWindow);
            }
        }

        boolean validByBackup = consumeBackupCode(settings, backupCode);
        return validByOtp || validByBackup;
    }

    private boolean consumeBackupCode(UserSettings settings, String backupCode) {
        if (backupCode == null || backupCode.isBlank()) {
            return false;
        }

        List<BackupCodeStatusDto> hashedCodes = backupCodeUtil.parseBackupCodes(settings.getTwoFactorBackupCodes());
        String incomingHash = twoFactorService.hashCode(backupCode);
        boolean consumed = backupCodeUtil.consumeBackupCode(hashedCodes, incomingHash);

        if (consumed) {
            settings.setTwoFactorBackupCodes(backupCodeUtil.serializeBackupCodes(hashedCodes));
            userSettingsRepository.save(settings);
        }

        return consumed;
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

    private String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private List<BackupCodeStatusDto> buildSafeBackupCodeStatus(List<BackupCodeStatusDto> storedCodes) {
        List<BackupCodeStatusDto> safeCodes = new ArrayList<>();
        for (int i = 0; i < storedCodes.size(); i++) {
            BackupCodeStatusDto code = storedCodes.get(i);
            safeCodes.add(BackupCodeStatusDto.builder()
                    .code("CODE-" + (i + 1))
                    .used(code.isUsed())
                    .usedAt(code.getUsedAt())
                    .build());
        }
        return safeCodes;
    }

    private void sendTwoFactorEmailVerification(String email, String verificationToken) {
        String subject = "MindRevol two-factor email verification";
        String content = "<h3>Verify your 2FA email</h3>"
                + "<p>Use this token to verify your email:</p>"
                + "<p><b>" + verificationToken + "</b></p>";

        asyncTaskProducer.submitEmailTask(EmailTask.builder()
                .toEmail(email)
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build());
    }

    private void sendTwoFactorLoginCode(String email, String code) {
        String subject = "MindRevol login verification code";
        String content = "<h3>Your verification code</h3>"
                + "<p>Enter this code to complete sign in:</p>"
                + "<p><b>" + code + "</b></p>"
                + "<p>This code expires in " + twoFactorChallengeTtlSeconds + " seconds.</p>";

        asyncTaskProducer.submitEmailTask(EmailTask.builder()
                .toEmail(email)
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build());
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String userEmail) {
        return userService.getMyProfile(userEmail);
    }
}
