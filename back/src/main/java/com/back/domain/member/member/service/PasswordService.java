package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshTokenService refreshTokenService;
    private final PasswordAuditService passwordAuditService;

    /** 이메일 앞뒤 공백을 제거하고 소문자로 정규화해 활성 회원만 조회한다. */
    public Optional<Member> findActiveMemberByEmail(String email) {
        if (email == null) return Optional.empty();

        return memberRepository.findByEmail(normalizeEmail(email))
                .filter(Member::isActive);
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 재설정 토큰 검증은 호출자에게 맡기고, 검증된 회원의 비밀번호만 변경한다.
     * 이메일이 있는 소셜 회원은 기존 로컬 비밀번호가 없어도 이 경로로 비밀번호를 등록할 수 있다.
     */
    @Transactional
    public void resetPassword(Member member, String newPassword, String newPasswordConfirm) {
        validateNewPasswordForMember(member, newPassword, newPasswordConfirm);
        applyValidatedPasswordReset(member, newPassword);
    }

    /** 토큰을 소진하기 전에 회원 상태, 정책, 확인값, 기존 비밀번호 재사용 여부를 모두 검증한다. */
    public Member validatePasswordReset(long memberId, String newPassword, String newPasswordConfirm) {
        Member member = activeMember(memberId);
        validateNewPasswordForMember(member, newPassword, newPasswordConfirm);
        return member;
    }

    /** 검증과 토큰의 원자적 소비가 끝난 뒤 실제 변경과 세션 폐기만 수행한다. */
    public void applyValidatedPasswordReset(Member member, String newPassword) {
        member.changeEncodedPassword(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(member.getId());
        passwordAuditService.passwordReset(member.getId());
    }

    @Transactional
    public void resetPassword(long memberId, String newPassword, String newPasswordConfirm) {
        Member member = validatePasswordReset(memberId, newPassword, newPasswordConfirm);
        applyValidatedPasswordReset(member, newPassword);
    }

    /** 인증 principal에 해당하는 회원만 변경할 수 있도록 Member 객체를 입력으로 받는다. */
    @Transactional
    public void changePassword(Member actor, String currentPassword, String newPassword, String newPasswordConfirm) {
        Member member = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        if (member.getPassword() == null || member.getPassword().isBlank()) {
            throw new ServiceException("400-4", "비밀번호 재설정 이메일을 통해 비밀번호를 먼저 등록해주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new ServiceException("401-5", "현재 비밀번호가 올바르지 않습니다.");
        }

        validateNewPasswordForMember(member, newPassword, newPasswordConfirm);
        member.changeEncodedPassword(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(member.getId());
        passwordAuditService.passwordChanged(member.getId());
    }

    private void validateNewPasswordForMember(Member member, String newPassword, String newPasswordConfirm) {
        validateNewPassword(newPassword, newPasswordConfirm);
        if (member.getPassword() != null && passwordEncoder.matches(newPassword, member.getPassword())) {
            throw new ServiceException("409-2", "기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }
    }

    private Member activeMember(long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("400-3", "유효하지 않거나 만료된 비밀번호 재설정 링크입니다."));
        if (!member.isActive()) {
            throw new ServiceException("400-3", "유효하지 않거나 만료된 비밀번호 재설정 링크입니다.");
        }
        return member;
    }

    public void validateNewPassword(String newPassword, String newPasswordConfirm) {
        if (!passwordPolicy.isValid(newPassword) || !newPassword.equals(newPasswordConfirm)) {
            throw new ServiceException("400-1", "새 비밀번호 입력값이 올바르지 않습니다.");
        }
    }
}
