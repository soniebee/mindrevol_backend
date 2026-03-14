package com.mindrevol.core.modules.box.dto.response;

import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import lombok.*;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxDetailResponse {

    private String id; // Trở về String
    private String name;
    private String description;
    private String themeSlug;
    private String avatar;
    private long memberCount;
    private String myRole;

    private Object mapData;
    private Object moodBubbleData;

    private List<JourneyResponse> ongoingJourneys;
    private List<JourneyResponse> endedJourneys;
}