package com.mindrevol.core.modules.mood.service;

import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;

import java.util.List;

public interface MoodService {
    
    MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request);

    List<MoodResponse> getActiveMoodsInBox(String boxId, String userId);

    void reactToMood(String boxId, String moodId, String userId, String emoji);

    void removeReaction(String moodId, String userId);

    void deleteMyMood(String boxId, String userId);

    // 🔥 TÍNH NĂNG MỚI: Hỏi thăm/Chọc bạn bè
    void askFriendMood(String boxId, String askerId, String targetUserId);
}