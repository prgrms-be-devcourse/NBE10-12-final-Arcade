package com.back.domain.party.github.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GitHub App이 개인 계정 또는 조직에 설치된 사실이다.
 * 특정 Party나 서비스 회원의 소유 개념이 아니며, 설치된 레포 inventory의 상위 모델이다.
 */
@Entity
@Table(name = "github_app_installation")
@Getter
@NoArgsConstructor
public class GithubAppInstallation extends BaseEntity {
    @Column(nullable = false, unique = true)
    private Long installationId;

    @Column(nullable = false)
    private Long accountGithubId;

    @Column(nullable = false)
    private String accountLogin;

    @Column(nullable = false)
    private String accountType;

    /** 설치를 수행한 GitHub 계정의 numeric id. 감사 정보일 뿐 Party 연결 권한의 근거는 아니다. */
    private Long installedByGithubUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GithubAppInstallationStatus status;

    private LocalDateTime installedAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime deletedAt;

    public GithubAppInstallation(long installationId, long accountGithubId, String accountLogin,
                                 String accountType, Long installedByGithubUserId) {
        this.installationId = installationId;
        this.accountGithubId = accountGithubId;
        this.accountLogin = accountLogin;
        this.accountType = accountType;
        this.installedByGithubUserId = installedByGithubUserId;
        this.status = GithubAppInstallationStatus.ACTIVE;
        this.installedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = GithubAppInstallationStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = GithubAppInstallationStatus.ACTIVE;
        this.suspendedAt = null;
        this.deletedAt = null;
    }

    public void refreshAccount(long accountGithubId, String accountLogin, String accountType) {
        this.accountGithubId = accountGithubId;
        this.accountLogin = accountLogin;
        this.accountType = accountType;
        activate();
    }

    public void delete() {
        this.status = GithubAppInstallationStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
