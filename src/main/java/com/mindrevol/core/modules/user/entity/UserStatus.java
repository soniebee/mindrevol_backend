package com.mindrevol.core.modules.user.entity;

public enum UserStatus {
    PENDING_ACTIVATION,  // Chờ kích hoạt (sau khi đăng ký)
    ACTIVE,              // Đang hoạt động
    SUSPENDED,           // Bị tạm ngưng
    BANNED,              // Bị cấm vĩnh viễn
    DEACTIVATED          // Tự vô hiệu hóa
}

