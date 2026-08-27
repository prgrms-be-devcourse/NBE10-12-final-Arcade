package com.back.domain.party.party.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Party extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false)
    private String partyName;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    // 대회 허브에 등록된 대회를 가리킴. Contest 도메인이 아직 없어 연관관계 없이 값만 보관.
    // Contest 엔티티가 생기면 @ManyToOne 매핑으로 전환 예정.
    @Column(name = "target_contest_id")
    private Integer targetContestId;

    // targetContestId가 없을 때(미등록 외부 대회) 자유 입력
    private String contestName;
    private String contestLinkUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicType topicType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyTag partyTag;

    private String githubRepoUrl;

    @Column(nullable = false)
    private int checklistRequiredApprovals;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Position> positions = new ArrayList<>();

    public Party(
        Member owner,
        String partyName,
        String title,
        String description,
        Integer targetContestId,
        String contestName,
        String contestLinkUrl,
        TopicType topicType,
        PartyTag partyTag,
        String githubRepoUrl,
        int checklistRequiredApprovals,
        LocalDateTime deadline
    ) {
        this.owner = owner;
        this.partyName = partyName;
        this.title = title;
        this.description = description;
        this.targetContestId = targetContestId;
        this.contestName = contestName;
        this.contestLinkUrl = contestLinkUrl;
        this.topicType = topicType;
        this.partyTag = partyTag;
        this.githubRepoUrl = githubRepoUrl;
        this.checklistRequiredApprovals = checklistRequiredApprovals;
        this.deadline = deadline;
        this.status = PartyStatus.RECRUITING;
        this.likeCount = 0;
        this.viewCount = 0;
    }

    public void addPosition(Position position) {
        positions.add(position);
        position.assignParty(this);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public boolean isOwnedBy(Member member) {
        return this.owner.getId() == member.getId();
    }
}
