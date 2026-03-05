package com.mindrevol.core.modules.checkin.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "checkin_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CheckinComment extends BaseEntity {
}
