package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.standard.util.Util;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final Rq rq;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.debug("Processing request for " + request.getRequestURI());

        try {
            work(request, response, filterChain);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.statusCode());
            response.getWriter().write(
                    Util.json.toString(rsData)
            );
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // OAuth2 시작/콜백 요청에서도 기존 로그인 계정을 복원해야 계정 연동 여부를 판단할 수 있다.
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith("/api/")
                && !requestUri.startsWith("/oauth2/authorization/")
                && !requestUri.startsWith("/login/oauth2/code/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증, 인가가 필요없는 API 요청이라면 패스
        if (List.of("/api/v1/members/login", "/api/v1/members/logout", "/api/v1/members/signup", "/api/v1/members/refresh").contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken;
        String refreshToken;

        String headerAuthorization = rq.getHeader("Authorization", "");

        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw new ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.");

            accessToken = headerAuthorization.substring("Bearer ".length()).trim();
            refreshToken = rq.getCookieValue("refreshToken", "");
        } else {
            refreshToken = rq.getCookieValue("refreshToken", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        logger.debug("accessToken : " + accessToken);
        logger.debug("refreshToken exists : " + !refreshToken.isBlank());

        boolean isAccessTokenExists = !accessToken.isBlank();
        boolean isRefreshTokenExists = !refreshToken.isBlank();

        if (!isAccessTokenExists && !isRefreshTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        Member member = null;

        if (isAccessTokenExists) {
            var payload = memberService.payload(accessToken);

            if (payload != null) {
                member = new Member(payload.memberId(), payload.role());
            }
        }

        if (member == null && isRefreshTokenExists) {
            // refresh token은 한 번 사용하면 폐기되므로, 재발급된 토큰 쌍으로 즉시 교체한다.
            MemberLoginDto loginDto = memberService.refreshToken(refreshToken);
            var payload = memberService.payload(loginDto.accessToken());

            if (payload == null) {
                throw new ServiceException("401-3", "유효하지 않은 토큰입니다.");
            }

            member = new Member(payload.memberId(), payload.role());
            rq.setCookie("accessToken", loginDto.accessToken());
            rq.setCookie("refreshToken", loginDto.refreshToken());
        }

        if (member == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails user = new SecurityUser(
                member.getId(), member.getRole());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );

        // 이 시점 이후부터는 시큐리티가 이 요청을 인증된 사용자의 요청이다.
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
