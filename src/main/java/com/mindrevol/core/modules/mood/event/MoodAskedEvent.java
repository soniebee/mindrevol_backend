package com.mindrevol.core.modules.mood.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodAskedEvent {
    private String boxId;
    private String askerId;
    private String targetUserId;
}