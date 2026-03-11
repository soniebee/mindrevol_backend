//package com.mindrevol.core.modules.auth.service;
//
//import com.mindrevol.core.common.exception.BadRequestException;
//import com.mindrevol.core.common.exception.ResourceNotFoundException;
//import com.mindrevol.core.common.service.EmailService;
//import com.mindrevol.core.common.utils.JwtUtil;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep1Request;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep2Request;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep3Request;
//import com.mindrevol.core.modules.auth.dto.request.validation.CheckEmailRequest;
//import com.mindrevol.core.modules.auth.dto.request.validation.CheckHandleRequest;
//import com.mindrevol.core.modules.auth.dto.request.otp.VerifyOtpRequest;
//import com.mindrevol.core.modules.auth.dto.request.otp.ResendOtpRequest;
//import com.mindrevol.core.modules.auth.dto.response.AvailabilityResponse;
//import com.mindrevol.core.modules.auth.entity.RegisterTempData;
//import com.mindrevol.core.modules.auth.mapper.AuthMapper;
//import com.mindrevol.core.modules.auth.repository.RegisterTempDataRepository;
//import com.mindrevol.core.modules.user.entity.Gender;
//import com.mindrevol.core.modules.user.entity.Role;
//import com.mindrevol.core.modules.user.entity.User;
//import com.mindrevol.core.modules.user.repository.RoleRepository;
//import com.mindrevol.core.modules.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDate;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
///**
// * Unit Tests cho RegistrationServiceImpl
// *
// * Test coverage:
// * - Email & Handle availability check
// * - Multi-step registration flow
// * - OTP generation & validation
// * - Password validation
// * - Error handling
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("RegistrationService Tests")
//class RegistrationServiceImplTest {
//
//    @Mock
//    private RegisterTempDataRepository registerTempDataRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private RoleRepository roleRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//    private EmailService emailService;
//
//    @Mock
//    private JwtUtil jwtUtil;
//
//    @Mock
//    private AuthMapper authMapper;
//
//    @InjectMocks
//    private RegistrationServiceImpl registrationService;
//
//    private static final String TEST_EMAIL = "test@example.com";
//    private static final String TEST_PASSWORD = "SecurePass123!@#";
//    private static final String TEST_HANDLE = "testuser";
//    private static final String TEST_FULLNAME = "Test User";
//
//    @BeforeEach
//    void setUp() {
//        // Set config values
//        ReflectionTestUtils.setField(registrationService, "otpValidityMinutes", 10);
//        ReflectionTestUtils.setField(registrationService, "otpLength", 6);
//        ReflectionTestUtils.setField(registrationService, "tempDataTtlMinutes", 30);
//    }
//
//    // ==================== CHECK EMAIL TESTS ====================
//
//    @Test
//    @DisplayName("Should return available when email does not exist")
//    void checkEmail_emailAvailable() {
//        // Arrange
//        CheckEmailRequest request = CheckEmailRequest.builder().email(TEST_EMAIL).build();
//        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
//
//        // Act
//        AvailabilityResponse response = registrationService.checkEmail(request);
//
//        // Assert
//        assertTrue(response.isAvailable());
//        assertEquals("Email này khả dụng", response.getMessage());
//        verify(userRepository, times(1)).existsByEmail(TEST_EMAIL);
//    }
//
//    @Test
//    @DisplayName("Should return unavailable when email already exists")
//    void checkEmail_emailNotAvailable() {
//        // Arrange
//        CheckEmailRequest request = CheckEmailRequest.builder().email(TEST_EMAIL).build();
//        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);
//
//        // Act
//        AvailabilityResponse response = registrationService.checkEmail(request);
//
//        // Assert
//        assertFalse(response.isAvailable());
//        assertTrue(response.getMessage().contains("đã được đăng ký"));
//        verify(userRepository, times(1)).existsByEmail(TEST_EMAIL);
//    }
//
//    // ==================== CHECK HANDLE TESTS ====================
//
//    @Test
//    @DisplayName("Should return available when handle does not exist")
//    void checkHandle_handleAvailable() {
//        // Arrange
//        CheckHandleRequest request = CheckHandleRequest.builder().handle(TEST_HANDLE).build();
//        when(userRepository.existsByHandle(TEST_HANDLE)).thenReturn(false);
//
//        // Act
//        AvailabilityResponse response = registrationService.checkHandle(request);
//
//        // Assert
//        assertTrue(response.isAvailable());
//        assertEquals("Handle này khả dụng", response.getMessage());
//        verify(userRepository, times(1)).existsByHandle(TEST_HANDLE);
//    }
//
//    @Test
//    @DisplayName("Should return unavailable when handle already exists")
//    void checkHandle_handleNotAvailable() {
//        // Arrange
//        CheckHandleRequest request = CheckHandleRequest.builder().handle(TEST_HANDLE).build();
//        when(userRepository.existsByHandle(TEST_HANDLE)).thenReturn(true);
//
//        // Act
//        AvailabilityResponse response = registrationService.checkHandle(request);
//
//        // Assert
//        assertFalse(response.isAvailable());
//        assertTrue(response.getMessage().contains("đã được sử dụng"));
//        verify(userRepository, times(1)).existsByHandle(TEST_HANDLE);
//    }
//
//    // ==================== STEP 1 TESTS ====================
//
//    @Test
//    @DisplayName("Should save step 1 when password matches and email is available")
//    void saveRegistrationStep1_success() {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .confirmPassword(TEST_PASSWORD)
//                .build();
//
//        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        registrationService.saveRegistrationStep1(request);
//
//        // Assert
//        verify(userRepository, times(1)).existsByEmail(TEST_EMAIL);
//        verify(registerTempDataRepository, times(1)).save(any(RegisterTempData.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when passwords don't match")
//    void saveRegistrationStep1_passwordMismatch() {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .confirmPassword("DifferentPass123!")
//                .build();
//
//        // Act & Assert
//        assertThrows(BadRequestException.class, () -> registrationService.saveRegistrationStep1(request));
//        verify(registerTempDataRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when email already exists")
//    void saveRegistrationStep1_emailExists() {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .confirmPassword(TEST_PASSWORD)
//                .build();
//
//        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);
//
//        // Act & Assert
//        assertThrows(BadRequestException.class, () -> registrationService.saveRegistrationStep1(request));
//        verify(registerTempDataRepository, never()).save(any());
//    }
//
//    // ==================== STEP 2 TESTS ====================
//
//    @Test
//    @DisplayName("Should save step 2 when handle is available")
//    void saveRegistrationStep2_success() {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(userRepository.existsByHandle(TEST_HANDLE)).thenReturn(false);
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        registrationService.saveRegistrationStep2(TEST_EMAIL, request);
//
//        // Assert
//        verify(registerTempDataRepository, times(1)).findByEmail(TEST_EMAIL);
//        verify(userRepository, times(1)).existsByHandle(TEST_HANDLE);
//        verify(registerTempDataRepository, times(1)).save(any(RegisterTempData.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when temp data not found in step 2")
//    void saveRegistrationStep2_tempDataNotFound() {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class,
//            () -> registrationService.saveRegistrationStep2(TEST_EMAIL, request));
//        verify(registerTempDataRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when handle already exists in step 2")
//    void saveRegistrationStep2_handleExists() {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(userRepository.existsByHandle(TEST_HANDLE)).thenReturn(true);
//
//        // Act & Assert
//        assertThrows(BadRequestException.class,
//            () -> registrationService.saveRegistrationStep2(TEST_EMAIL, request));
//        verify(registerTempDataRepository, never()).save(any());
//    }
//
//    // ==================== OTP TESTS ====================
//
//    @Test
//    @DisplayName("Should generate and send OTP successfully")
//    void generateAndSendOtp_success() {
//        // Arrange
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        registrationService.generateAndSendOtp(TEST_EMAIL);
//
//        // Assert
//        verify(registerTempDataRepository, times(1)).findByEmail(TEST_EMAIL);
//        verify(emailService, times(1)).sendHtmlEmail(anyString(), anyString(), anyString());
//        verify(registerTempDataRepository, times(1)).save(any(RegisterTempData.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when temp data not found for OTP")
//    void generateAndSendOtp_tempDataNotFound() {
//        // Arrange
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class,
//            () -> registrationService.generateAndSendOtp(TEST_EMAIL));
//        verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when temp data incomplete for OTP")
//    void generateAndSendOtp_incompleteData() {
//        // Arrange
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                // Missing password, handle, or fullname
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//
//        // Act & Assert
//        assertThrows(BadRequestException.class,
//            () -> registrationService.generateAndSendOtp(TEST_EMAIL));
//        verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should verify OTP successfully")
//    void verifyOtp_success() {
//        // Arrange
//        String otpCode = "123456";
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .otpCode(otpCode)
//                .otpExpirationTime(System.currentTimeMillis() + 600000) // 10 minutes from now
//                .otpAttempts(0)
//                .build();
//
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp(otpCode)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        registrationService.verifyOtp(request);
//
//        // Assert
//        verify(registerTempDataRepository, times(1)).findByEmail(TEST_EMAIL);
//        verify(registerTempDataRepository, times(1)).save(any(RegisterTempData.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when OTP is expired")
//    void verifyOtp_expired() {
//        // Arrange
//        String otpCode = "123456";
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .otpCode(otpCode)
//                .otpExpirationTime(System.currentTimeMillis() - 1000) // Already expired
//                .otpAttempts(0)
//                .build();
//
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp(otpCode)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act & Assert
//        assertThrows(BadRequestException.class,
//            () -> registrationService.verifyOtp(request));
//        assertTrue(tempData.getOtpAttempts() == 0); // Attempts should be reset for expired OTP
//    }
//
//    @Test
//    @DisplayName("Should throw exception when OTP is incorrect")
//    void verifyOtp_incorrectOtp() {
//        // Arrange
//        String correctOtp = "123456";
//        String incorrectOtp = "654321";
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .otpCode(correctOtp)
//                .otpExpirationTime(System.currentTimeMillis() + 600000)
//                .otpAttempts(0)
//                .build();
//
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp(incorrectOtp)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act & Assert
//        assertThrows(BadRequestException.class,
//            () -> registrationService.verifyOtp(request));
//        verify(registerTempDataRepository, times(1)).save(any(RegisterTempData.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when OTP attempts exceeded")
//    void verifyOtp_attemptsExceeded() {
//        // Arrange
//        String otpCode = "123456";
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .otpCode(otpCode)
//                .otpExpirationTime(System.currentTimeMillis() + 600000)
//                .otpAttempts(3) // Already at max attempts
//                .build();
//
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp("wrong")
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//
//        // Act & Assert
//        assertThrows(BadRequestException.class,
//            () -> registrationService.verifyOtp(request));
//        verify(registerTempDataRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should resend OTP successfully")
//    void resendOtp_success() {
//        // Arrange
//        ResendOtpRequest request = ResendOtpRequest.builder().email(TEST_EMAIL).build();
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//        when(registerTempDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        registrationService.resendOtp(request);
//
//        // Assert
//        verify(registerTempDataRepository, times(1)).findByEmail(TEST_EMAIL);
//        verify(emailService, times(1)).sendHtmlEmail(anyString(), anyString(), anyString());
//    }
//
//    // ==================== GET REGISTRATION DATA TESTS ====================
//
//    @Test
//    @DisplayName("Should retrieve registration data successfully")
//    void getRegistrationData_success() {
//        // Arrange
//        RegisterTempData tempData = RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .build();
//
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(tempData));
//
//        // Act
//        RegisterTempData result = registrationService.getRegistrationData(TEST_EMAIL);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(TEST_EMAIL, result.getEmail());
//        verify(registerTempDataRepository, times(1)).findByEmail(TEST_EMAIL);
//    }
//
//    @Test
//    @DisplayName("Should throw exception when registration data not found")
//    void getRegistrationData_notFound() {
//        // Arrange
//        when(registerTempDataRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class,
//            () -> registrationService.getRegistrationData(TEST_EMAIL));
//    }
//}
//
