package com.mindrevol.core.modules.checkin.dto.response;

import com.mindrevol.core.modules.checkin.entity.ActivityType;
import com.mindrevol.core.modules.checkin.entity.CheckinStatus;
import com.mindrevol.core.modules.checkin.entity.CheckinVisibility;
import com.mindrevol.core.modules.checkin.entity.MediaType; 
import com.mindrevol.core.modules.feed.dto.FeedItemResponse;
import com.mindrevol.core.modules.feed.dto.FeedItemType;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponse implements FeedItemResponse {

    private String id;
    private UserSummaryResponse user;
    private String journeyId;
    private String journeyName;

    // --- MEDIA ---
    private String imageUrl;      
    private String videoUrl;      
    private MediaType mediaType;  

    // --- CONTENT ---
    private String caption;
    private String emotion;
    private ActivityType activityType;
    private String activityName;
    private String locationName;
    private List<String> tags;

    // --- STATUS ---
    private CheckinStatus status;
    private CheckinVisibility visibility;
    private LocalDate checkinDate;
    private LocalDateTime createdAt;

    // --- INTERACTION ---
    private int reactionCount;
    private int commentCount;
    private List<CheckinReactionDetailResponse> latestReactions; 
    
    // [THÊM MỚI Ở ĐÂY] Cờ nhận biết bài viết này đã được user hiện tại lưu hay chưa
    private boolean isSaved;
    
    @Override
    public FeedItemType getType() {
        return FeedItemType.POST;
    }
}