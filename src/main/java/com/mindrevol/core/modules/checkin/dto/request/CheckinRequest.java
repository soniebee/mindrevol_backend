package com.mindrevol.core.modules.checkin.dto.request;

import com.mindrevol.core.modules.checkin.entity.ActivityType;
import com.mindrevol.core.modules.checkin.entity.CheckinStatus;
import com.mindrevol.core.modules.checkin.entity.CheckinVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class CheckinRequest {

    private String journeyId;

    @NotNull(message = "Ảnh check-in là bắt buộc")
    private MultipartFile file;

    private String caption;
    
    private String emotion;                 
    private ActivityType activityType;      
    private String activityName;            
    private String locationName;                 

    // --- [MỚI] THẺ HIỂN THỊ VÀ SPOTIFY ---
    private String displayTag;
    private String spotifyTrackId;

    private Double latitude;
    private Double longitude;
    private List<String> tags;                   
    private CheckinStatus statusRequest = CheckinStatus.NORMAL; 
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;
}