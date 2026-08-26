package com.back.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class SecurityUser extends User implements OAuth2User {
    private final int id;
    private final String nickname;

    public SecurityUser(
            int id,
            String username,
            String nickname,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, "", authorities); // 우리의 시나리오(REST API)에서는 이 객체의 비밀번호 필드를 활용할 일이 없다.
        this.id = id;
        this.nickname = nickname;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    // OAuth2User.getName()은 유니크한 값을 리턴해야 하는데, nickname은 유니크하지 않을 수 있으므로 username을 리턴한다.
    @Override
    public String getName() {
        return getUsername();
    }
}
