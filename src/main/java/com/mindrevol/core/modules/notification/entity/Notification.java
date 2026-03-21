package com.mindrevol.core.modules.notification.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    private String referenceId;

    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isSeen = false;

    @Column(nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private int actorsCount = 1;

    // --- BỔ SUNG SPRINT 2 ---

    // EPIC 3: Hỗ trợ đa ngôn ngữ (i18n)
    @Column(name = "message_key")
    private String messageKey;

    @Column(name = "message_args", columnDefinition = "TEXT")
    private String messageArgs; // Lưu JSON array, VD: '["Khang", "Đà Lạt"]'

    // EPIC 1: Trạng thái hành động (Chấp nhận/Từ chối)
    @Column(name = "action_status")
    private String actionStatus; // PENDING, ACCEPTED, REJECTED
}