package com.back.domain.party.showcase.comment.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_showcase_comment_party_showcase_id", columnList = "party_showcase_id"))
public class ShowcaseComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_showcase_id", nullable = false)
    private PartyShowcase showcase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private ShowcaseComment parent;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean deleted;

    public ShowcaseComment(PartyShowcase showcase, Member author, ShowcaseComment parent, String content) {
        this.showcase = showcase;
        this.author = author;
        this.parent = parent;
        this.content = content;
        this.deleted = false;
    }

    public boolean isReply() {
        return parent != null;
    }

    public boolean isAuthor(Member member) {
        return this.author.getId().equals(member.getId());
    }

    public void edit(String content) {
        if (this.deleted) {
            throw new ServiceException("409-1", "삭제된 댓글은 수정할 수 없습니다.");
        }
        this.content = content;
    }

    public void softDelete() {
        if (this.deleted) {
            throw new ServiceException("409-1", "이미 삭제된 댓글입니다.");
        }
        this.deleted = true;
        this.content = null;
    }
}
