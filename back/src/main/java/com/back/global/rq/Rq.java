package com.back.global.rq;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import com.back.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final MemberService memberService;
    private final CustomConfigProperties customConfigProperties;

    public Member getActor() {
        return Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser)
                .map(principal -> (SecurityUser) principal)
                .map(securityUser -> new Member(securityUser.getId(), securityUser.getRole()))
                .orElse(null);
    }

    public Member getActorFromDb() {
        Member actor = getActor();

        if (actor == null) {
            return null;
        }

        return memberService.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("401-3", "유효하지 않은 회원입니다."));
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
                .ofNullable(req.getHeader(name))
                .filter(headerValue -> !headerValue.isBlank())
                .orElse(defaultValue);
    }

    public void setHeader(String name, String value) {
        if (value == null) value = "";

        if (value.isBlank()) {
            req.removeAttribute(name);
        } else {
            resp.setHeader(name, value);
        }
    }

    public String getCookieValue(String name, String defaultValue) {
        return Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(defaultValue);
    }

    public void setCookie(String name, String value) {
        setCookie(name, value, 60 * 60 * 24 * 365);
    }

    public void setCookie(String name, String value, int maxAgeSeconds) {
        addCookie(name, value, maxAgeSeconds, "/");
    }

    public void deleteCookie(String name) {
        setCookie(name, null);
    }

    private static final int VIEW_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24;

    /** 이 방문자가 오늘(24h) 이미 조회수를 올린 대상인지 - 쿠키 유무로만 판단한다 */
    public boolean hasViewCookie(String name) {
        return getCookieValue(name, null) != null;
    }

    /**
     * 조회수 쿠키는 accessToken/refreshToken과 달리 요청마다 새로 생기고 대상 수만큼 쌓이므로,
     * path="/"로 전체 도메인 요청에 실리지 않게 그 기능의 API prefix로 좁혀서 심는다.
     */
    public void setViewCookie(String name, String path) {
        addCookie(name, "true", VIEW_COOKIE_MAX_AGE_SECONDS, path);
    }

    private void addCookie(String name, String value, int maxAgeSeconds, String path) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(customConfigProperties.getCookie().isSecure());
        cookie.setAttribute("SameSite", customConfigProperties.getCookie().getSameSite());

        cookie.setMaxAge(value.isBlank() ? 0 : maxAgeSeconds);

        resp.addCookie(cookie);
    }

    @SneakyThrows
    public void sendRedirect(String url) {
        resp.sendRedirect(url);
    }
}
