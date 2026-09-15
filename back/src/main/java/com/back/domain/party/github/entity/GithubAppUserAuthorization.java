package com.back.domain.party.github.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GitHub App user-to-server OAuth authorization.
 * token 값은 애플리케이션 암호화 계층을 거친 ciphertext만 저장하며 클라이언트에 반환하지 않는다.
 */
@Entity
@Table(name = "github_app_user_authorization")
@Getter
@NoArgsConstructor
public class GithubAppUserAuthorization extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, unique = true)
    private Long githubUserId;

    @Lob
    @Column(nullable = false)
    private String encryptedAccessToken;

    @Lob
    private String encryptedRefreshToken;

    @Column(nullable = false)
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private LocalDateTime revokedAt;

    public GithubAppUserAuthorization(Member member, long githubUserId, String encryptedAccessToken,
                                      String encryptedRefreshToken, LocalDateTime accessTokenExpiresAt,
                                      LocalDateTime refreshTokenExpiresAt) {
        this.member = member;
        this.githubUserId = githubUserId;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void renew(String encryptedAccessToken, String encryptedRefreshToken,
                      LocalDateTime accessTokenExpiresAt, LocalDateTime refreshTokenExpiresAt) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.revokedAt = null;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isUsable() {
        return revokedAt == null && accessTokenExpiresAt.isAfter(LocalDateTime.now());
    }
}
