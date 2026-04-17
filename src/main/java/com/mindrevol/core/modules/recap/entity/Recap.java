package com.mindrevol.core.modules.recap.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Recap extends BaseEntity {

    // ❌ ĐÃ BỎ: private Long id; (Vì BaseEntity đã có trường String id)

    @Column(name = "user_id", nullable = false)
    private String userId; // Đổi thành String

    @Column(name = "journey_id")
    private String journeyId; // Đổi thành String

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecapStatus status = RecapStatus.PENDING;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "bg_music_url", length = 1000)
    private String bgMusicUrl;
}