package com.back.domain.party.showcase.entity;

import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_party_showcase_published", columnList = "published"))
public class PartyShowcase extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private boolean published;

    private LocalDateTime publishedAt;

    // 전시 단계 전용 카운터. 모집글(Party.likeCount/viewCount)과 완전히 독립적으로 0부터 새로 쌓는다
    // 파티가 삭제돼도(모집글 반응만 같이 지워짐) 전시글 반응은 영향받지 않는다(3.2).
    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int viewCount;

    public PartyShowcase(Party party) {
        this.party = party;
        this.published = false;
        this.likeCount = 0;
        this.viewCount = 0;
    }

    public void publish(String title, String description) {
        this.title = title;
        this.description = description;
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
