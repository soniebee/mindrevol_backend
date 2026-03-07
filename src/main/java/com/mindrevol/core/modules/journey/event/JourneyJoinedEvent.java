package com.mindrevol.core.modules.journey.event;

import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JourneyJoinedEvent {
    private final Journey journey;
    private final User participant;
}