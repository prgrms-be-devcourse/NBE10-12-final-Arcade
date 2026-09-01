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
    private boolean draft;
    private boolean merged;
    private String baseBranch;
    private String headBranch;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime mergedAt;
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

}
