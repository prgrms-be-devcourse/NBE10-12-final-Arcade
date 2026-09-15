package com.back.domain.party.partyPr.entity;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.partyPr.model.GithubPullRequestSnapshot;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_party_pr_github_pr", columnNames = {"party_id", "github_pr_id"}))
public class PartyPr extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "github_pr_id", nullable = false)
    private Long githubPrId;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(length = 1024, nullable = false)
    private String htmlUrl;

    @Column(nullable = false)
    private String state;

    private String authorLogin;
    /** GitHub login은 변경될 수 있으므로 회원의 "내 PR" 매칭은 이 numeric id로 수행한다. */
    private Long authorGithubUserId;
    private boolean draft;
    private boolean merged;
    private String baseBranch;
    private String headBranch;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime mergedAt;
    @Column(nullable = false)
    private OffsetDateTime githubUpdatedAt;

    public PartyPr(Party party, GithubPullRequestSnapshot data) {
        this.party = party;
        this.githubPrId = data.githubPrId();
        update(data);
    }

    public void update(GithubPullRequestSnapshot data) {
        this.number = data.number();
        this.title = data.title();
        this.htmlUrl = data.htmlUrl();
        this.state = data.state();
        this.authorGithubUserId = data.authorGithubUserId();
        this.authorLogin = data.authorLogin();
        this.draft = data.draft();
        this.merged = data.merged();
        this.baseBranch = data.baseBranch();
        this.headBranch = data.headBranch();
        this.openedAt = data.openedAt();
        this.closedAt = data.closedAt();
        this.mergedAt = data.mergedAt();
        this.githubUpdatedAt = data.githubUpdatedAt();
    }

    /** 같은 GitHub 상태의 재전송은 DB write 및 SSE 재발행 대상이 아니다. */
    public boolean hasSameContent(GithubPullRequestSnapshot data) {
        return number == data.number()
                && draft == data.draft()
                && merged == data.merged()
                && java.util.Objects.equals(title, data.title())
                && java.util.Objects.equals(htmlUrl, data.htmlUrl())
                && java.util.Objects.equals(state, data.state())
                && java.util.Objects.equals(authorGithubUserId, data.authorGithubUserId())
                && java.util.Objects.equals(authorLogin, data.authorLogin())
                && java.util.Objects.equals(baseBranch, data.baseBranch())
                && java.util.Objects.equals(headBranch, data.headBranch())
                && java.util.Objects.equals(openedAt, data.openedAt())
                && java.util.Objects.equals(closedAt, data.closedAt())
                && java.util.Objects.equals(mergedAt, data.mergedAt())
                && java.util.Objects.equals(githubUpdatedAt, data.githubUpdatedAt());
    }

}
