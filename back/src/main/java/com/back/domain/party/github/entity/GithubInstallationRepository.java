package com.back.domain.party.github.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** GitHub App installation에서 선택된 레포의 서버 내부 inventory다. */
@Entity
@Table(name = "github_installation_repository", uniqueConstraints =
        @UniqueConstraint(name = "uk_github_installation_repository", columnNames = {"installation_id", "repository_id"}))
@Getter
@NoArgsConstructor
public class GithubInstallationRepository extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id", nullable = false)
    private GithubAppInstallation installation;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GithubInstallationRepositoryStatus status;

    private LocalDateTime removedAt;

    public GithubInstallationRepository(GithubAppInstallation installation, long repositoryId, String fullName) {
        this.installation = installation;
        this.repositoryId = repositoryId;
        this.fullName = fullName;
        this.status = GithubInstallationRepositoryStatus.AVAILABLE;
    }

    public void refresh(String fullName) {
        this.fullName = fullName;
        this.status = GithubInstallationRepositoryStatus.AVAILABLE;
        this.removedAt = null;
    }

    public void remove() {
        this.status = GithubInstallationRepositoryStatus.REMOVED;
        this.removedAt = LocalDateTime.now();
    }
}
