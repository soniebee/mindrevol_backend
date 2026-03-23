package com.mindrevol.core.modules.checkin.service;

import com.mindrevol.core.modules.checkin.dto.response.CheckinReactionDetailResponse;
import java.util.List;

public interface ReactionService {
    // [UUID] Long -> String
    void toggleReaction(String checkinId, String userId, String emoji, String mediaUrl);
    List<CheckinReactionDetailResponse> getReactions(String checkinId);
    List<CheckinReactionDetailResponse> getPreviewReactions(String checkinId);
}