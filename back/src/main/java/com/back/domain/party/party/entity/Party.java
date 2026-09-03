package com.back.domain.party.party.entity;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
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
import org.hibernate.annotations.BatchSize;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_contest_id")
    private Contest targetContest;

    // targetContestId가 없을 때(미등록 외부 대회) 자유 입력
    private String contestTitle;
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

    // 완료 처리 시점. status만 두면 "언제 끝났는지"가 남지 않아, 성취(Project.endDate)가 참조할 값이 없어진다.
    // PartyCompletedEvent에 실려 나가므로 리스너가 재실행돼도 같은 값이 쓰인다.
    private LocalDateTime completedAt;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Position> positions = new ArrayList<>();

    public Party(
        Member owner,
        String partyName,
        String title,
        String description,
        Contest targetContest,
        String contestTitle,
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
        this.targetContest = targetContest;
        this.contestTitle = contestTitle;
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
        return this.owner.getId().equals(member.getId());
    }

    public void checkModifiable() {
        if (this.status != PartyStatus.RECRUITING) {
            throw new ServiceException("409-1", "모집이 종료된 파티는 수정할 수 없습니다.");
        }
    }

    public void checkDeletable() {
        if (this.status != PartyStatus.RECRUITING) {
            throw new ServiceException("409-1", "모집 완료되지 않은 파티만 삭제할 수 있습니다.");
        }
    }

    public void update(
            String partyName, String title, String description,
            Contest targetContest, String contestTitle, String contestLinkUrl,
            TopicType topicType, PartyTag partyTag, String githubRepoUrl,
            LocalDateTime deadline
    ) {
        this.partyName = partyName;
        this.title = title;
        this.description = description;
        this.targetContest = targetContest;
        this.contestTitle = contestTitle;
        this.contestLinkUrl = contestLinkUrl;
        this.topicType = topicType;
        this.partyTag = partyTag;
        this.githubRepoUrl = githubRepoUrl;
        this.deadline = deadline;
    }

    /** 모집이 끝난 뒤에도 GitHub App 재연동을 위해 저장소 주소만 별도로 바꿀 수 있다. */
    public void updateGithubRepoUrl(String githubRepoUrl) {
        this.githubRepoUrl = githubRepoUrl;
    }

    public Position findPosition(long positionId) {
        return positions.stream()
                .filter(p -> p.getId() == positionId)
                .findFirst()
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 포지션입니다."));
    }

    public void closeRecruiting() {
        if (this.status != PartyStatus.RECRUITING) {
            throw new ServiceException("409-1", "이미 모집이 종료된 파티입니다.");
        }
        this.status = PartyStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status != PartyStatus.IN_PROGRESS) {
            throw new ServiceException("409-1", "IN_PROGRESS 상태의 파티만 완료 처리할 수 있습니다.");
        }
        this.status = PartyStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
