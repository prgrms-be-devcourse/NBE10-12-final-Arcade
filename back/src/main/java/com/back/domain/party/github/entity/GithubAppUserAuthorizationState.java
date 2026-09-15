package com.back.domain.party.github.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** GitHub App user OAuth callback의 CSRF state와 PKCE verifier를 짧게 보관한다. */
@Entity
@Table(name = "github_app_user_authorization_state")
@Getter
@NoArgsConstructor
public class GithubAppUserAuthorizationState extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 128)
    private String state;

    @Column(nullable = false, length = 256)
    private String codeVerifier;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;

    public GithubAppUserAuthorizationState(Member member, String state, String codeVerifier) {
        this.member = member;
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public boolean isUsable() {
        return consumedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    public void consume() {
        this.consumedAt = LocalDateTime.now();
    }
}
