package com.mindrevol.core.modules.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Dữ liệu đăng ký tạm thời lưu trong Redis
 * Key: "register:temp:{email}"
 * TTL: 15 phút
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTempData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;

    private String handle;

    private String fullname;

    private String password; // Đã được mã hóa bởi BCrypt

    private String dateOfBirth; // Format: YYYY-MM-DD

    private String gender; // MALE, FEMALE, OTHER

    private String timezone;

    private String otpCode; // Mã OTP 6 số

    private int otpAttempts; // Số lần nhập sai OTP

    private long createdAt; // Timestamp tạo

    private long expiryTime; // Timestamp hết hạn (for in-memory storage)
}

