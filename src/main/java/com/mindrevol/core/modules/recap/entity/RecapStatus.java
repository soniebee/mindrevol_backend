package com.mindrevol.core.modules.recap.entity;

public enum RecapStatus {
    PENDING,     // Đang chờ trong hàng đợi
    PROCESSING,  // Đang được Worker / Cloudinary xử lý render video
    COMPLETED,   // Đã hoàn thành và có sẵn videoUrl
    FAILED       // Lỗi trong quá trình render
}