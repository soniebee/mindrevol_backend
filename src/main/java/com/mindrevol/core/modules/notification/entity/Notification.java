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

    // [TASK-102] Tách biệt trạng thái Seen (Đã thấy notification trên chuông)
    @Column(nullable = false)
    @Builder.Default
    private boolean isSeen = false;

    // [TASK-101] Phục vụ việc gom nhóm (Ví dụ: A và 2 người khác...)
    @Column(nullable = false)
    @Builder.Default
    private int actorsCount = 1;
}