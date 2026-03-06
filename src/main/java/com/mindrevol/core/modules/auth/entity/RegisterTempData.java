package com.mindrevol.core.modules.auth.entity;

import com.mindrevol.core.modules.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Entity lưu dữ liệu đăng ký tạm thời vào Redis
 * TTL (Time-to-Live): 30 phút (có thể cấu hình)
 *
 * Mục đích: Lưu dữ liệu qua các bước của Wizard mà chưa chính thức tạo User
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("register_temp_data")
public class RegisterTempData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Key là email (dùng làm ID duy nhất)
    private String id; // Email

    // === STEP 1 ===
    private String email;
    private String password;

    // === STEP 2 ===
    private String handle;
    private String fullname;

    // === STEP 3 ===
    private LocalDate dateOfBirth;
    private Gender gender;

    // === OTP VERIFICATION ===
    private String otpCode;           // Mã OTP được gửi
    private Long otpExpirationTime;    // Thời gian hết hạn OTP (millis)
    private Integer otpAttempts;       // Số lần nhập sai (mặc định 0)
    private static final int MAX_OTP_ATTEMPTS = 3;

    // === METADATA ===
    private Long createdAtMillis;      // Thời gian tạo
    private Long updatedAtMillis;      // Thời gian cập nhật

    // TTL tự động xóa record sau 30 phút nếu chưa hoàn thành đăng ký
    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttl;

    /**
     * Khởi tạo TTL = 30 phút (1800 giây)
     */
    public void initializeTtl() {
        this.ttl = 1800L; // 30 phút
    }

    /**
     * Tăng số lần nhập sai OTP
     */
    public void incrementOtpAttempts() {
        if (this.otpAttempts == null) {
            this.otpAttempts = 1;
        } else {
            this.otpAttempts++;
        }
    }

    /**
     * Kiểm tra xem OTP còn hiệu lực không
     */
    public boolean isOtpExpired() {
        if (this.otpExpirationTime == null) {
            return true;
        }
        return System.currentTimeMillis() > this.otpExpirationTime;
    }

    /**
     * Kiểm tra xem đã vượt quá số lần nhập sai OTP không
     */
    public boolean isOtpAttemptsExceeded() {
        return this.otpAttempts != null && this.otpAttempts >= MAX_OTP_ATTEMPTS;
    }

    /**
     * Reset OTP attempts
     */
    public void resetOtpAttempts() {
        this.otpAttempts = 0;
    }
}

