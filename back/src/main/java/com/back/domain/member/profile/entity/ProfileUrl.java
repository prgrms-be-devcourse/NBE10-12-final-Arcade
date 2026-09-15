package com.back.domain.member.profile.entity;

import com.back.global.exception.ServiceException;

import java.util.regex.Pattern;

/** 프로필에 저장하는 주소를 다듬는다. webPage 와 링크가 같은 규칙을 쓴다. */
final class ProfileUrl {

    // 스킴이 http/https 인 주소. 대소문자는 가리지 않는다.
    private static final Pattern HTTP_URL = Pattern.compile("(?i)^https?://.+");
    // 스킴처럼 생긴 것 전부. 위 검사를 통과하지 못하면 거부한다.
    // 점을 뺀 이유는 example.com:8080 같은 호스트를 스킴에 대한 예외
    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+-]*:.*");

    private ProfileUrl() {
    }

    /**
     * 스킴이 없으면 https 를 붙인다.
     * 없는 채로 두면 <a href="github.com/x"> 가 상대 경로로 해석돼 링크가 열리지 않는다.
     * 스킴이 있으면 http/https 만 받는다. javascript: 를 그대로 두면
     * 화면에서 <a href> 를 눌렀을 때 스크립트가 실행된다.
     */
    static String normalize(String url) {
        if (url == null) return null;

        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;

        if (HTTP_URL.matcher(trimmed).matches()) return trimmed;

        if (HAS_SCHEME.matcher(trimmed).matches()) {
            throw new ServiceException("400-1", "링크는 http 또는 https 주소만 등록할 수 있습니다.");
        }

        return "https://" + trimmed;
    }
}
