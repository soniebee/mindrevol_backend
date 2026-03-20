package com.mindrevol.core.modules.mood.service;

import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;

import java.util.List;

public interface MoodService {
    // 1. Đăng hoặc Cập nhật Mood
    MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request);

    // 2. Lấy danh sách Mood CÒN HẠN trong Box
    List<MoodResponse> getActiveMoodsInBox(String boxId);

    // 3. React vào Mood
    void reactToMood(String moodId, String userId, String emoji);
}