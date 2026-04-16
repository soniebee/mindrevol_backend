package com.mindrevol.core.modules.feed.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.advertising.entity.SystemAd;
import com.mindrevol.core.modules.advertising.repository.SystemAdRepository;
import com.mindrevol.core.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.feed.dto.AdFeedItemResponse;
import com.mindrevol.core.modules.feed.dto.FeedItemResponse;
import com.mindrevol.core.modules.feed.dto.FeedItemType;
import com.mindrevol.core.modules.feed.service.FeedService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserBlockRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final SystemAdRepository systemAdRepository;
    private final ObjectMapper objectMapper;

    private static final String FEED_CACHE_PREFIX = "feed:unified:";
    private static final long CACHE_TTL_MINUTES = 5;

    @Override
    @Transactional(readOnly = true)
    public List<FeedItemResponse> getNewsFeed(String userId, int offset, int limit) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CheckinResponse> posts = getCachedPosts(userId, offset, limit);
        List<FeedItemResponse> finalFeed = new ArrayList<>(posts);

        if (currentUser.isPremium()) {
            return finalFeed;
        }

        if (!posts.isEmpty()) {
            return injectContextualAds(finalFeed, offset, limit);
        }

        return finalFeed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedItemResponse> getJourneyGridFeed(String userId, int page, int limit) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, limit);
        
        Page<Checkin> checkins = checkinRepository.findJourneyGridFeed(userId, pageable);
        
        List<FeedItemResponse> posts = checkins.getContent().stream()
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());

        // Bỏ qua quảng cáo nếu là Premium
        if (currentUser.isPremium()) {
            return posts;
        }

        // Trộn quảng cáo vào feed grid
        if (!posts.isEmpty()) {
            int offset = page * limit;
            return injectContextualAds(posts, offset, limit);
        }

        return posts;
    }

    private List<CheckinResponse> getCachedPosts(String userId, int offset, int limit) {
        int page = (limit > 0) ? (offset / limit) : 0;
        String cacheKey = FEED_CACHE_PREFIX + userId + ":" + page;

        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.convertValue(cachedData, new TypeReference<List<CheckinResponse>>() {});
            }
        } catch (Exception e) {
            log.error("Redis read error: {}", e.getMessage());
        }

        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.add("00000000-0000-0000-0000-000000000000");

        Pageable pageable = PageRequest.of(page, limit);
        LocalDateTime cursor = LocalDateTime.now().plusSeconds(10);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Checkin> dbCheckins = checkinRepository.findUnifiedFeedRecent(
                userId,
                thirtyDaysAgo, 
                cursor,
                blockedIds,
                pageable
        );

        List<CheckinResponse> responseList = dbCheckins.stream().map(checkinMapper::toResponse).collect(Collectors.toList());

        try {
            if (!responseList.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, responseList, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("Redis write error: {}", e.getMessage());
        }

        return responseList;
    }

    @Override
    public List<FeedItemResponse> injectContextualAds(List<FeedItemResponse> posts, int offset, int limit) {
        // Lấy tất cả quảng cáo (Internal & Affiliate) đang active
        List<SystemAd> internalAds = systemAdRepository.findByTypeAndIsActiveTrue(FeedItemType.INTERNAL_AD);
        List<SystemAd> affiliateAds = systemAdRepository.findAllActiveAffiliateAds();

        List<SystemAd> allAds = new ArrayList<>();
        allAds.addAll(internalAds);
        allAds.addAll(affiliateAds);

        System.out.println("======== DEBUG QUẢNG CÁO ========");
        System.out.println("1. Lấy từ DB -> Internal: " + internalAds.size() + " | Affiliate: " + affiliateAds.size());
        System.out.println("2. Số lượng bài Post gốc: " + posts.size());

        // Nếu không có quảng cáo nào trong DB, trả về Feed gốc
        if (allAds.isEmpty()) {
            System.out.println("   -> Không có quảng cáo trong DB, bỏ qua chèn.");
            return posts;
        }

        List<FeedItemResponse> mixedFeed = new ArrayList<>();
        int globalIndex = offset;
        
        // Khởi tạo bộ đếm và Random mục tiêu chèn quảng cáo đầu tiên (Sau 3 đến 5 bài)
        int postsSinceLastAd = 0;
        int nextAdTarget = ThreadLocalRandom.current().nextInt(3, 6);

        for (FeedItemResponse item : posts) {
            mixedFeed.add(item);
            globalIndex++;
            postsSinceLastAd++;

            // Điều kiện: Luôn bỏ qua 2 bài đầu tiên (globalIndex > 2) và Đã đạt số bài viết mục tiêu (postsSinceLastAd >= nextAdTarget)
            if (globalIndex > 2 && postsSinceLastAd >= nextAdTarget) {
                // Lấy ngẫu nhiên 1 quảng cáo từ tổng danh sách
                SystemAd randomAd = allAds.get(ThreadLocalRandom.current().nextInt(allAds.size()));
                mixedFeed.add(mapAdToResponse(randomAd));
                
                System.out.println("   -> Đã chèn 1 " + randomAd.getType() + " vào vị trí bài viết thứ: " + globalIndex);
                
                // Reset bộ đếm về 0
                postsSinceLastAd = 0;
                
                // Random lại khoảng cách cho lần chèn tiếp theo (Giãn cách xa hơn: từ 4 đến 8 bài viết)
                nextAdTarget = ThreadLocalRandom.current().nextInt(4, 9);
            }
        }
        
        System.out.println("3. TỔNG SỐ ITEM SAU KHI TRỘN: " + mixedFeed.size());
        System.out.println("=================================");
        return mixedFeed;
    }

    private AdFeedItemResponse mapAdToResponse(SystemAd ad) {
        return AdFeedItemResponse.builder()
                .type(ad.getType())
                .id(ad.getId())
                .adProvider("MINDREVOL")
                .title(ad.getTitle())
                .imageUrl(ad.getImageUrl())
                .ctaLink(ad.getCtaLink())
                .ctaText(ad.getCtaText())
                .build();
    }

    @Override
    public void evictFeedCache(String userId) {
        try {
            String pattern = FEED_CACHE_PREFIX + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Error evicting cache: {}", e.getMessage());
        }
    }
}