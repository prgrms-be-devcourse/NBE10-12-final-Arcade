package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {

    private LocalDateTime lastLoginAt;
    @Column(unique = true)
    private String email;
    private String password;
    private String name;
    @Column(unique = true)
    private String apiKey;
    @Setter
    private String profileImgUrl;
    private Long hostId = null;
    private boolean active = true;
    private String suspendReason = null;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;
    @Column(unique = true)
    private String githubProviderUserId = null;
    /** GitHub가 부여한 불변 numeric user id. PR 작성자 및 GitHub App user token의 계정 대조에 사용한다. */
    @Column(unique = true)
    private Long githubUserId = null;
    private String githubEmail = null;

    /** JWT 인증 정보를 담는 비영속 임시 회원 객체용 생성자다. */
    public Member(long id, Role role) {
        setId(id);
        this.role = role;
    }

    public Member(String email, String password, String name, String profileImgUrl) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImgUrl = profileImgUrl;
        this.apiKey = UUID.randomUUID().toString();
    }

    public void setGithubSocial(String githubProviderUserId, String githubEmail) {
        Long socialGithubUserId = numericGithubUserId(githubProviderUserId);
        if (this.githubUserId != null && socialGithubUserId != null && !this.githubUserId.equals(socialGithubUserId)) {
            throw new IllegalArgumentException("이미 연결된 GitHub App 인증 계정과 다른 GitHub 소셜 계정입니다.");
        }
        this.githubProviderUserId = githubProviderUserId;
        this.githubEmail = githubEmail;
        this.githubUserId = socialGithubUserId;
    }

    /**
     * GitHub App 사용자 인증으로 확인한 불변 GitHub ID를 연결한다.
     * 소셜 계정이 이미 연결돼 있으면 같은 계정인지 검증하고, 없으면 App 인증을 최초 연결로 사용한다.
     */
    public void linkGithubAppUserId(long githubUserId) {
        Long knownId = this.githubUserId == null ? numericGithubUserId(this.githubProviderUserId) : this.githubUserId;
        if (knownId != null && knownId != githubUserId) {
            throw new IllegalArgumentException("연결된 GitHub 계정과 GitHub App 인증 계정이 일치하지 않습니다.");
        }
        this.githubUserId = githubUserId;
    }

    private Long numericGithubUserId(String providerUserId) {
        if (providerUserId == null) return null;
        String value = providerUserId.startsWith("GITHUB__")
                ? providerUserId.substring("GITHUB__".length()) : providerUserId;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** PasswordEncoder로 인코딩된 값만 전달받아 저장한다. */
    public void changeEncodedPassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("인코딩된 비밀번호는 비어 있을 수 없습니다.");
        }
        this.password = encodedPassword;
    }

    public void grantAdmin() {
        this.role = Role.ADMIN;
    }

    public void modify(String name, String profileImgUrl) {
        this.name = name;
        this.profileImgUrl = profileImgUrl;
    }

    public String getProfileImgUrlOrDefault() {
        if (profileImgUrl == null)
            return "https://placehold.co/600x600?text=U_U";

        return profileImgUrl;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesAsStringList()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private List<String> getAuthoritiesAsStringList() {
        List<String> authorities = new ArrayList<>();

        if (isAdmin())
            authorities.add("ROLE_ADMIN");

        return authorities;
    }

    public void suspend(String reason) {
        this.active = false;
        this.suspendReason = reason;
    }

    public void activate() {
        this.active = true;
        this.suspendReason = null;
    }
}
