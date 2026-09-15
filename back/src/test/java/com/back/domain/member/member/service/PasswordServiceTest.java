package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordServiceTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final PasswordAuditService auditService = mock(PasswordAuditService.class);
    private final PasswordService passwordService = new PasswordService(
            memberRepository,
            passwordEncoder,
            new PasswordPolicy(new CustomConfigProperties()),
            refreshTokenService,
            auditService
    );

    @Test
    void resetPasswordRegistersLocalPasswordForSocialMember() {
        Member socialMember = memberWithPassword(null);

        passwordService.resetPassword(socialMember, "NewPassword!1", "NewPassword!1");

        assertThat(passwordEncoder.matches("NewPassword!1", socialMember.getPassword())).isTrue();
        verify(refreshTokenService).revokeAll(socialMember.getId());
        verify(auditService).passwordReset(socialMember.getId());
    }

    @Test
    void resetPasswordRejectsReusedPassword() {
        Member member = memberWithPassword("OldPassword!1");

        assertThatThrownBy(() -> passwordService.resetPassword(member, "OldPassword!1", "OldPassword!1"))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getRsData().resultCode()).isEqualTo("409-2"));
    }

    @Test
    void changePasswordRejectsSocialOnlyMemberAndWrongCurrentPassword() {
        Member actor = new Member(1L, Role.MEMBER);
        Member socialMember = memberWithPassword(null);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(socialMember));

        assertThatThrownBy(() -> passwordService.changePassword(actor, "anything", "NewPassword!1", "NewPassword!1"))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getRsData().resultCode()).isEqualTo("400-4"));

        Member member = memberWithPassword("OldPassword!1");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> passwordService.changePassword(actor, "wrong", "NewPassword!1", "NewPassword!1"))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getRsData().resultCode()).isEqualTo("401-5"));
    }

    @Test
    void changePasswordUsesAuthenticatedActorAndNormalizesEmailLookup() {
        Member actor = new Member(1L, Role.MEMBER);
        Member member = memberWithPassword("OldPassword!1");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        passwordService.changePassword(actor, "OldPassword!1", "NewPassword!1", "NewPassword!1");

        assertThat(passwordEncoder.matches("NewPassword!1", member.getPassword())).isTrue();
        assertThat(passwordService.normalizeEmail("  USER@EXAMPLE.COM  ")).isEqualTo("user@example.com");
        verify(refreshTokenService).revokeAll(member.getId());
        verify(auditService).passwordChanged(member.getId());
    }

    private Member memberWithPassword(String rawPassword) {
        TestMember member = new TestMember(
                "member@test.com",
                rawPassword == null ? null : passwordEncoder.encode(rawPassword),
                "회원",
                null
        );
        member.assignId(1L);
        return member;
    }

    private static class TestMember extends Member {
        private TestMember(String email, String password, String name, String profileImgUrl) {
            super(email, password, name, profileImgUrl);
        }

        private void assignId(long id) {
            setId(id);
        }
    }
}
