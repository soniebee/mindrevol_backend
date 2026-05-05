package com.mindrevol.core.modules.mood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodResponse {
    private String id;
    private String boxId;
    private String userId;
    private String fullname;
    private String avatarUrl;

    private String icon; 
    private String message;
    private String spotifyTrackId;
    
    private String activity;
    private String location;
    private String weather;

    private LocalDateTime updatedAt; 
    private LocalDateTime expiresAt; 

    private List<MoodReactionResponse> reactions;
}