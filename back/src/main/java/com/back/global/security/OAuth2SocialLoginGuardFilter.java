package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.standard.util.Util;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SocialLoginGuardFilter extends OncePerRequestFilter {
    private static final String GITHUB_AUTHORIZATION_URI = "/oauth2/authorization/github";

    private final Rq rq;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Member actor;

        try {
            actor = rq.getActorFromDb();
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setStatus(rsData.statusCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(Util.json.toString(rsData));
            return;
        }

        // 현재는 GitHub만 지원한다. 다른 provider를 추가해도 GitHub 중복 연결만 차단한다.
        if (GITHUB_AUTHORIZATION_URI.equals(request.getRequestURI())
                && actor != null && actor.getGithubProviderUserId() != null
                && !actor.getGithubProviderUserId().isBlank()) {
            RsData<Void> rsData = new RsData<>("400-1",
                    "현재 계정에는 이미 GitHub 계정이 연결되어 있습니다.");
            response.setStatus(rsData.statusCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(Util.json.toString(rsData));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
