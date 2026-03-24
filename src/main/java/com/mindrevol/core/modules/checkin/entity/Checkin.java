package com.mindrevol.core.modules.checkin.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checkins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_checkin_user_journey_date", columnNames = {"user_id", "journey_id", "checkin_date"})
        },
        indexes = {
                @Index(name = "idx_checkin_journey", columnList = "journey_id"),
                @Index(name = "idx_checkin_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Checkin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "image_url")
    private String imageUrl;

    // [THÊM MỚI] Lưu ID của file trên ImageKit để phục vụ xóa
    @Column(name = "image_file_id")
    private String imageFileId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // THÊM: Lưu ID của Chapter nếu tính năng Chapter được bật
    @Column(name = "chapter_id")
    private String chapterId;

    // THÊM: Thời gian hết hạn (Dùng cho bài đăng UNSORTED 14 ngày)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    // --- CÁC TRƯỜNG CONTEXT/PLATFORM (GIỮ NGUYÊN) ---

    @Column(length = 50)
    private String emotion; // Ví dụ: "🔥", "🌿", "CHILL"

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type")
    @Builder.Default
    private ActivityType activityType = ActivityType.DEFAULT;

    @Column(name = "activity_name")
    private String activityName;

    @Column(name = "location_name")
    private String locationName;
    
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ElementCollection
    @CollectionTable(name = "checkin_tags", joinColumns = @JoinColumn(name = "checkin_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "video_url")
    private String videoUrl;

    // ----------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinReaction> reactions = new ArrayList<>();

    public void prePersist() {
        if (this.checkinDate == null) this.checkinDate = LocalDate.now();
        if (this.tags == null) this.tags = new ArrayList<>();
    }
}

