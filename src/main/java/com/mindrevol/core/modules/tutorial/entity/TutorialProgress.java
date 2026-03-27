package com.mindrevol.core.modules.tutorial.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tutorial_progress")
@Getter
@Setter
public class TutorialProgress extends BaseEntity {

    // Đổi kiểu dữ liệu từ Long sang String để khớp với User ID của hệ thống
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;
}