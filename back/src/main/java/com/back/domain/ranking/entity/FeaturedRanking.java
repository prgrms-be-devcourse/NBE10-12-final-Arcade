package com.back.domain.ranking.entity;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_featured_ranking_target_type", columnList = "target_type"))
public class FeaturedRanking extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private long targetId;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private double score;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    public FeaturedRanking(TargetType targetType, long targetId, int rank, double score, LocalDateTime computedAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.rank = rank;
        this.score = score;
        this.computedAt = computedAt;
    }
}
