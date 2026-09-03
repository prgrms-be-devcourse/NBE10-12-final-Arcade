package com.back.domain.party.github.entity;

import com.back.domain.party.party.entity.Party;
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(name = "idx_party_github_connection_installation_id", columnList = "installation_id"))
@Getter
@NoArgsConstructor
/** Party와 GitHub repository를 OAuth credential으로 연결한 영속 상태다. */
public class PartyGithubConnection extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    /** 기존 DB 호환을 위해 Party 소유자만 보관한다. OAuth token은 사용하지 않는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_member_id", nullable = false)
    private Member credentialMember;
    // GitHub App 설치 하나는 여러 저장소를 포함할 수 있으므로 Party마다 중복될 수 있다.
    // 저장소 자체의 중복 연결은 아래 repositoryId의 unique 제약으로 막는다.
    @Column
    private Long installationId;
    // 이름 변경에도 유지되는 GitHub repository.id. 현재 정책은 하나의 repository를 하나의 Party에만 연결한다.
    @Column(unique = true)
    private Long repositoryId;
    private Long webhookId;
    @Column(nullable = false)
    private String repositoryFullName;
    private LocalDateTime lastSyncedAt;
    private String lastErrorCode;
    @Column(length = 2000)
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyGithubConnectionStatus status;

    /** GitHub App 설치를 시작할 때도 repositoryFullName은 DB 필수값이므로 함께 저장한다. */
    public PartyGithubConnection(Party party, String repositoryFullName) {
        this.party = party;
        this.credentialMember = party.getOwner();
        this.repositoryFullName = repositoryFullName;
        this.status = PartyGithubConnectionStatus.PENDING;
    }

    /** 웹훅 검증 및 초기 PR 동기화가 끝나기 전에는 이벤트 fan-out 대상에서 제외한다. */
    public void startSync() {
        this.status = PartyGithubConnectionStatus.SYNCING;
        this.lastError = null;
        this.lastErrorCode = null;
    }

    public void awaitInstallation() {
        this.status = PartyGithubConnectionStatus.PENDING;
        this.lastError = null;
        this.lastErrorCode = null;
    }

    /** GitHub App installation과 repository 불변 ID를 연결한다. */
    public void install(long installationId, long repositoryId, String repositoryFullName) {
        this.installationId = installationId;
        this.repositoryId = repositoryId;
        this.repositoryFullName = repositoryFullName;
    }

    public void activate() {
        this.status = PartyGithubConnectionStatus.ACTIVE;
        this.lastSyncedAt = java.time.LocalDateTime.now();
        this.lastError = null; this.lastErrorCode = null;
    }

    public void markError(String code, String message) {
        this.status = PartyGithubConnectionStatus.ERROR;
        this.lastErrorCode = code;
        this.lastError = message;
    }

    /** 설치가 해제되거나 접근 권한이 사라진 경우, 기존 PR은 보존하고 재설치만 요구한다. */
    public void markInstallationRequired(String code, String message) {
        this.status = PartyGithubConnectionStatus.INSTALLATION_REQUIRED;
        this.lastErrorCode = code;
        this.lastError = message;
    }

}
