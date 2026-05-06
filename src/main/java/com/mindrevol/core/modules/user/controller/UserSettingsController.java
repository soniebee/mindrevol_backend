package com.mindrevol.core.modules.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "Cài đặt người dùng (Thông báo, Privacy...)")
public class UserSettingsController {

    // TOÀN BỘ CÁC ENDPOINT LIÊN QUAN ĐẾN CÀI ĐẶT THÔNG BÁO VÀ NGƯỜI DÙNG 
    // ĐÃ ĐƯỢC CHUYỂN VÀ GỘP CHUNG VÀO: UserController.java 
    // nhằm xử lý triệt để lỗi "Ambiguous handler methods mapped".
    
}