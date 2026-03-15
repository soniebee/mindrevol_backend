package com.mindrevol.core.modules.checkin.service;

import com.mindrevol.core.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.core.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.core.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.core.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinService {

    CheckinResponse createCheckin(CheckinRequest request, User currentUser);

    // [SỬA LẠI]: Thêm tham số String chapterId để đồng bộ với CheckinServiceImpl
    Page<CheckinResponse> getJourneyFeed(String journeyId, String chapterId, Pageable pageable, User currentUser);

    CommentResponse postComment(String checkinId, String content, User currentUser);

    Page<CommentResponse> getComments(String checkinId, Pageable pageable);

    List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit);

    // [SỬA LẠI]: Thêm tham số String chapterId để đồng bộ với CheckinServiceImpl
    List<CheckinResponse> getJourneyFeedByCursor(String journeyId, String chapterId, User currentUser, LocalDateTime cursor, int limit);

    CheckinResponse updateCheckin(String checkinId, String caption, User currentUser);

    void deleteCheckin(String checkinId, User currentUser);
}