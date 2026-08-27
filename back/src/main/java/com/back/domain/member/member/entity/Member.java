package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private String profileImgUrl;
    private Long hostId = null;
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;

    public Member(long id, String email, String nickname) {
        setId(id);
        this.email = email;
        this.name = nickname;
    }

    public Member(String email, String password, String nickname, String profileImgUrl) {
        this.email = email;
        this.password = password;
        this.name = nickname;
        this.profileImgUrl = profileImgUrl;
        this.apiKey = UUID.randomUUID().toString();
    }

    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void modify(String nickname, String profileImgUrl) {
        this.name = nickname;
        this.profileImgUrl = profileImgUrl;
    }

    public String getProfileImgUrlOrDefault() {
        if (profileImgUrl == null)
            return "https://placehold.co/600x600?text=U_U";

        return profileImgUrl;
    }

    public boolean isAdmin() {
        if ("system".equals(email)) return true;
        if ("admin".equals(email)) return true;

        return false;
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
