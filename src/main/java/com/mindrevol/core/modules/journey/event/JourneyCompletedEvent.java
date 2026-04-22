package com.mindrevol.core.modules.journey.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JourneyCompletedEvent extends ApplicationEvent {
    private final String journeyId; // Sửa Long -> String

    public JourneyCompletedEvent(String journeyId) {
        super(journeyId);
        this.journeyId = journeyId;
    }
}