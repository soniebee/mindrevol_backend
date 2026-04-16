package com.mindrevol.core.modules.user.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private String id;
    private String handle;
    private String fullname;
    private String avatarUrl;
    // --- THÊM 2 TRƯỜNG NÀY ---
    private boolean hasPassword; // True: Hiện form Pass | False: Hiện form OTP
    private String authProvider; // LOCAL, GOOGLE, FACEBOOK...
    // -------------------------
    private boolean isOnline;
    private LocalDateTime lastActiveAt;
    private String friendshipStatus; 
}