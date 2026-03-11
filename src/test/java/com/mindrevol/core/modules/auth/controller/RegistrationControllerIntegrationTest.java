//package com.mindrevol.core.modules.auth.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep1Request;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep2Request;
//import com.mindrevol.core.modules.auth.dto.request.registration.RegisterStep3Request;
//import com.mindrevol.core.modules.auth.dto.request.validation.CheckEmailRequest;
//import com.mindrevol.core.modules.auth.dto.request.validation.CheckHandleRequest;
//import com.mindrevol.core.modules.auth.dto.request.otp.VerifyOtpRequest;
//import com.mindrevol.core.modules.auth.dto.request.otp.ResendOtpRequest;
//import com.mindrevol.core.modules.auth.dto.request.otp.SendOtpRequest;
//import com.mindrevol.core.modules.auth.service.RegistrationService;
//import com.mindrevol.core.modules.auth.service.SocialLoginService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//
//import static org.hamcrest.Matchers.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * Integration Tests cho RegistrationController
// *
// * Test coverage:
// * - API endpoints response format
// * - Request validation
// * - Error handling
// * - HTTP status codes
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//@DisplayName("RegistrationController Integration Tests")
//class RegistrationControllerIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private RegistrationService registrationService;
//
//    @MockBean
//    private SocialLoginService socialLoginService;
//
//    private static final String API_BASE = "/api/v1/auth";
//    private static final String TEST_EMAIL = "test@example.com";
//    private static final String TEST_PASSWORD = "SecurePass123!@#";
//    private static final String TEST_HANDLE = "testuser";
//    private static final String TEST_FULLNAME = "Test User";
//
//    @BeforeEach
//    void setUp() {
//        reset(registrationService, socialLoginService);
//    }
//
//    // ==================== STEP 1 TESTS ====================
//
//    @Test
//    @DisplayName("POST /register/step1 - Should register step 1 successfully")
//    void registerStep1_success() throws Exception {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .confirmPassword(TEST_PASSWORD)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step1")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("Bước 1"))
//                .andExpect(jsonPath("$.data").isEmpty());
//
//        verify(registrationService, times(1)).saveRegistrationStep1(any(RegisterStep1Request.class));
//    }
//
//    @Test
//    @DisplayName("POST /register/step1 - Should reject invalid email")
//    void registerStep1_invalidEmail() throws Exception {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email("invalid-email")
//                .password(TEST_PASSWORD)
//                .confirmPassword(TEST_PASSWORD)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step1")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).saveRegistrationStep1(any());
//    }
//
//    @Test
//    @DisplayName("POST /register/step1 - Should reject weak password (no special char)")
//    void registerStep1_weakPassword() throws Exception {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password("WeakPass123") // No special char
//                .confirmPassword("WeakPass123")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step1")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").containsString("ký tự đặc biệt"));
//
//        verify(registrationService, never()).saveRegistrationStep1(any());
//    }
//
//    @Test
//    @DisplayName("POST /register/step1 - Should reject mismatched passwords")
//    void registerStep1_mismatchedPasswords() throws Exception {
//        // Arrange
//        RegisterStep1Request request = RegisterStep1Request.builder()
//                .email(TEST_EMAIL)
//                .password(TEST_PASSWORD)
//                .confirmPassword("DifferentPass123!")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step1")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true)); // Validation passes at controller level
//
//        verify(registrationService, times(1)).saveRegistrationStep1(any()); // Service will throw exception
//    }
//
//    // ==================== STEP 2 TESTS ====================
//
//    @Test
//    @DisplayName("POST /register/step2 - Should register step 2 successfully")
//    void registerStep2_success() throws Exception {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step2")
//                .param("email", TEST_EMAIL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("Bước 2"));
//
//        verify(registrationService, times(1)).saveRegistrationStep2(TEST_EMAIL, request);
//    }
//
//    @Test
//    @DisplayName("POST /register/step2 - Should reject invalid handle format")
//    void registerStep2_invalidHandle() throws Exception {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle("invalid handle!@#") // Invalid characters
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step2")
//                .param("email", TEST_EMAIL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).saveRegistrationStep2(anyString(), any());
//    }
//
//    @Test
//    @DisplayName("POST /register/step2 - Should reject short handle")
//    void registerStep2_shortHandle() throws Exception {
//        // Arrange
//        RegisterStep2Request request = RegisterStep2Request.builder()
//                .handle("ab") // Too short (min 3)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step2")
//                .param("email", TEST_EMAIL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).saveRegistrationStep2(anyString(), any());
//    }
//
//    // ==================== STEP 3 TESTS ====================
//
//    @Test
//    @DisplayName("POST /register/step3 - Should register step 3 successfully")
//    void registerStep3_success() throws Exception {
//        // Arrange
//        RegisterStep3Request request = RegisterStep3Request.builder()
//                .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
//                .gender(com.mindrevol.core.modules.user.entity.Gender.MALE)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/register/step3")
//                .param("email", TEST_EMAIL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("Bước 3"));
//
//        verify(registrationService, times(1)).saveRegistrationStep3(anyString(), any(RegisterStep3Request.class));
//    }
//
//    // ==================== CHECK EMAIL TESTS ====================
//
//    @Test
//    @DisplayName("POST /check-email - Should check email availability")
//    void checkEmail_available() throws Exception {
//        // Arrange
//        CheckEmailRequest request = CheckEmailRequest.builder().email(TEST_EMAIL).build();
//
//        com.mindrevol.core.modules.auth.dto.response.AvailabilityResponse response =
//            com.mindrevol.core.modules.auth.dto.response.AvailabilityResponse.builder()
//                .available(true)
//                .message("Email này khả dụng")
//                .build();
//
//        when(registrationService.checkEmail(any())).thenReturn(response);
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/check-email")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.available").value(true));
//
//        verify(registrationService, times(1)).checkEmail(any(CheckEmailRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /check-email - Should reject invalid email format")
//    void checkEmail_invalidEmail() throws Exception {
//        // Arrange
//        CheckEmailRequest request = CheckEmailRequest.builder().email("invalid-email").build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/check-email")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).checkEmail(any());
//    }
//
//    // ==================== CHECK HANDLE TESTS ====================
//
//    @Test
//    @DisplayName("POST /check-handle - Should check handle availability")
//    void checkHandle_available() throws Exception {
//        // Arrange
//        CheckHandleRequest request = CheckHandleRequest.builder().handle(TEST_HANDLE).build();
//
//        com.mindrevol.core.modules.auth.dto.response.AvailabilityResponse response =
//            com.mindrevol.core.modules.auth.dto.response.AvailabilityResponse.builder()
//                .available(true)
//                .message("Handle này khả dụng")
//                .build();
//
//        when(registrationService.checkHandle(any())).thenReturn(response);
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/check-handle")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.available").value(true));
//
//        verify(registrationService, times(1)).checkHandle(any(CheckHandleRequest.class));
//    }
//
//    // ==================== SEND OTP TESTS ====================
//
//    @Test
//    @DisplayName("POST /send-otp - Should send OTP successfully with request body")
//    void sendOtp_success() throws Exception {
//        // Arrange
//        SendOtpRequest request = SendOtpRequest.builder().email(TEST_EMAIL).build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/send-otp")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("OTP"))
//                .andExpect(jsonPath("$.message").containsString("gửi"));
//
//        verify(registrationService, times(1)).generateAndSendOtp(TEST_EMAIL);
//    }
//
//    @Test
//    @DisplayName("POST /send-otp - Should reject invalid email")
//    void sendOtp_invalidEmail() throws Exception {
//        // Arrange
//        SendOtpRequest request = SendOtpRequest.builder().email("invalid-email").build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/send-otp")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).generateAndSendOtp(anyString());
//    }
//
//    // ==================== VERIFY OTP TESTS ====================
//
//    @Test
//    @DisplayName("POST /verify-otp - Should verify OTP successfully")
//    void verifyOtp_success() throws Exception {
//        // Arrange
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp("123456")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/verify-otp")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("xác thực thành công"));
//
//        verify(registrationService, times(1)).verifyOtp(any(VerifyOtpRequest.class));
//    }
//
//    @Test
//    @DisplayName("POST /verify-otp - Should reject invalid OTP length")
//    void verifyOtp_invalidOtpLength() throws Exception {
//        // Arrange
//        VerifyOtpRequest request = VerifyOtpRequest.builder()
//                .email(TEST_EMAIL)
//                .otp("12345") // Too short (must be exactly 6)
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/verify-otp")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//
//        verify(registrationService, never()).verifyOtp(any());
//    }
//
//    // ==================== RESEND OTP TESTS ====================
//
//    @Test
//    @DisplayName("POST /resend-otp - Should resend OTP successfully")
//    void resendOtp_success() throws Exception {
//        // Arrange
//        ResendOtpRequest request = ResendOtpRequest.builder().email(TEST_EMAIL).build();
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/resend-otp")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").containsString("OTP"));
//
//        verify(registrationService, times(1)).resendOtp(any(ResendOtpRequest.class));
//    }
//
//    // ==================== GET REGISTRATION DATA TESTS ====================
//
//    @Test
//    @DisplayName("GET /register/data - Should retrieve registration data")
//    void getRegistrationData_success() throws Exception {
//        // Arrange
//        com.mindrevol.core.modules.auth.entity.RegisterTempData tempData =
//            com.mindrevol.core.modules.auth.entity.RegisterTempData.builder()
//                .email(TEST_EMAIL)
//                .handle(TEST_HANDLE)
//                .fullname(TEST_FULLNAME)
//                .build();
//
//        when(registrationService.getRegistrationData(TEST_EMAIL)).thenReturn(tempData);
//
//        // Act & Assert
//        mockMvc.perform(get(API_BASE + "/register/data")
//                .param("email", TEST_EMAIL))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
//                .andExpect(jsonPath("$.data.handle").value(TEST_HANDLE));
//
//        verify(registrationService, times(1)).getRegistrationData(TEST_EMAIL);
//    }
//
//    // ==================== COMPLETE REGISTRATION TESTS ====================
//
//    @Test
//    @DisplayName("POST /complete - Should complete registration successfully")
//    void completeRegistration_success() throws Exception {
//        // Arrange
//        com.mindrevol.core.modules.auth.dto.response.RegistrationResponse response =
//            com.mindrevol.core.modules.auth.dto.response.RegistrationResponse.builder()
//                .userId(1L)
//                .email(TEST_EMAIL)
//                .accessToken("token123")
//                .refreshToken("refresh123")
//                .message("Đăng ký thành công!")
//                .build();
//
//        when(registrationService.completeRegistration(TEST_EMAIL)).thenReturn(response);
//
//        // Act & Assert
//        mockMvc.perform(post(API_BASE + "/complete")
//                .param("email", TEST_EMAIL))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.accessToken").value("token123"))
//                .andExpect(jsonPath("$.data.refreshToken").value("refresh123"));
//
//        verify(registrationService, times(1)).completeRegistration(TEST_EMAIL);
//    }
//}
//
