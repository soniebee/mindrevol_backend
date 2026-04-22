package com.mindrevol.core.modules.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinReactedEvent {
    private String checkinId;
    private String reactorId;
    private String checkinOwnerId;
    private String emoji;
}

