package com.mindrevol.core.modules.user.entity;

import com.mindrevol.core.common.entity.BaseEntity; // Sử dụng đúng đường dẫn có typo 'enitty' của bạn
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    private String fullname;

    private String avatarUrl;
}