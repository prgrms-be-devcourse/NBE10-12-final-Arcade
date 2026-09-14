package com.back.domain.ranking.entity;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"target_type", "target_id", "snapshot_date"}),
        indexes = @Index(name = "idx_view_snapshot_date", columnList = "snapshot_date")
)
public class ViewSnapshot extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private long targetId;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    public ViewSnapshot(TargetType targetType, long targetId, int viewCount, LocalDate snapshotDate) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.viewCount = viewCount;
        this.snapshotDate = snapshotDate;
    }
}
