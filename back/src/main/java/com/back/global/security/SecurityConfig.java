package com.back.global.security;

import com.back.global.app.CustomConfigProperties;
import com.back.global.rsData.RsData;
import com.back.standard.util.Util;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final AuthenticationSuccessHandler customOAuth2LoginSuccessHandler;
    private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SocialLoginGuardFilter oAuth2SocialLoginGuardFilter;
    /** 약관 미동의 차단. custom.agreement.guard.enabled 가 true 일 때만 빈이 만들어진다 */
    private final org.springframework.beans.factory.ObjectProvider<MemberAgreementGuardFilter> agreementGuardFilter;
    private final CustomConfigProperties customConfigProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(
                        auth -> auth
                                // SseEmitter 전송·완료 시 컨테이너가 ASYNC dispatch로 다시 진입한다.
                                // 최초 REQUEST는 아래 /api 규칙에서 인증·인가되므로, 이미 열린 스트림의
                                // 후속 dispatch만 허용해야 "response is already committed" 오류가 나지 않는다.
                                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/*/members/{id:\\d+}",
                                        "/api/*/members/{id:\\d+}/showcases",
                                        "/api/*/parties",
                                        "/api/*/parties/{id:\\d+}",
                                        "/api/*/parties/search",
                                        "/api/*/contests",
                                        "/api/*/contests/{id:\\d+}",
                                        "/api/*/goals/{id:\\d+}",
                                        "/api/*/goals/{id:\\d+}/checklist",
                                        "/api/*/showcase/goals",
                                        "/api/*/parties/{id:\\d+}/showcase",
                                        "/api/*/parties/{id:\\d+}/showcase/comments",
                                        "/api/*/parties/showcase/top3",
                                        "/api/*/parties/top3"
                                ).permitAll()
                                .requestMatchers(
                                        "/api/*/members/login",
                                        "/api/*/members/refresh",
                                        "/api/*/members/logout"
                                ).permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/*/members/password/reset-requests",
                                        "/api/*/members/password/resets"
                                ).permitAll()
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/*/members/password/reset-tokens/*"
                                ).permitAll()
                                // GitHub App 설치 완료는 GitHub가 state·installation_id만 붙여 호출한다.
                                // 사용자 쿠키를 기대하면 setup 콜백이 401로 막혀 프론트로 돌아갈 수 없다.
                                .requestMatchers(HttpMethod.GET, "/api/*/github-app/setup").permitAll()
                                // GitHub App user authorization callback은 state로 연결하므로 GitHub 리다이렉트를 허용한다.
                                .requestMatchers(HttpMethod.GET, "/api/*/github-app/user/callback").permitAll()
                                // GitHub 웹훅은 일반 로그인 대신 컨트롤러에서 HMAC 서명을 검증한다.
                                .requestMatchers(HttpMethod.POST, "/api/*/github/webhook").permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/*/members/signup"
                                ).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/*/contests").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/*/contests/{id:\\d+}").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/*/contests/{id:\\d+}").hasRole("ADMIN")
                                .requestMatchers("/api/*/adm/**").hasRole("ADMIN")
                                .requestMatchers("/api/*/**").authenticated()
                                .anyRequest().permitAll()
                )
                .headers(
                        headers -> headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                )
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(STATELESS))
                .oauth2Login(oauth2Login -> oauth2Login
                        .successHandler(customOAuth2LoginSuccessHandler)
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .userService(customOAuth2UserService)
                        )
                        .authorizationEndpoint(
                                authorizationEndpoint -> authorizationEndpoint
                                        .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver)
                        )
                )
                // OAuth2 요청에서는 기존 인증 → 소셜 연동 가드 → OAuth2 리다이렉트 순서를 보장한다.
                .addFilterBefore(oAuth2SocialLoginGuardFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(customAuthenticationFilter, OAuth2SocialLoginGuardFilter.class)
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    Util.json.toString(
                                                            new RsData<Void>(
                                                                    "401-1",
                                                                    "로그인 후 이용해주세요."
                                                            )
                                                    )
                                            );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Util.json.toString(
                                                            new RsData<Void>(
                                                                    "403-1",
                                                                    "권한이 없습니다."
                                                            )
                                                    )
                                            );
                                        }
                                )
                );
        // 약관 미동의 차단 필터는 설정으로 켤 때만 존재한다(MemberAgreementGuardFilter).
        // 인증이 끝난 뒤에 놓아야 누구인지 보고 판단할 수 있다.
        agreementGuardFilter.ifAvailable(filter ->
                http.addFilterAfter(filter, CustomAuthenticationFilter.class));


        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(customConfigProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true);

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(List.of("*"));

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
