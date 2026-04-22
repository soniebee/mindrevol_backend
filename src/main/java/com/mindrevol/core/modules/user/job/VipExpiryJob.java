package com.mindrevol.core.modules.user.job;

import com.mindrevol.core.modules.user.entity.AccountType;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VipExpiryJob {

    private final UserRepository userRepository;

    // Chạy vào lúc 00:01 mỗi ngày
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void downgradeExpiredVipUsers() {
        log.info("Bắt đầu chạy Job kiểm tra và hạ cấp các tài khoản VIP hết hạn...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Quét các user đang là GOLD nhưng ngày hết hạn đã qua
        int updatedCount = userRepository.downgradeExpiredUsers(AccountType.FREE, AccountType.GOLD, now);
        
        log.info("Đã hạ cấp thành công {} tài khoản hết hạn VIP về mức FREE.", updatedCount);
    }
}