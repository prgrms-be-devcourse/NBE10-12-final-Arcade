package com.back.domain.party.showcase.ranking.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        uniqueConstraints = @UniqueConstraint(columnNames = {"showcase_id", "snapshot_date"}),
        indexes = @Index(name = "idx_showcase_view_snapshot_date", columnList = "snapshot_date")
)
public class ShowcaseViewSnapshot extends BaseEntity {

    @Column(name = "showcase_id", nullable = false)
    private long showcaseId;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    public ShowcaseViewSnapshot(long showcaseId, int viewCount, LocalDate snapshotDate) {
        this.showcaseId = showcaseId;
        this.viewCount = viewCount;
        this.snapshotDate = snapshotDate;
    }
}
