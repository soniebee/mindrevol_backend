package com.mindrevol.core.modules.tutorial.service;

import com.mindrevol.core.modules.tutorial.dto.TutorialStatusResponse;

public interface TutorialService {
    TutorialStatusResponse getTutorialStatus();
    TutorialStatusResponse markAsCompleted();
}