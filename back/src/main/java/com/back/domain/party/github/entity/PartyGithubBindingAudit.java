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

/** Party GitHub 연결의 보안·운영 감사 이력. */
@Entity
@Table(name = "party_github_binding_audit")
@Getter
@NoArgsConstructor
public class PartyGithubBindingAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_repository_id")
    private GithubInstallationRepository installationRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_member_id")
    private Member actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyGithubBindingAuditAction action;

    @Column(length = 1000)
    private String reason;

    public PartyGithubBindingAudit(Party party, GithubInstallationRepository installationRepository,
                                   Member actor, PartyGithubBindingAuditAction action, String reason) {
        this.party = party;
        this.installationRepository = installationRepository;
        this.actor = actor;
        this.action = action;
        this.reason = reason;
    }
}
