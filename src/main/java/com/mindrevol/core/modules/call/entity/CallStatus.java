package com.mindrevol.core.modules.call.entity;

public enum CallStatus {
    RINGING,      // Đang đổ chuông
    IN_PROGRESS,  // Đang gọi
    ENDED,        // Kết thúc bình thường
    REJECTED,     // Bị từ chối
    MISSED,       // Bỏ lỡ (Không nghe máy)
    BUSY          // Đang trong cuộc gọi khác
}