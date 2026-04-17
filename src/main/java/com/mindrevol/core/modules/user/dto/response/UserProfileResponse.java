package com.mindrevol.core.modules.user.dto.response;

import com.mindrevol.core.modules.user.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    
    private String id; // [UUID] String
    private String email;
    private String handle;
    private String fullname;
    private String avatarUrl;
    private String bio;
    private String website;
    private OffsetDateTime joinedAt;
    private String status;
    private Set<String> roles;
    private long followerCount; 
    private long followingCount; 
    private boolean isFollowedByCurrentUser; 
    private long friendCount; 

    // --- CÁC TRƯỜNG CHO TÍNH NĂNG XEM PROFILE ---
    private FriendshipStatus friendshipStatus; 
    private boolean isBlockedByMe;   
    private boolean isBlockedByThem; 
    private boolean isMe;            
    private Long totalCheckins;
    private Integer currentStreak;

    // [THÊM MỚI] Gửi accountType (FREE/GOLD) xuống cho Frontend
    private String accountType; 
}