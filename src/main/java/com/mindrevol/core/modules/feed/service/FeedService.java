package com.mindrevol.core.modules.feed.service;

import com.mindrevol.core.modules.feed.dto.FeedItemResponse;
import java.util.List;

public interface FeedService {
    // Lấy feed (Cache Checkin + Live Ads)
    List<FeedItemResponse> getNewsFeed(String userId, int offset, int limit);

    // [THÊM MỚI] Lấy Grid Feed Hành trình (bản thân + bạn bè)
    List<FeedItemResponse> getJourneyGridFeed(String userId, int page, int limit);

    void evictFeedCache(String userId);
    
    List<FeedItemResponse> injectContextualAds(List<FeedItemResponse> posts, int offset, int limit);
}