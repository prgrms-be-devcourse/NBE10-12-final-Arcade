package com.back.domain.party.github.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 설치 callback을 특정 Party와 파티장에 안전하게 연결하는 일회성 state다. */
@Entity
@Getter
@NoArgsConstructor
public class GithubAppInstallState extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member requestedBy;

    @Column(nullable = false, unique = true, length = 128)
    private String state;

    @Column(length = 2048)
    private String redirectPath;

    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;

    public GithubAppInstallState(Party party, Member requestedBy, String state, String redirectPath) {
        this.party = party;
        this.requestedBy = requestedBy;
        this.state = state;
        this.redirectPath = redirectPath;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public boolean isUsable() {
        return consumedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    public void renew(String state, String redirectPath) {
        this.state = state;
        this.redirectPath = redirectPath;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
        this.consumedAt = null;
    }

    public void consume() {
        this.consumedAt = LocalDateTime.now();
    }
}
