package com.mindrevol.core.modules.journey.entity;

import com.mindrevol.core.common.enitty.BaseEntity;
import com.mindrevol.core.modules.box.entity.Box;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "journeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE journeys SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")


public class Journey extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private Box box;
}
