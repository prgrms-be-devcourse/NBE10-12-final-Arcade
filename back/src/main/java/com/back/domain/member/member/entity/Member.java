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

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;
    @Column(unique = true)
    private String githubProviderUserId = null;
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
        this.githubProviderUserId = githubProviderUserId;
        this.githubEmail = githubEmail;
    }

    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
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
}
