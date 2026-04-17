package com.mindrevol.core.modules.mood.service;

import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;

import java.util.List;

public interface MoodService {

    // 1. Đăng hoặc Cập nhật Mood (Ghi đè - Đã xử lý dọn dẹp cache Hibernate)
    MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request);

    // 2. Lấy danh sách Mood CÒN HẠN trong Box (Đã chặn người ngoài xem trộm)
    List<MoodResponse> getActiveMoodsInBox(String boxId, String userId);

    // 3. React vào Mood (Đã chặn người ngoài thả tim & không cho tự spam bão status của mình)
    void reactToMood(String boxId, String moodId, String userId, String emoji);

    // 4. 🔥 ULTIMATE (MỚI): Rút lại cảm xúc (Hủy thả tim)
    void removeReaction(String moodId, String userId);

    // 5. Tự xóa trạng thái của bản thân trước thời hạn (Hard Delete xóa thẳng tay cho nhẹ Database)
    void deleteMyMood(String boxId, String userId);
}