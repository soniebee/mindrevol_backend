package com.mindrevol.core.modules.auth.dto.request;

import com.mindrevol.core.modules.user.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name must not be blank")
    private String fullname;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Handle (username) must not be blank")
    @Size(min = 3, max = 30, message = "Handle must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle can only contain letters, numbers, dots, and underscores")
    private String handle;

    // --- MỚI: Thêm ngày sinh để kiểm tra độ tuổi (Luật COPPA/GDPR) ---
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    // --- MỚI: Bắt buộc đồng ý điều khoản ---
    @AssertTrue(message = "You must agree to the Terms of Use and Privacy Policy")
    private boolean agreedToTerms;
    
    @NotNull(message = "Gender is required")
    private Gender gender;
}