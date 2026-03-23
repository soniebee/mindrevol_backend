package com.mindrevol.core.modules.advertising.repository;

import com.mindrevol.core.modules.advertising.entity.SystemAd;
import com.mindrevol.core.modules.feed.dto.FeedItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemAdRepository extends JpaRepository<SystemAd, String> {

    // Lấy quảng cáo theo loại (ví dụ lấy INTERNAL_AD để chạy slot đầu)
    List<SystemAd> findByTypeAndIsActiveTrue(FeedItemType type);

    // Lấy tất cả quảng cáo Affiliate để thuật toán lọc
    @Query("SELECT a FROM SystemAd a WHERE a.type = 'AFFILIATE_AD' AND a.isActive = true")
    List<SystemAd> findAllActiveAffiliateAds();
}