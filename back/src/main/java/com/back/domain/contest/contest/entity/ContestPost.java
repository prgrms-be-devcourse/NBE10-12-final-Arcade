package com.back.domain.contest.contest.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ContestPost extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false, unique = true)
    private Contest contest;

    @Lob
    private String description;

    @Column(nullable = false)
    private String linkUrl;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int viewCount;

    private String imageUrl;

    public ContestPost(
            Contest contest,
            String description,
            String linkUrl,
            String imageUrl
    ) {
        this.contest = contest;
        this.description = description;
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
    public void modify(String description, String linkUrl, String imageUrl) {
        this.description = description;
        this.linkUrl = linkUrl;
        this.imageUrl = imageUrl;
    }
}
