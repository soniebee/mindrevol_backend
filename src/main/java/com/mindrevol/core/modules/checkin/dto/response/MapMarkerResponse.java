package com.mindrevol.core.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MapMarkerResponse {
    private String checkinId;
    private Double latitude;
    private Double longitude;
    private String thumbnailUrl;
    private String userAvatar;
    private String fullname;
    private LocalDateTime createdAt; // THÊM TRƯỜNG NÀY ĐỂ SORT TIME-LAPSE
}