package com.back.domain.contest.contest.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Contest extends BaseEntity {

    @Column(name = "creator_member_id")
    private Long creatorMemberId;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestTag contestTag;

    private LocalDate applicationPeriodStart;

    private LocalDate applicationPeriodEnd;

    @Column(nullable = false)
    private String linkUrl;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int viewCount;

    private String imageUrl;

    public Contest(
        Long creatorMemberId,
        String name,
        String description,
        ContestFormat format,
        ContestTag contestTag,
        LocalDate applicationPeriodStart,
        LocalDate applicationPeriodEnd,
        String linkUrl,
        String imageUrl
    ) {
        this.creatorMemberId = creatorMemberId;
        this.name = name;
        this.description = description;
        this.format = format;
        this.contestTag = contestTag;
        this.applicationPeriodStart = applicationPeriodStart;
        this.applicationPeriodEnd = applicationPeriodEnd;
        this.linkUrl = linkUrl;
        this.imageUrl = imageUrl;
        this.likeCount = 0;
        this.viewCount = 0;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}
