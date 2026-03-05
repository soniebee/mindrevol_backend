package com.mindrevol.core.modules.box.entity;

import com.mindrevol.core.common.enitty.BaseEntity; // Lưu ý package BaseEntity có typo 'enitty' theo code cũ của bạn
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boxes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
@SQLRestriction("deleted_at IS NULL")
public class Box extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    private String avatar; // Emoji hoặc link icon của Box

    @Column(name = "theme_slug", length = 50)
    private String themeSlug; // Ví dụ: "farm-theme", "cafe-theme"

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt; // Cập nhật khi có tin nhắn hoặc ảnh mới

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoxMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "box")
    @Builder.Default
    private List<com.mindrevol.core.modules.journey.entity.Journey> journeys = new ArrayList<>();
}