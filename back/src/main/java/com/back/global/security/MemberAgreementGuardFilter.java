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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 필수 약관에 동의하지 않은 계정의 요청을 막는다 (개인정보보호법 / ISMS).
 *
 * 화면 모달만으로는 API 를 직접 부르면 그냥 통과한다. 그래서 서버에서도 같은 상태를 강제한다.
 *
 * <p><b>기본은 꺼져 있다.</b> {@code matchIfMissing = false} 라 설정을 적지 않으면 빈으로 등록되지 않는다.
 * 온보딩 화면이 없는 환경에서 켜면 동의할 방법 없이 전원이 403 에 갇히기 때문이다 -
 * 프론트가 준비된 환경에서만 {@code custom.agreement.guard.enabled: true} 로 켠다.
 *
 * <p>인증 필터 뒤에 놓는다. 누구인지 알아야 동의 여부를 볼 수 있다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "custom.agreement.guard", name = "enabled",
        havingValue = "true", matchIfMissing = false)
public class MemberAgreementGuardFilter extends OncePerRequestFilter {

    /**
     * 미동의 계정에도 열어 두는 경로.
     *
     * 동의하러 가는 길과 빠져나가는 길은 막으면 안 된다 - 막으면 아무것도 할 수 없는 계정이 된다.
     */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/members/me/agreements",  // 동의하러 가는 길
            "/api/v1/members/me",             // 화면이 동의 여부를 확인하는 통로
            "/api/v1/members/logout",         // 갇힌 상태에서 나갈 수 있어야 한다
            "/api/v1/members/refresh"         // 토큰 만료로 더 갇히지 않게
    );

    private final Rq rq;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 비인증으로 열린 조회(파티·전시·프로필 등)는 애초에 대상이 아니다.
        // 인증이 필요한 /api 요청만 본다.
        return !request.getRequestURI().startsWith("/api/")
                || ALLOWED_PATHS.contains(request.getRequestURI())
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Member actor = currentActor();

        // 비로그인은 이 필터가 판단할 문제가 아니다. 인가 규칙이 알아서 401 을 준다.
        //
        // 역할로 예외를 두지 않는다. 관리자도 동의 기록이 있어야 통과한다 -
        // 시드 계정은 만들 때 동의를 함께 기록하고(BaseInitData), 기존 계정은 배포 전 DDL 로 채운다.
        // 역할 예외를 두면 "관리자는 동의 없이 서비스 이용" 이라는 구멍이 생긴다.
        if (actor == null || actor.hasAgreedToRequiredTerms()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 403 중에서 이 사유만의 코드를 쓴다. 프론트는 resultCode 로만 이유를 가릴 수 있어,
        // 다른 사유와 코드를 겹치면 "권한 부족"·"정지"·"동의 미완료" 를 구분하지 못한다.
        // 403-1 = 권한 없음, 403-2 = 정지된 계정(MemberService.login) 이라 403-3 을 쓴다.
        writeForbidden(response);
    }

    /** 토큰이 깨졌거나 회원이 사라진 경우는 인가 규칙에 맡긴다 */
    private Member currentActor() {
        try {
            return rq.getActorFromDb();
        } catch (ServiceException e) {
            return null;
        }
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        RsData<Void> rsData = new RsData<>("403-3", "서비스 이용에 필요한 약관 동의가 필요합니다.");

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(rsData.statusCode());
        response.getWriter().write(Util.json.toString(rsData));
    }
}
