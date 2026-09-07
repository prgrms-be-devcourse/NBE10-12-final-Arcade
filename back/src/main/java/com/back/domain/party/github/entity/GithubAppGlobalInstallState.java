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

/** Party와 무관한 GitHub App 설치와 설치 중 사용자 인증 callback을 연결하는 일회성 state다. */
@Entity
@Table(name = "github_app_global_install_state")
@Getter
@NoArgsConstructor
public class GithubAppGlobalInstallState extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_member_id", nullable = false)
    private Member requestedBy;

    @Column(nullable = false, unique = true, length = 128)
    private String state;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;

    public GithubAppGlobalInstallState(Member requestedBy, String state) {
        this.requestedBy = requestedBy;
        this.state = state;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public boolean isUsable() {
        return consumedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    public void consume() {
        this.consumedAt = LocalDateTime.now();
    }
}
