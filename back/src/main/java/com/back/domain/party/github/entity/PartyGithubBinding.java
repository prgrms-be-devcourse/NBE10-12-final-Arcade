package com.back.domain.party.github.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Party와 GitHub App installation 레포의 연결 이력이다.
 * Party 하나는 활성 연결을 하나만 가지며, 종료 또는 해제 후에도 이력과 PR은 보존한다.
 * 활성 레포 중복은 서비스 계층에서 잠금과 함께 방지한다. (이력 보존 때문에 단순 unique 제약을 두지 않는다.)
 */
@Entity
@Table(name = "party_github_binding")
@Getter
@NoArgsConstructor
public class PartyGithubBinding extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_repository_id", nullable = false)
    private GithubInstallationRepository installationRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by_member_id", nullable = false)
    private Member linkedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyGithubBindingStatus status;

    @Column(nullable = false)
    private LocalDateTime linkedAt;
    /** PR을 Party 활동으로 추적하기 시작한 시각. 최초 연결 시 한 번만 기록한다. */
    @Column(nullable = false)
    private OffsetDateTime trackingStartedAt;
    private LocalDateTime disconnectedAt;
    private String disconnectReason;

    public PartyGithubBinding(Party party, GithubInstallationRepository installationRepository, Member linkedBy) {
        this.party = party;
        this.installationRepository = installationRepository;
        this.linkedBy = linkedBy;
        this.status = PartyGithubBindingStatus.SYNCING;
        this.linkedAt = LocalDateTime.now();
        this.trackingStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void archive() {
        disconnect(PartyGithubBindingStatus.ARCHIVED, "PARTY_COMPLETED");
    }

    public void activate() {
        this.status = PartyGithubBindingStatus.ACTIVE;
    }

    public void disconnect(String reason) {
        disconnect(PartyGithubBindingStatus.DISCONNECTED, reason);
    }

    private void disconnect(PartyGithubBindingStatus status, String reason) {
        this.status = status;
        this.disconnectedAt = LocalDateTime.now();
        this.disconnectReason = reason;
    }
}
