package com.mindrevol.core.modules.advertising.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.feed.dto.FeedItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_ads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAd extends BaseEntity {

    @Column(nullable = false)
    private String title;

    // SỬA: Chuyển sang TEXT để chứa link ảnh dài
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    // SỬA: Chuyển sang TEXT để chứa link shopee, affiliate dài
    @Column(name = "cta_link", nullable = false, columnDefinition = "TEXT")
    private String ctaLink;

    @Column(name = "cta_text", nullable = false)
    private String ctaText;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private int priority = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @Builder.Default
    private FeedItemType type = FeedItemType.INTERNAL_AD;

    @Column(name = "target_tags")
    private String targetTags;
}