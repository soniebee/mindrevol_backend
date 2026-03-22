package com.mindrevol.core.modules.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSessionsGroupedResponse {
    private UserSessionResponse currentSession;
    private List<UserSessionResponse> otherSessions;
}

