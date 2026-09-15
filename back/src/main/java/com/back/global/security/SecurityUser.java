package com.back.global.security;

import com.back.domain.member.member.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class SecurityUser extends User implements OAuth2User {
    private final long id;
    private final Role role;

    /** 인증 방식과 무관하게 회원 식별자와 역할만으로 principal을 구성한다. */
    public SecurityUser(long id, Role role) {
        super(String.valueOf(id), "", authoritiesOf(role));
        this.id = id;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    // OAuth2User.getName()은 유니크한 값을 리턴해야 하므로 회원 ID를 사용한다.
    @Override
    public String getName() {
        return getUsername();
    }

    private static Collection<? extends GrantedAuthority> authoritiesOf(Role role) {
        return role == Role.ADMIN
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of();
    }
}
